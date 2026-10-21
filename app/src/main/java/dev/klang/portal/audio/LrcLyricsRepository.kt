package dev.klang.portal.audio

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Optional online helper; it is not called during startup or playback. */
class LrcLyricsRepository(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
) {
    fun findSyncedLyrics(title: String, artist: String, callback: (String?) -> Unit) {
        executor.execute {
            val result = runCatching {
                val query = "track_name=" + encode(title) + "&artist_name=" + encode(artist)
                val connection = URL("https://lrclib.net/api/get?" + query).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "Klang-Android/0.1")
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(body).optString("syncedLyrics").takeIf { it.isNotBlank() }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
            callback(result)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
