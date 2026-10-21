package dev.klang.player.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.klang.player.data.Track
import java.util.Locale
import kotlin.math.roundToInt

private enum class Page(val label: String) {
    Songs("Songs"), Albums("Albums"), Artists("Artists"), Playlists("Playlists"),
    Favorites("Liked"), Settings("Settings")
}

private object AlbumArtCache {
    val bitmaps = LruCache<Long, Bitmap>(24)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlangApp(
    tracks: List<Track>, currentTrack: Track?, isPlaying: Boolean, isLoading: Boolean, message: String?,
    favoriteIds: Set<Long>, playlists: List<String>, positionMs: Long, durationMs: Long,
    volume: Int, maxVolume: Int, volumeOverlayVisible: Boolean,
    themeMode: String, accentColor: String, compactRows: Boolean,
    onPlayTrack: (Track) -> Unit, onTogglePlayback: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onToggleFavorite: (Track) -> Unit, onScan: () -> Unit,
    onEditMetadata: (Track, String, String, String) -> Unit, onCreatePlaylist: (String) -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit, onThemeModeChange: (String) -> Unit,
    onAccentColorChange: (String) -> Unit, onCompactRowsChange: (Boolean) -> Unit
) {
    var page by remember { mutableStateOf(Page.Songs) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var fullPlayer by remember { mutableStateOf(false) }
    var createPlaylist by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("Title") }

    fun matches(track: Track, text: String): Boolean =
        track.title.contains(text, true) || track.artist.contains(text, true) ||
            track.album.contains(text, true) || track.folder.contains(text, true)

    val needle = query.trim()
    val visibleTracks = remember(tracks, needle, page, favoriteIds, sortMode) {
        val base = if (page == Page.Favorites) tracks.filter { favoriteIds.contains(it.id) } else tracks
        val searched = if (needle.isBlank()) base else base.filter { matches(it, needle) }
        when (sortMode) {
            "Artist" -> searched.sortedWith(compareBy<Track> { it.artist.lowercase(Locale.ROOT) }.thenBy { it.title.lowercase(Locale.ROOT) })
            "Longest" -> searched.sortedByDescending { it.durationMs }
            else -> searched.sortedBy { it.title.lowercase(Locale.ROOT) }
        }
    }
    val albums = remember(tracks, needle) {
        tracks.groupBy { it.album.ifBlank { "Unknown album" } }.toSortedMap(java.lang.String.CASE_INSENSITIVE_ORDER)
            .filter { (name, songs) -> needle.isBlank() || name.contains(needle, true) || songs.any { matches(it, needle) } }
    }
    val artists = remember(tracks, needle) {
        tracks.groupBy { it.artist.ifBlank { "Unknown artist" } }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .filter { (name, songs) -> needle.isBlank() || name.contains(needle, true) || songs.any { matches(it, needle) } }
    }
    val visiblePlaylists = remember(playlists, needle) {
        if (needle.isBlank()) playlists else playlists.filter { it.contains(needle, true) }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (searchOpen) {
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search songs, albums, artists") },
                                singleLine = true,
                                trailingIcon = {
                                    if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Clear") }
                                }
                            )
                        } else Column {
                            Text("Klang", fontWeight = FontWeight.Bold)
                            Text(page.label, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (searchOpen) query = ""
                            searchOpen = !searchOpen
                        }) {
                            Icon(if (searchOpen) Icons.Default.ArrowBack else Icons.Default.Search, contentDescription = "Search")
                        }
                        if (page == Page.Songs && !searchOpen) {
                            TextButton(onClick = {
                                sortMode = when (sortMode) {
                                    "Title" -> "Artist"
                                    "Artist" -> "Longest"
                                    else -> "Title"
                                }
                            }) { Text(sortMode) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                Column {
                    if (currentTrack != null) {
                        MiniPlayer(
                            currentTrack, isPlaying, positionMs, durationMs,
                            { fullPlayer = true }, onTogglePlayback, onNext
                        )
                    }
                    NavigationBar {
                        listOf(Page.Songs, Page.Albums, Page.Artists, Page.Playlists, Page.Favorites, Page.Settings).forEach { item ->
                            NavigationBarItem(
                                selected = page == item, onClick = { page = item },
                                icon = {
                                    Text(when (item) {
                                        Page.Songs -> "♫"; Page.Albums -> "▤"; Page.Artists -> "♬"
                                        Page.Playlists -> "☷"; Page.Favorites -> "♥"; Page.Settings -> "⚙"
                                    })
                                },
                                label = { Text(item.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "page", modifier = Modifier.fillMaxSize().padding(padding)
            ) { selectedPage ->
                when (selectedPage) {
                    Page.Songs, Page.Favorites -> SongList(
                        tracks = visibleTracks, libraryCount = if (page == Page.Favorites) favoriteIds.size else tracks.size,
                        loading = isLoading, message = message, searchQuery = needle, current = currentTrack,
                        favorites = favoriteIds, compactRows = compactRows, onScan = onScan,
                        onPlay = onPlayTrack, onMenu = { menuTrack = it }, onFavorite = onToggleFavorite
                    )
                    Page.Albums -> GroupList(albums, "Albums", needle.isNotBlank()) { it.firstOrNull()?.let(onPlayTrack) }
                    Page.Artists -> GroupList(artists, "Artists", needle.isNotBlank()) { it.firstOrNull()?.let(onPlayTrack) }
                    Page.Playlists -> PlaylistScreen(visiblePlaylists, { createPlaylist = true }, tracks.size, needle)
                    Page.Settings -> SettingsScreen(
                        tracks.size, onScan, themeMode, onThemeModeChange, accentColor, onAccentColorChange,
                        compactRows, onCompactRowsChange, volume, maxVolume, onVolumeChange,
                        onAbout = { showAbout = true }
                    )
                }
            }
        }

        if (volumeOverlayVisible) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
                    .fillMaxWidth(0.90f).widthIn(max = 360.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Volume", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(if (maxVolume > 0) "${(volume * 100 / maxVolume)}%" else "0%",
                            style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = volume.toFloat().coerceIn(0f, maxVolume.coerceAtLeast(1).toFloat()),
                        onValueChange = { onVolumeChange(it.roundToInt()) },
                        valueRange = 0f..maxVolume.coerceAtLeast(1).toFloat()
                    )
                }
            }
        }
    }

    menuTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { menuTrack = null },
            title = { Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text(track.artist + "
" + track.album + "
" + track.durationLabel + "
" + track.folder) },
            confirmButton = {
                TextButton(onClick = { editingTrack = track; menuTrack = null }) { Text("Edit details") }
            },
            dismissButton = {
                TextButton(onClick = { onToggleFavorite(track); menuTrack = null }) {
                    Text(if (favoriteIds.contains(track.id)) "Unlike" else "Like")
                }
            }
        )
    }
    editingTrack?.let { track ->
        MetadataDialog(track, { editingTrack = null }) { title, artist, album ->
            onEditMetadata(track, title, artist, album)
            editingTrack = null
        }
    }
    if (createPlaylist) PlaylistDialog({ createPlaylist = false }) { name ->
        onCreatePlaylist(name)
        createPlaylist = false
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About Klang") },
            text = { Text("Klang is an offline-first music player. Your library, favorites, and appearance preferences stay on this device. Android's media notification and hardware audio controls are used for background playback.") },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Done") } }
        )
    }
    if (fullPlayer && currentTrack != null) FullPlayer(
        track = currentTrack, playing = isPlaying, positionMs = positionMs,
        durationMs = durationMs, volume = volume, maxVolume = maxVolume,
        onVolumeChange = onVolumeChange, onClose = { fullPlayer = false },
        onToggle = onTogglePlayback, onNext = onNext, onPrevious = onPrevious,
        onShuffle = onShuffle, onRepeat = onRepeat, isFavorite = favoriteIds.contains(currentTrack.id),
        onFavorite = { onToggleFavorite(currentTrack) }, onSeek = onSeek
    )
}

@Composable
private fun SongList(
    tracks: List<Track>, libraryCount: Int, loading: Boolean, message: String?, searchQuery: String,
    current: Track?, favorites: Set<Long>, compactRows: Boolean, onScan: () -> Unit,
    onPlay: (Track) -> Unit, onMenu: (Track) -> Unit, onFavorite: (Track) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (searchQuery.isBlank()) "$libraryCount tracks" else "${tracks.size} matches",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                )
                Text("On-device music library", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onScan) { Text("Rescan") }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            tracks.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("♫", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isNotBlank()) "No matches for “$searchQuery”"
                        else message ?: "No music found",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (searchQuery.isBlank()) {
                        Button(onClick = onScan) { Text("Scan music") }
                    } else {
                        Text("Try another search", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(tracks, key = { it.id }) { track ->
                    SongRow(
                        track, current?.id == track.id, favorites.contains(track.id), compactRows,
                        { onPlay(track) }, { onMenu(track) }, { onFavorite(track) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(
    track: Track, selected: Boolean, favorite: Boolean, compact: Boolean,
    onClick: () -> Unit, onMenu: () -> Unit, onFavorite: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onMenu)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = if (compact) 5.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumTile(track, Modifier.size(if (compact) 42.dp else 50.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(
                track.artist + " · " + track.album, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(track.durationLabel, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onFavorite) {
            Icon(
                if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Toggle favorite",
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumTile(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val albumId = track.albumId
    val bitmap by produceState<Bitmap?>(
        initialValue = if (albumId > 0L) AlbumArtCache.bitmaps.get(albumId) else null,
        key1 = albumId
    ) {
        if (value == null && albumId > 0L) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            value?.let { AlbumArtCache.bitmaps.put(albumId, it) }
        }
    }
    val swatches = listOf(Color(0xFF6E5AC8), Color(0xFF327C78), Color(0xFFB66A46), Color(0xFF4D668E))
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(swatches[Math.floorMod(albumId, swatches.size.toLong()).toInt()]),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(), contentDescription = "Album artwork for ${track.album}",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
            )
        } else Text("♫", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun MiniPlayer(
    track: Track, playing: Boolean, positionMs: Long, durationMs: Long,
    onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit
) {
    val duration = durationMs.takeIf { it > 0L } ?: track.durationMs
    val progress = if (duration > 0L) (positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp
    ) {
        Column {
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumTile(track, Modifier.size(42.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onToggle) { Text(if (playing) "Ⅱ" else "▶", style = MaterialTheme.typography.titleLarge) }
                TextButton(onClick = onNext) { Text("⏭", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
private fun GroupList(groups: Map<String, List<Track>>, label: String, isSearching: Boolean, onSelect: (List<Track>) -> Unit) {
    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(if (isSearching) "No matching $label" else "Your $label will appear here")
        }
    } else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(groups.entries.toList(), key = { it.key }) { (name, songs) ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(songs) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumTile(songs.first(), Modifier.size(54.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name.ifBlank { "Unknown" }, fontWeight = FontWeight.Medium)
                    Text("${songs.size} songs", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlaylistScreen(playlists: List<String>, onCreate: () -> Unit, count: Int, query: String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Create listening collections", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onCreate) { Text("New") }
        }
        Spacer(Modifier.height(12.dp))
        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (query.isNotBlank()) "No matching playlists" else "No playlists yet. Create your first playlist.")
            }
        } else LazyColumn {
            items(playlists, key = { it }) { name ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text("Local playlist · $count library tracks available") },
                    leadingContent = { Text("♫", color = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    count: Int, onScan: () -> Unit, themeMode: String, onThemeModeChange: (String) -> Unit,
    accent: String, onAccentChange: (String) -> Unit, compactRows: Boolean,
    onCompactRowsChange: (Boolean) -> Unit, volume: Int, maxVolume: Int,
    onVolumeChange: (Int) -> Unit, onAbout: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item { SettingRow("Scan music", "Re-index local audio files", onScan) }
        item {
            ListItem(
                headlineContent = { Text("Music library") },
                supportingContent = { Text("$count indexed tracks · stored locally") },
                leadingContent = { Text("♫", color = MaterialTheme.colorScheme.primary) }
            )
        }
        item { HorizontalDividerIfAvailable() }
        item { Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Text("Choose the app's light and dark appearance", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark", "amoled" to "AMOLED").forEach { (value, label) ->
                        FilterChip(selected = themeMode == value, onClick = { onThemeModeChange(value) }, label = { Text(label) })
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Accent color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("purple" to "Purple", "blue" to "Blue", "green" to "Green", "orange" to "Orange").forEach { (value, label) ->
                        FilterChip(selected = accent == value, onClick = { onAccentChange(value) }, label = { Text(label) })
                    }
                }
            }
        }
        item {
            ListItem(
                headlineContent = { Text("Compact song list") },
                supportingContent = { Text("Fit more songs on screen") },
                trailingContent = { Switch(checked = compactRows, onCheckedChange = onCompactRowsChange) }
            )
        }
        item { HorizontalDividerIfAvailable() }
        item { Text("Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Media volume", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(if (maxVolume > 0) "${(volume * 100 / maxVolume)}%" else "0%",
                        style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = volume.toFloat().coerceIn(0f, maxVolume.coerceAtLeast(1).toFloat()),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..maxVolume.coerceAtLeast(1).toFloat()
                )
                Text("Hardware volume buttons use this in-app control while Klang is open.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ListItem(
                headlineContent = { Text("Audio effects") },
                supportingContent = { Text("No equalizer is bundled in this lightweight build. Playback uses Android's normal audio path.") },
                leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
        item { HorizontalDividerIfAvailable() }
        item { SettingRow("About Klang", "Offline-first · no account or analytics SDK", onAbout) }
    }
}

@Composable
private fun HorizontalDividerIfAvailable() {
    Divider(Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SettingRow(title: String, detail: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        headlineContent = { Text(title) }, supportingContent = { Text(detail) },
        leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }
    )
}

@Composable
private fun MetadataDialog(track: Track, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Edit song details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(artist, { artist = it }, label = { Text("Artist") }, singleLine = true)
                OutlinedTextField(album, { album = it }, label = { Text("Album") }, singleLine = true)
                Text("Android may ask for file-write approval before saving these library details.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, artist, album) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PlaylistDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("New playlist") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Playlist name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FullPlayer(
    track: Track, playing: Boolean, positionMs: Long, durationMs: Long, volume: Int, maxVolume: Int,
    onVolumeChange: (Int) -> Unit, onClose: () -> Unit, onToggle: () -> Unit,
    onNext: () -> Unit, onPrevious: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit,
    isFavorite: Boolean, onFavorite: () -> Unit, onSeek: (Long) -> Unit
) {
    val duration = durationMs.takeIf { it > 0L } ?: track.durationMs
    var progress by remember(track.id) { mutableFloatStateOf(0f) }
    var isSeeking by remember(track.id) { mutableStateOf(false) }
    LaunchedEffect(track.id, positionMs, duration) {
        if (!isSeeking && duration > 0L) progress = (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    }
    AlertDialog(
        onDismissRequest = onClose, confirmButton = {},
        title = { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Close player") }
            Text("Now playing")
        } },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AlbumTile(track, Modifier.fillMaxWidth().height(210.dp))
                Spacer(Modifier.height(18.dp))
                Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(track.album, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { progress = it; isSeeking = true },
                    onValueChangeFinished = {
                        onSeek((duration * progress).toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..1f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(positionMs), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onShuffle) { Text("⇄") }
                    TextButton(onClick = onPrevious) { Text("⏮", style = MaterialTheme.typography.titleLarge) }
                    FilledIconButton(onClick = onToggle, modifier = Modifier.size(60.dp)) {
                        Text(if (playing) "Ⅱ" else "▶", style = MaterialTheme.typography.headlineMedium)
                    }
                    TextButton(onClick = onNext) { Text("⏭", style = MaterialTheme.typography.titleLarge) }
                    TextButton(onClick = onRepeat) { Text("↻") }
                }
                TextButton(onClick = onFavorite) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.width(6.dp)); Text(if (isFavorite) "Liked" else "Add to liked songs")
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Volume", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = volume.toFloat().coerceIn(0f, maxVolume.coerceAtLeast(1).toFloat()),
                        onValueChange = { onVolumeChange(it.roundToInt()) },
                        valueRange = 0f..maxVolume.coerceAtLeast(1).toFloat()
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L)
}
