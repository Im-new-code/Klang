package dev.klang.player

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val tracks = androidx.compose.runtime.mutableStateListOf<Track>()
    private val currentTrack = androidx.compose.runtime.mutableStateOf<Track?>(null)
    private val playing = androidx.compose.runtime.mutableStateOf(false)
    private val loading = androidx.compose.runtime.mutableStateOf(false)
    private val message = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val favorites = androidx.compose.runtime.mutableStateListOf<Long>()
    private val playlists = androidx.compose.runtime.mutableStateListOf<String>()
    private val positionMs = androidx.compose.runtime.mutableLongStateOf(0L)
    private val durationMs = androidx.compose.runtime.mutableLongStateOf(0L)
    private val volume = androidx.compose.runtime.mutableIntStateOf(0)
    private val maxVolume = androidx.compose.runtime.mutableIntStateOf(1)
    private val volumeOverlayVisible = androidx.compose.runtime.mutableStateOf(false)
    private val themeMode = androidx.compose.runtime.mutableStateOf("system")
    private val accentColor = androidx.compose.runtime.mutableStateOf("purple")
    private val compactRows = androidx.compose.runtime.mutableStateOf(false)
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var repository: MusicRepository
    private lateinit var audioManager: AudioManager
    private var pendingEdit: Triple<Track, String, String>? = null
    private var pendingAlbum = ""
    private var scanRunning = false
    private var lastScanAttemptAt = 0L
    private var lastScanCompletedAt = 0L
    private var volumeHideJob: Job? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanLibrary() else message.value = "Music access is off. Allow audio access in Android app settings, then tap Scan music."
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
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = syncPlayerState()
        override fun onEvents(player: Player, events: Player.Events) = syncPlayerState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MusicRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val prefs = getSharedPreferences("klang", MODE_PRIVATE)
        favorites.addAll(prefs.getStringSet("favorites", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() })
        playlists.addAll(prefs.getStringSet("playlists", emptySet()).orEmpty().sorted())
        themeMode.value = prefs.getString("theme_mode", "system") ?: "system"
        accentColor.value = prefs.getString("accent_color", "purple") ?: "purple"
        compactRows.value = prefs.getBoolean("compact_rows", false)
        refreshVolumeState()
        connectPlayer()
        setContent {
            KlangTheme(mode = themeMode.value, accent = accentColor.value) {
                KlangApp(
                    tracks = tracks, currentTrack = currentTrack.value, isPlaying = playing.value,
                    isLoading = loading.value, message = message.value, favoriteIds = favorites.toSet(),
                    playlists = playlists.toList(), positionMs = positionMs.longValue,
                    durationMs = durationMs.longValue, volume = volume.intValue,
                    maxVolume = maxVolume.intValue, volumeOverlayVisible = volumeOverlayVisible.value,
                    themeMode = themeMode.value, accentColor = accentColor.value, compactRows = compactRows.value,
                    onPlayTrack = ::playTrack, onTogglePlayback = ::togglePlayback,
                    onNext = { controller?.seekToNextMediaItem() },
                    onPrevious = { controller?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } },
                    onToggleFavorite = ::toggleFavorite, onScan = { scanLibrary(force = true) },
                    onEditMetadata = ::editMetadata, onCreatePlaylist = ::createPlaylist,
                    onShuffle = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
                    onRepeat = { controller?.let { it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF } },
                    onSeek = { ms -> controller?.seekTo(ms); positionMs.longValue = ms },
                    onVolumeChange = ::setVolume, onThemeModeChange = ::setThemeMode,
                    onAccentColorChange = ::setAccentColor, onCompactRowsChange = ::setCompactRows
                )
            }
        }
        if (hasAudioPermission()) scanLibrary() else requestMusicPermission()
        lifecycleScope.launch {
            while (isActive) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    val player = controller
                    if (player != null) {
                        positionMs.longValue = player.currentPosition.coerceAtLeast(0L)
                        durationMs.longValue = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                            ?: currentTrack.value?.durationMs ?: 0L
                        playing.value = player.isPlaying
                    }
                }
                delay(400L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized && hasAudioPermission()) {
            val stale = lastScanCompletedAt == 0L || System.currentTimeMillis() - lastScanCompletedAt > 30_000L
            if (stale) scanLibrary(force = false)
        } else if (::repository.isInitialized && tracks.isEmpty()) {
            message.value = "Music permission is required to scan audio on this device."
        }
        if (::audioManager.isInitialized) refreshVolumeState()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::audioManager.isInitialized &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val direction = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
                refreshVolumeState()
                showVolumeOverlay()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showVolumeOverlay() {
        volumeOverlayVisible.value = true
        volumeHideJob?.cancel()
        volumeHideJob = lifecycleScope.launch {
            delay(2200L)
            volumeOverlayVisible.value = false
        }
    }

    private fun refreshVolumeState() {
        volume.intValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        maxVolume.intValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    private fun setVolume(value: Int) {
        val safe = value.coerceIn(0, maxVolume.intValue.coerceAtLeast(1))
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safe, 0)
        refreshVolumeState()
        showVolumeOverlay()
    }

    private fun setThemeMode(mode: String) {
        themeMode.value = mode
        getSharedPreferences("klang", MODE_PRIVATE).edit().putString("theme_mode", mode).apply()
    }

    private fun setAccentColor(accent: String) {
        accentColor.value = accent
        getSharedPreferences("klang", MODE_PRIVATE).edit().putString("accent_color", accent).apply()
    }

    private fun setCompactRows(compact: Boolean) {
        compactRows.value = compact
        getSharedPreferences("klang", MODE_PRIVATE).edit().putBoolean("compact_rows", compact).apply()
    }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            val connected = runCatching { controllerFuture?.get() }.getOrNull() ?: return@addListener
            if (isFinishing || isDestroyed) { connected.release(); return@addListener }
            controller = connected
            connected.addListener(playerListener)
            updateQueue(tracks.toList())
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

    private fun scanLibrary(force: Boolean = true) {
        if (!hasAudioPermission()) {
            loading.value = false
            message.value = "Music access is not granted. Allow audio access, then scan again."
            return
        }
        if (scanRunning) return
        val now = System.currentTimeMillis()
        if (!force && now - lastScanAttemptAt < 30_000L) return
        scanRunning = true
        lastScanAttemptAt = now
        loading.value = true
        message.value = null
        lifecycleScope.launch {
            val result = runCatching { repository.scan() }
            scanRunning = false
            loading.value = false
            lastScanCompletedAt = System.currentTimeMillis()
            result.onSuccess { found ->
                tracks.clear()
                tracks.addAll(found)
                message.value = if (found.isEmpty()) {
                    "No audio files were indexed. Check that the files are stored locally and are audio files, then scan again."
                } else null
                updateQueue(found)
                syncPlayerState()
            }.onFailure { error ->
                message.value = when (error) {
                    is SecurityException -> "Android blocked music access. Recheck the audio permission in app settings."
                    else -> "Couldn't scan the music library (" + error.javaClass.simpleName + "). Check permission and try again."
                }
            }
        }
    }

    private fun itemsFor(source: List<Track>) = source.map { track ->
        MediaItem.Builder().setMediaId(track.id.toString()).setUri(track.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setDisplayTitle(track.title)
                .setArtist(track.artist).setAlbumTitle(track.album).setArtworkUri(track.artworkUri).build()).build()
    }

    private fun updateQueue(source: List<Track>) {
        val player = controller ?: return
        if (source.isEmpty()) return
        val sourceIds = source.map { it.id }
        val queueIds = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        if (sourceIds == queueIds) return
        val wasPlaying = player.isPlaying
        val activeId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val oldPosition = player.currentPosition.coerceAtLeast(0L)
        val startIndex = source.indexOfFirst { it.id == activeId }.let { if (it >= 0) it else 0 }
        val startPosition = if (source.any { it.id == activeId }) oldPosition else 0L
        player.setMediaItems(itemsFor(source), startIndex, startPosition)
        player.prepare()
        if (wasPlaying) player.play()
    }

    private fun playTrack(track: Track) {
        val player = controller ?: return
        val source = tracks.toList()
        val index = source.indexOfFirst { it.id == track.id }
        if (index < 0) return
        val queueIds = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        if (queueIds != source.map { it.id }) {
            player.setMediaItems(itemsFor(source), index, 0L)
            player.prepare()
        } else player.seekTo(index, 0L)
        player.play()
        currentTrack.value = track
        positionMs.longValue = 0L
        durationMs.longValue = track.durationMs
    }

    private fun togglePlayback() {
        val player = controller ?: return
        if (player.isPlaying) player.pause()
        else {
            if (player.mediaItemCount == 0) updateQueue(tracks.toList())
            if (player.mediaItemCount > 0) player.play()
        }
    }

    private fun syncPlayerState() {
        val player = controller ?: return
        playing.value = player.isPlaying
        val id = player.currentMediaItem?.mediaId?.toLongOrNull()
        currentTrack.value = tracks.firstOrNull { it.id == id }
        positionMs.longValue = player.currentPosition.coerceAtLeast(0L)
        durationMs.longValue = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
            ?: currentTrack.value?.durationMs ?: 0L
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
        volumeHideJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(false)
        controllerFuture = null
        super.onDestroy()
    }
}
