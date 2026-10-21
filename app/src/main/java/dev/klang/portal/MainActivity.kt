package dev.klang.portal

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dev.klang.portal.audio.LocalAudioRepository
import dev.klang.portal.audio.PortalPlaybackService
import dev.klang.portal.model.Track
import dev.klang.portal.world.FantasyWorldView
import dev.klang.portal.world.WorldHudView
import java.util.concurrent.Executors

class MainActivity : Activity(), SensorEventListener {
    private lateinit var worldView: FantasyWorldView
    private lateinit var hudView: WorldHudView
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: LocalAudioRepository
    private var rotationSensor: Sensor? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var tracks: List<Track> = emptyList()
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private val clockTick = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            worldView.refreshWorldClock()
            handler.postDelayed(this, 60_000L)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateCurrentTrack() }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            hudView.setPlaying(isPlaying)
            if (isPlaying) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) hudView.setPlaying(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(23, 37, 31)
        window.navigationBarColor = android.graphics.Color.rgb(16, 26, 22)
        worldView = FantasyWorldView(this)
        hudView = WorldHudView(this)
        val root = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(23, 37, 31))
            addView(worldView, android.widget.FrameLayout.LayoutParams(-1, -1))
            addView(hudView, android.widget.FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        repository = LocalAudioRepository(this)
        hudView.onPlayPause = { togglePlayback() }
        hudView.onNext = { controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
        hudView.onPrevious = {
            controller?.let {
                if (it.currentPosition > 3_000L) it.seekTo(0L)
                else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem()
            }
        }
        hudView.onVolumeChanged = { volume -> controller?.setVolume(volume) }
        hudView.setStatus(if (rotationSensor == null) "Portal ready · motion sensor unavailable"
            else "Portal ready · tilt to look around")
        connectPlaybackService()
        if (hasAudioPermission()) scanLibrary() else requestAudioPermission()
    }

    private fun connectPlaybackService() {
        val token = SessionToken(this, ComponentName(this, PortalPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            val connected = runCatching { controllerFuture?.get() }.getOrNull() ?: return@addListener
            if (isFinishing || isDestroyed) { connected.release(); return@addListener }
            controller = connected
            connected.addListener(playerListener)
            installPlaylistIfReady()
            hudView.setPlaying(connected.isPlaying)
            updateCurrentTrack()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        requestPermissions(arrayOf(permission), REQUEST_MEDIA_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MEDIA_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) scanLibrary()
        else if (requestCode == REQUEST_MEDIA_PERMISSION) hudView.setStatus("Music access was declined. Allow media access to scan local songs.")
    }

    private fun scanLibrary() {
        hudView.setStatus("Listening for music in your local library…")
        repository.scan { result ->
            result.onSuccess { found ->
                tracks = found
                hudView.setLibrarySummary(found.size)
                hudView.setStatus(if (found.isEmpty()) "No indexed music found. Add audio and scan again."
                    else "Your music stays on this device")
                installPlaylistIfReady()
            }.onFailure {
                hudView.setStatus("Could not scan music. Check permission and try again.")
                android.util.Log.e(TAG, "MediaStore scan failed", it)
            }
        }
    }

    private fun installPlaylistIfReady() {
        val player = controller ?: return
        if (tracks.isEmpty() || player.mediaItemCount > 0) return
        val items = tracks.map { track ->
            MediaItem.Builder().setMediaId(track.id.toString()).setUri(track.uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist)
                    .setAlbumTitle(track.album).build()).build()
        }
        player.setMediaItems(items)
        player.prepare()
        updateCurrentTrack()
    }

    private fun togglePlayback() {
        val player = controller
        if (player == null || player.mediaItemCount == 0) {
            hudView.setStatus("No tracks are ready yet. Add music and scan again.")
            return
        }
        if (player.isPlaying) player.pause()
        else {
            if (player.currentMediaItem == null) player.seekTo(0, 0L)
            player.play()
        }
    }

    private fun updateCurrentTrack() {
        val player = controller ?: return
        val track = tracks.getOrNull(player.currentMediaItemIndex)
        if (track == null) {
            if (tracks.isNotEmpty()) hudView.setTrack("Choose a song", "Your library is ready")
            return
        }
        hudView.setTrack(track.title, track.artist)
        val artUri = track.albumArtUri
        if (artUri == null) { hudView.setAlbumArt(null); return }
        artworkExecutor.execute {
            val bitmap = runCatching {
                contentResolver.openInputStream(artUri)?.use { input ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(artUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    val sample = if (bounds.outWidth > 512 || bounds.outHeight > 512) 4 else 1
                    BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    })
                }
            }.getOrNull()
            handler.post { if (!isFinishing && !isDestroyed) hudView.setAlbumArt(bitmap) else bitmap?.recycle() }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val targetPitch = orientation[1].coerceIn(-0.45f, 0.45f)
        val targetRoll = orientation[2].coerceIn(-0.45f, 0.45f)
        smoothedPitch = smoothedPitch * 0.84f + targetPitch * 0.16f
        smoothedRoll = smoothedRoll * 0.84f + targetRoll * 0.16f
        worldView.setParallax(smoothedRoll / 0.45f, smoothedPitch / 0.45f)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onResume() {
        super.onResume()
        worldView.onResume()
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        handler.removeCallbacks(clockTick)
        handler.postDelayed(clockTick, 60_000L)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        sensorManager.unregisterListener(this)
        worldView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(false)
        controllerFuture = null
        artworkExecutor.shutdown()
        repository.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_MEDIA_PERMISSION = 7301
        private const val TAG = "KlangPortal"
    }
}
