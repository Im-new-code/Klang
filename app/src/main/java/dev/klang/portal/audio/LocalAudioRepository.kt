package dev.klang.portal.audio

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dev.klang.portal.model.Track
import java.util.concurrent.Executors

/** Queries Android's indexed media collection off the UI thread; avoids broad all-files access. */
class LocalAudioRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scanner = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Klang-Media-Scanner").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    fun scan(onComplete: (Result<List<Track>>) -> Unit) {
        scanner.execute {
            val result = runCatching { queryTracks() }
            mainHandler.post { onComplete(result) }
        }
    }

    private fun queryTracks(): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        val tracks = mutableListOf<Track>()

        appContext.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                tracks += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = cursor.getStringOrFallback(titleColumn, "Unknown track"),
                    artist = cursor.getStringOrFallback(artistColumn, "Unknown artist"),
                    album = cursor.getStringOrFallback(albumColumn, "Unknown album"),
                    durationMs = cursor.getLongOrFallback(durationColumn, 0L),
                    albumArtUri = if (albumId > 0L) Uri.parse("content://media/external/audio/albumart/" + albumId) else null
                )
            }
        }
        return tracks
    }

    private fun Cursor.getStringOrFallback(index: Int, fallback: String): String {
        val value = getString(index)?.trim()
        return value?.takeIf { it.isNotEmpty() && it != "<unknown>" } ?: fallback
    }

    private fun Cursor.getLongOrFallback(index: Int, fallback: Long): Long =
        if (isNull(index)) fallback else getLong(index)

    fun shutdown() { scanner.shutdown() }
}
