package dev.klang.player

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dev.klang.player.data.MusicRepository
import dev.klang.player.data.Track
import dev.klang.player.playback.MusicService
import dev.klang.player.ui.KlangApp
import dev.klang.player.ui.KlangTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val tracks = androidx.compose.runtime.mutableStateListOf<Track>()
    private val currentTrack = androidx.compose.runtime.mutableStateOf<Track?>(null)
    private val playing = androidx.compose.runtime.mutableStateOf(false)
    private val loading = androidx.compose.runtime.mutableStateOf(false)
    private val message = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val favorites = androidx.compose.runtime.mutableStateListOf<Long>()
    private val playlists = androidx.compose.runtime.mutableStateListOf<String>()
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var repository: MusicRepository
    private var pendingEdit: Triple<Track, String, String>? = null
    private var pendingAlbum = ""

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanLibrary() else message.value = "Allow music access to show your library."
    }
    private val writeRequestLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val edit = pendingEdit
        if (result.resultCode == RESULT_OK && edit != null) saveMetadataNow(edit.first, edit.second, edit.third, pendingAlbum)
        else message.value = "Write access was not granted."
        pendingEdit = null
    }
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncPlayerState()
        override fun onIsPlayingChanged(isPlaying: Boolean) { playing.value = isPlaying }
        override fun onPlaybackStateChanged(playbackState: Int) = syncPlayerState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MusicRepository(this)
        val prefs = getSharedPreferences("klang", MODE_PRIVATE)
        favorites.addAll(prefs.getStringSet("favorites", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() })
        playlists.addAll(prefs.getStringSet("playlists", emptySet()).orEmpty().sorted())
        connectPlayer()
        setContent {
            KlangTheme {
                KlangApp(
                    tracks = tracks, currentTrack = currentTrack.value, isPlaying = playing.value,
                    isLoading = loading.value, message = message.value, favoriteIds = favorites.toSet(),
                    playlists = playlists.toList(),
                    onPlayTrack = ::playTrack, onTogglePlayback = ::togglePlayback,
                    onNext = { controller?.seekToNextMediaItem() },
                    onPrevious = { controller?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } },
                    onToggleFavorite = ::toggleFavorite, onScan = ::scanLibrary,
                    onEditMetadata = ::editMetadata, onCreatePlaylist = ::createPlaylist,
                    onShuffle = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
                    onRepeat = { controller?.let { it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF } },
                    onSeek = { controller?.seekTo(it) }
                )
            }
        }
        if (hasAudioPermission()) scanLibrary() else requestMusicPermission()
    }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            val connected = runCatching { controllerFuture?.get() }.getOrNull() ?: return@addListener
            if (isFinishing || isDestroyed) { connected.release(); return@addListener }
            controller = connected
            connected.addListener(playerListener)
            installPlaylistIfNeeded()
            syncPlayerState()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestMusicPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        permissionLauncher.launch(permission)
    }

    private fun scanLibrary() {
        loading.value = true
        message.value = null
        lifecycleScope.launch {
            runCatching { repository.scan() }
                .onSuccess { found ->
                    tracks.clear(); tracks.addAll(found); loading.value = false
                    if (found.isEmpty()) message.value = "No music found. Add audio files and scan again."
                    installPlaylistIfNeeded(); syncPlayerState()
                }
                .onFailure { loading.value = false; message.value = "Library scan failed. Check storage permission and try again." }
        }
    }

    private fun itemsFor(source: List<Track>) = source.map { track ->
        MediaItem.Builder().setMediaId(track.id.toString()).setUri(track.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
                .setAlbumTitle(track.album).setArtworkUri(track.artworkUri).build()).build()
    }

    private fun installPlaylistIfNeeded() {
        val player = controller ?: return
        if (tracks.isNotEmpty() && player.mediaItemCount == 0) {
            player.setMediaItems(itemsFor(tracks)); player.prepare()
        }
    }

    private fun playTrack(track: Track) {
        val player = controller ?: return
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        if (player.mediaItemCount != tracks.size) {
            player.setMediaItems(itemsFor(tracks), index, 0L); player.prepare()
        } else player.seekTo(index, 0L)
        player.play()
        currentTrack.value = track
    }

    private fun togglePlayback() {
        val player = controller ?: return
        if (player.isPlaying) player.pause()
        else { installPlaylistIfNeeded(); if (player.mediaItemCount > 0) player.play() }
    }

    private fun syncPlayerState() {
        val player = controller ?: return
        playing.value = player.isPlaying
        val id = player.currentMediaItem?.mediaId?.toLongOrNull()
        currentTrack.value = tracks.firstOrNull { it.id == id }
    }

    private fun toggleFavorite(track: Track) {
        if (favorites.contains(track.id)) favorites.remove(track.id) else favorites.add(track.id)
        getSharedPreferences("klang", MODE_PRIVATE).edit()
            .putStringSet("favorites", favorites.map { it.toString() }.toSet()).apply()
    }

    private fun createPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        if (!playlists.contains(clean)) playlists.add(clean)
        getSharedPreferences("klang", MODE_PRIVATE).edit().putStringSet("playlists", playlists.toSet()).apply()
    }

    private fun editMetadata(track: Track, title: String, artist: String, album: String) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val request = MediaStore.createWriteRequest(contentResolver, listOf(track.uri))
                pendingEdit = Triple(track, title, artist)
                pendingAlbum = album
                writeRequestLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } catch (_: Exception) {
                message.value = "Android could not request write access for this file."
            }
        } else saveMetadataNow(track, title, artist, album)
    }

    private fun saveMetadataNow(track: Track, title: String, artist: String, album: String) {
        lifecycleScope.launch {
            if (repository.updateMetadata(track, title, artist, album)) {
                message.value = "Song details updated."
                scanLibrary()
            } else message.value = "Android denied metadata editing for this file."
        }
    }

    override fun onDestroy() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(false)
        controllerFuture = null
        super.onDestroy()
    }
}
