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
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA)
        val result = ArrayList<Track>(256)
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                result += Track(id, ContentUris.withAppendedId(collection, id),
                    cursor.getString(titleCol).orEmpty().ifBlank { "Unknown title" },
                    cursor.getString(artistCol).orEmpty().ifBlank { "Unknown artist" },
                    cursor.getString(albumCol).orEmpty().ifBlank { "Unknown album" },
                    cursor.getLong(albumIdCol), cursor.getLong(durationCol), path.substringBeforeLast('/', ""))
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
            try { context.contentResolver.update(track.uri, values, null, null) > 0 }
            catch (_: SecurityException) { false } catch (_: IllegalArgumentException) { false }
        }
}
