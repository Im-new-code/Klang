package dev.klang.player.data

import android.net.Uri

data class Track(
    val id: Long, val uri: Uri, val title: String, val artist: String,
    val album: String, val albumId: Long, val durationMs: Long, val folder: String = ""
) {
    val artworkUri: Uri? get() = if (albumId > 0) Uri.parse("content://media/external/audio/albumart/" + albumId) else null
    val durationLabel: String get() {
        val seconds = durationMs / 1000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
