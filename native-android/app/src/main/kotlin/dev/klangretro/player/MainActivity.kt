package dev.klangretro.player

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale

private data class Track(
    val id: Long, val title: String, val artist: String, val album: String,
    val duration: Long, val uri: Uri
)

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("klang_native", Context.MODE_PRIVATE) }
    private val tracks = mutableListOf<Track>()
    private val favourites = mutableSetOf<Long>()
    private var themeMode = "dark"
    private var activeTab = "SONGS"
    private var query = ""
    private var albumFilter: String? = null
    private var artistFilter: String? = null
    private var screen = "library"
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var listBox: LinearLayout? = null
    private var miniTitle: TextView? = null
    private var miniArtist: TextView? = null
    private var miniPlay: TextView? = null
    private var titleNow: TextView? = null
    private var artistNow: TextView? = null
    private var progressNow: SeekBar? = null
    private var elapsedNow: TextView? = null
    private var durationNow: TextView? = null
    private var playNow: TextView? = null

    private fun bg() = when (themeMode) {
        "amoled" -> 0xFF000000.toInt()
        "light" -> 0xFFF3EEDC.toInt()
        else -> 0xFF11141C.toInt()
    }
    private fun card() = when (themeMode) {
        "amoled" -> 0xFF080808.toInt()
        "light" -> 0xFFFFF9E9.toInt()
        else -> 0xFF1C202B.toInt()
    }
    private fun fg() = if (themeMode == "light") 0xFF222018.toInt() else 0xFFFFE7B7.toInt()
    private fun muted() = if (themeMode == "light") 0xFF625B4C.toInt() else 0xFFC1B9A7.toInt()
    private fun cyan() = if (themeMode == "light") 0xFF006F78.toInt() else 0xFF72D3DC.toInt()
    private fun orange() = if (themeMode == "light") 0xFFAD4A11.toInt() else 0xFFFFA45E.toInt()
    private fun dp(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { refreshPlayerUi() }
    }
    private val ticker = object : Runnable {
        override fun run() { refreshPlayerUi(); handler.postDelayed(this, 500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeMode = prefs.getString("theme", "dark") ?: "dark"
        favourites.addAll(prefs.getStringSet("favourites", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() })
        connectPlayer()
        draw()
        checkAudioPermission()
        handler.post(ticker)
    }

    private fun connectPlayer() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(listener)
                refreshPlayerUi()
            } catch (e: Exception) { toast("Playback setup failed") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkAudioPermission() {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED) scan()
        else requestPermissions(arrayOf(p), 721)
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, p, results)
        if (code == 721) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) scan()
            else toast("Allow audio access to scan local music.")
        }
    }

    private fun scan() {
        val result = mutableListOf<Track>()
        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION)
        var cursor: Cursor? = null
        try {
            // Do not rely on IS_MUSIC here: Android 8.1 and some OEM media
            // scanners classify user audio inconsistently. Query the indexed
            // audio collection first, then keep readable audio rows.
            cursor = contentResolver.query(base, cols, null, null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")
            cursor?.use { c ->
                val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val ms = c.getLong(duration).coerceAtLeast(0L)
                    val trackId = c.getLong(id)
                    result.add(Track(trackId,
                        c.getString(title)?.takeIf { it.isNotBlank() } ?: "Unknown title",
                        c.getString(artist)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown artist",
                        c.getString(album)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown album",
                        ms, Uri.withAppendedPath(base, trackId.toString())))
                }
            }
        } catch (e: Exception) {
            toast("Music scan failed: " + (e.message ?: e.javaClass.simpleName))
            android.util.Log.e("KlangRetro", "MediaStore audio scan failed", e)
        }
        tracks.clear()
        tracks.addAll(result)
        android.util.Log.i("KlangRetro", "MediaStore scan completed: ${result.size} audio rows")
        draw()
        if (result.isEmpty()) {
            toast("No indexed audio found. Check Music/Downloads and try Rescan.")
        }
    }

    private fun matching(): List<Track> = tracks.filter {
        (albumFilter == null || it.album == albumFilter) &&
        (artistFilter == null || it.artist == artistFilter) &&
        (activeTab != "FAVOURITES" || favourites.contains(it.id)) &&
        (query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true))
    }

    private fun draw() {
        window.statusBarColor = bg()
        window.navigationBarColor = bg()
        if (Build.VERSION.SDK_INT >= 26) {
            window.decorView.systemUiVisibility = if (themeMode == "light")
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(8), dp(15), dp(9))
            setBackgroundColor(bg())
        }
        setContentView(root)
        when (screen) {
            "player" -> drawPlayer(root)
            "settings" -> drawSettings(root)
            else -> drawLibrary(root)
        }
        refreshPlayerUi()
    }

    private fun drawLibrary(root: LinearLayout) {
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("KLANG\nRETRO PLAYER", 21f, cyan(), true), LinearLayout.LayoutParams(0, dp(58), 1f))
        top.addView(label("SETTINGS", 10f, orange()).apply { setPadding(dp(8), dp(12), dp(3), dp(12)); setOnClickListener { screen = "settings"; draw() } })
        root.addView(top)
        root.addView(label("LOCAL MUSIC  //  OFFLINE FIRST", 10f, orange()).apply { setPadding(0, dp(6), 0, dp(10)) })
        val search = EditText(this).apply {
            hint = "Search songs, artists, albums..."
            setHintTextColor(muted()); setTextColor(fg()); textSize = 13f
            typeface = Typeface.MONOSPACE; setSingleLine(true); imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(dp(12), 0, dp(12), 0); setBackgroundColor(card()); setText(query)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(45)))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s?.toString().orEmpty(); drawRows() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, dp(8)) }
        listOf("SONGS", "ALBUMS", "ARTISTS", "FAVES").forEach { name ->
            val actual = if (name == "FAVES") "FAVOURITES" else name
            tabs.addView(label(name, 9f, if (activeTab == actual) cyan() else muted()).apply {
                gravity = Gravity.CENTER; setPadding(dp(4), dp(11), dp(4), dp(11))
                setBackgroundColor(if (activeTab == actual) card() else bg())
                setOnClickListener { activeTab = actual; albumFilter = null; artistFilter = null; draw() }
            }, LinearLayout.LayoutParams(0, dp(38), 1f))
        }
        root.addView(tabs)
        val scroll = ScrollView(this)
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        drawRows()

        val mini = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(6), dp(7), dp(6)); setBackgroundColor(card()); setOnClickListener { screen = "player"; draw() } }
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        miniTitle = label("Nothing playing", 13f, fg(), true)
        miniArtist = label("Choose a track", 10f, muted())
        names.addView(miniTitle); names.addView(miniArtist)
        mini.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        miniPlay = label("▶", 22f, cyan()).apply { gravity = Gravity.CENTER; setPadding(dp(15), dp(3), dp(12), dp(3)); setOnClickListener { togglePlay() } }
        mini.addView(miniPlay)
        root.addView(mini, LinearLayout.LayoutParams(-1, dp(56)))
        root.addView(label("↻  RESCAN DEVICE MUSIC", 10f, muted()).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(2)); setOnClickListener { scan() }
        })
    }

    private fun drawRows() {
        val box = listBox ?: return
        box.removeAllViews()
        val visible = matching()
        when (activeTab) {
            "ALBUMS" -> visible.groupBy { it.album }.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (album, items) ->
                box.addView(row(album, items.size.toString() + " tracks  ·  " + (items.firstOrNull()?.artist ?: "Unknown artist")) {
                    albumFilter = album; artistFilter = null; activeTab = "SONGS"; draw()
                })
            }
            "ARTISTS" -> visible.groupBy { it.artist }.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (artist, items) ->
                box.addView(row(artist, items.size.toString() + " tracks") {
                    artistFilter = artist; albumFilter = null; activeTab = "SONGS"; draw()
                })
            }
            else -> {
                if (albumFilter != null || artistFilter != null) box.addView(label("←  ALL SONGS", 11f, orange()).apply {
                    setPadding(dp(8), dp(12), 0, dp(12)); setOnClickListener { albumFilter = null; artistFilter = null; draw() }
                })
                if (visible.isEmpty()) box.addView(label(
                    if (tracks.isEmpty()) "NO TRACKS FOUND\n\nAdd music to internal storage or an accessible SD card, then rescan."
                    else "No matches found.", 13f, muted()).apply {
                    gravity = Gravity.CENTER; setPadding(dp(15), dp(35), dp(15), dp(35))
                })
                visible.forEach { track ->
                    box.addView(row(track.title, track.artist + "  ·  " + track.album) { play(track, visible) }.apply {
                        setOnLongClickListener { toggleFavourite(track); true }
                    })
                }
            }
        }
    }

    private fun row(title: String, sub: String, click: () -> Unit): View {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(10)); setBackgroundColor(bg())
        }
        line.addView(label("♪", 20f, orange()).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(34), dp(42)))
        val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        words.addView(label(title, 13f, fg(), true))
        words.addView(label(sub, 10f, muted()))
        line.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(line)
        wrap.addView(View(this).apply { setBackgroundColor(card()) }, LinearLayout.LayoutParams(-1, dp(1)))
        wrap.setOnClickListener { click() }
        return wrap
    }

    private fun drawPlayer(root: LinearLayout) {
        root.addView(label("←  BACK TO LIBRARY", 11f, orange()).apply {
            setPadding(0, dp(12), 0, dp(20)); setOnClickListener { screen = "library"; draw() }
        })
        root.addView(label("NOW PLAYING  //  TAPE DECK", 12f, cyan()).apply { gravity = Gravity.CENTER })
        root.addView(label("┌──────────────────┐\n│                  │\n│   K L A N G      │\n│   ▰ RETRO ▰      │\n│                  │\n└──────────────────┘",
            19f, orange()).apply { gravity = Gravity.CENTER; setBackgroundColor(card()) },
            LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(22); bottomMargin = dp(22) })
        titleNow = label("Nothing playing", 18f, fg(), true).apply { gravity = Gravity.CENTER; maxLines = 2 }
        artistNow = label("", 12f, muted()).apply { gravity = Gravity.CENTER; maxLines = 1 }
        root.addView(titleNow); root.addView(artistNow)
        progressNow = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(s: SeekBar) = Unit
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) = Unit
                override fun onStopTrackingTouch(s: SeekBar) {
                    val duration = controller?.duration ?: 0L
                    if (duration > 0) controller?.seekTo(duration * s.progress / 1000L)
                }
            })
        }
        root.addView(progressNow)
        val times = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        elapsedNow = label("0:00", 10f, muted())
        durationNow = label("0:00", 10f, muted()).apply { gravity = Gravity.RIGHT }
        times.addView(elapsedNow, LinearLayout.LayoutParams(0, dp(22), 1f))
        times.addView(durationNow, LinearLayout.LayoutParams(0, dp(22), 1f))
        root.addView(times)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(28)) }
        controls.addView(label("◀|", 23f, fg()).apply { gravity = Gravity.CENTER; setOnClickListener { controller?.seekToPreviousMediaItem() } }, LinearLayout.LayoutParams(0, dp(60), 1f))
        playNow = label("▶", 30f, cyan()).apply { gravity = Gravity.CENTER; setOnClickListener { togglePlay() } }
        controls.addView(playNow, LinearLayout.LayoutParams(0, dp(60), 1f))
        controls.addView(label("|▶", 23f, fg()).apply { gravity = Gravity.CENTER; setOnClickListener { controller?.seekToNextMediaItem() } }, LinearLayout.LayoutParams(0, dp(60), 1f))
        root.addView(controls)
    }

    private fun drawSettings(root: LinearLayout) {
        root.addView(label("←  BACK", 12f, orange()).apply { setPadding(0, dp(12), 0, dp(22)); setOnClickListener { screen = "library"; draw() } })
        root.addView(label("SETTINGS", 24f, cyan(), true))
        root.addView(label("DISPLAY THEME", 11f, orange()).apply { setPadding(0, dp(22), 0, dp(10)) })
        listOf("dark" to "RETRO DARK", "light" to "LIGHT", "amoled" to "AMOLED BLACK").forEach { (value, text) ->
            root.addView(label((if (themeMode == value) "●  " else "○  ") + text, 13f, if (themeMode == value) cyan() else fg()).apply {
                setPadding(dp(12), dp(15), dp(12), dp(15)); setBackgroundColor(card())
                setOnClickListener { themeMode = value; prefs.edit().putString("theme", value).apply(); draw() }
            }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(7) })
        }
        root.addView(label("RESCAN MUSIC LIBRARY", 12f, fg()).apply {
            setPadding(dp(12), dp(15), dp(12), dp(15)); setBackgroundColor(card())
            setOnClickListener { scan() }
        })
        root.addView(label("Klang Retro Native Preview\nKotlin · native Views · Media3\nMinimum Android 8.1 / API 27\n\nTag editing, metadata-write consent, embedded artwork and full feature parity are still in progress. This preview is not the release build.", 11f, muted()).apply { setPadding(0, dp(24), 0, 0) })
    }

    private fun play(track: Track, queue: List<Track>) {
        val p = controller
        if (p == null) { toast("Playback engine is starting. Tap the track again."); return }
        val media = queue.map {
            MediaItem.Builder().setMediaId(it.id.toString()).setUri(it.uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album).build()).build()
        }
        p.setMediaItems(media, queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0), 0L)
        p.prepare(); p.play()
        screen = "player"; draw()
    }

    private fun togglePlay() {
        val p = controller
        if (p == null) { toast("Playback engine is still starting."); return }
        if (p.isPlaying) p.pause()
        else if (p.mediaItemCount > 0) p.play()
        else matching().firstOrNull()?.let { play(it, matching()) } ?: toast("No track to play.")
        refreshPlayerUi()
    }

    private fun toggleFavourite(track: Track) {
        if (!favourites.add(track.id)) favourites.remove(track.id)
        prefs.edit().putStringSet("favourites", favourites.map { it.toString() }.toSet()).apply()
        toast(if (favourites.contains(track.id)) "Added to favourites" else "Removed from favourites")
        drawRows()
    }

    private fun refreshPlayerUi() {
        val p = controller
        val item = p?.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: "Nothing playing"
        val artist = item?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Choose a track from your library"
        miniTitle?.text = title; miniArtist?.text = artist
        miniPlay?.text = if (p?.isPlaying == true) "Ⅱ" else "▶"
        titleNow?.text = title; artistNow?.text = artist
        playNow?.text = if (p?.isPlaying == true) "Ⅱ" else "▶"
        val duration = p?.duration?.takeIf { it > 0 } ?: 0L
        val position = p?.currentPosition?.coerceAtLeast(0L) ?: 0L
        if (duration > 0 && progressNow?.isPressed != true) progressNow?.progress = (position * 1000L / duration).toInt().coerceIn(0, 1000)
        elapsedNow?.text = time(position); durationNow?.text = time(duration)
    }

    private fun time(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) / 1000).toInt()
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; typeface = Typeface.MONOSPACE
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setTextColor(color); maxLines = 2
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}
