package dev.klang.player.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val result = ArrayList<Track>(256)
        // The Audio collection is already audio-only. Do not rely on the IS_MUSIC
        // flag because correctly indexed files on some devices have it unset.
        val selection = MediaStore.Audio.Media.DURATION + " > 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        val cursor = context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?: error("Android returned no music-library cursor.")
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val path = if (pathCol >= 0) it.getString(pathCol).orEmpty() else ""
                result += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = it.getString(titleCol).orEmpty().ifBlank { "Unknown title" },
                    artist = it.getString(artistCol).orEmpty().ifBlank { "Unknown artist" },
                    album = it.getString(albumCol).orEmpty().ifBlank { "Unknown album" },
                    albumId = it.getLong(albumIdCol),
                    durationMs = it.getLong(durationCol),
                    folder = path.substringBeforeLast('/', "").ifBlank { "Unknown folder" }
                )
            }
        }
        result
    }

    suspend fun updateMetadata(track: Track, title: String, artist: String, album: String): Boolean =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title.trim().ifBlank { track.title })
                put(MediaStore.Audio.Media.ARTIST, artist.trim().ifBlank { track.artist })
                put(MediaStore.Audio.Media.ALBUM, album.trim().ifBlank { track.album })
            }
            try {
                context.contentResolver.update(track.uri, values, null, null) > 0
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            }
        }
}
