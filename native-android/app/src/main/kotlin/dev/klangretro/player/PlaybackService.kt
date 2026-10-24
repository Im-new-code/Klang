package dev.klangretro.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Owns playback outside the Activity and exposes system media controls. */
class PlaybackService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
