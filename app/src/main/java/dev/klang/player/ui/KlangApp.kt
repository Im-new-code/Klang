package dev.klang.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.klang.player.data.Track
import java.util.Locale

private enum class Page(val label: String) { Songs("Songs"), Albums("Albums"), Artists("Artists"), Playlists("Playlists"), Favorites("Liked"), Settings("Settings") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlangApp(
    tracks: List<Track>, currentTrack: Track?, isPlaying: Boolean, isLoading: Boolean, message: String?,
    favoriteIds: Set<Long>, playlists: List<String>,
    onPlayTrack: (Track) -> Unit, onTogglePlayback: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onToggleFavorite: (Track) -> Unit, onScan: () -> Unit,
    onEditMetadata: (Track, String, String, String) -> Unit, onCreatePlaylist: (String) -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, onSeek: (Long) -> Unit
) {
    var page by remember { mutableStateOf(Page.Songs) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var fullPlayer by remember { mutableStateOf(false) }
    var createPlaylist by remember { mutableStateOf(false) }
    var sortByTitle by remember { mutableStateOf(true) }
    val filtered = remember(tracks, query, page, favoriteIds, sortByTitle) {
        val base = if (page == Page.Favorites) tracks.filter { favoriteIds.contains(it.id) } else tracks
        val matching = if (query.isBlank()) base else base.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
        if (sortByTitle) matching.sortedBy { it.title.lowercase(Locale.getDefault()) }
        else matching.sortedByDescending { it.durationMs }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen) OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search songs, albums, artists") }, singleLine = true)
                    else Column {
                        Text("Klang", fontWeight = FontWeight.Bold)
                        Text(page.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(if (searchOpen) Icons.Default.ArrowBack else Icons.Default.Search, contentDescription = "Search")
                    }
                    if (page == Page.Songs) TextButton(onClick = { sortByTitle = !sortByTitle }) { Text("Sort") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Column {
                if (currentTrack != null) MiniPlayer(currentTrack, isPlaying, { fullPlayer = true }, onTogglePlayback, onNext)
                NavigationBar {
                    listOf(Page.Songs, Page.Albums, Page.Artists, Page.Playlists, Page.Favorites, Page.Settings).forEach { item ->
                        NavigationBarItem(
                            selected = page == item, onClick = { page = item },
                            icon = { Text(when (item) {
                                Page.Songs -> "♫"; Page.Albums -> "▤"; Page.Artists -> "♬"
                                Page.Playlists -> "☷"; Page.Favorites -> "♥"; Page.Settings -> "⚙"
                            }) },
                            label = { Text(item.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page",
            modifier = Modifier.fillMaxSize().padding(padding)) { selectedPage ->
            when (selectedPage) {
                Page.Songs, Page.Favorites -> SongList(filtered, tracks.size, isLoading, message, currentTrack,
                    favoriteIds, onScan, onPlayTrack, { menuTrack = it }, onToggleFavorite)
                Page.Albums -> GroupList(tracks.groupBy { it.album }.toSortedMap(), "Albums") { it.firstOrNull()?.let(onPlayTrack) }
                Page.Artists -> GroupList(tracks.groupBy { it.artist }.toSortedMap(), "Artists") { it.firstOrNull()?.let(onPlayTrack) }
                Page.Playlists -> PlaylistScreen(playlists, { createPlaylist = true }, tracks.size)
                Page.Settings -> SettingsScreen(tracks.size, onScan)
            }
        }
    }
    menuTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { menuTrack = null },
            title = { Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text(track.artist + "\n" + track.album + "\n" + track.durationLabel) },
            confirmButton = { TextButton(onClick = { editingTrack = track; menuTrack = null }) { Text("Edit details") } },
            dismissButton = { TextButton(onClick = { onToggleFavorite(track); menuTrack = null }) { Text(if (favoriteIds.contains(track.id)) "Unlike" else "Like") } }
        )
    }
    editingTrack?.let { track ->
        MetadataDialog(track, { editingTrack = null }) { title, artist, album ->
            onEditMetadata(track, title, artist, album); editingTrack = null
        }
    }
    if (createPlaylist) PlaylistDialog({ createPlaylist = false }) { name ->
        onCreatePlaylist(name); createPlaylist = false
    }
    if (fullPlayer && currentTrack != null) FullPlayer(
        track = currentTrack, playing = isPlaying, onClose = { fullPlayer = false },
        onToggle = onTogglePlayback, onNext = onNext, onPrevious = onPrevious,
        onShuffle = onShuffle, onRepeat = onRepeat, isFavorite = favoriteIds.contains(currentTrack.id),
        onFavorite = { onToggleFavorite(currentTrack) }, onSeek = onSeek
    )
}

@Composable
private fun SongList(
    tracks: List<Track>, allCount: Int, loading: Boolean, message: String?, current: Track?,
    favorites: Set<Long>, onScan: () -> Unit, onPlay: (Track) -> Unit,
    onMenu: (Track) -> Unit, onFavorite: (Track) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(allCount.toString() + " tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Offline library", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onScan) { Text("Rescan") }
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (tracks.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("♫", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(message ?: "No music found", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onScan) { Text("Scan music") }
            }
        } else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(tracks, key = { it.id }) { track ->
                SongRow(track, current?.id == track.id, favorites.contains(track.id),
                    { onPlay(track) }, { onMenu(track) }, { onFavorite(track) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRow(track: Track, selected: Boolean, favorite: Boolean, onClick: () -> Unit, onMenu: () -> Unit, onFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onMenu)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumTile(track, Modifier.size(50.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(track.artist + " · " + track.album, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(track.durationLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Toggle favorite",
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlbumTile(track: Track, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFF6E5AC8), Color(0xFF327C78), Color(0xFFB66A46), Color(0xFF4D668E))
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(colors[(track.albumId % colors.size).toInt()]), contentAlignment = Alignment.Center) {
        Text("♫", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun MiniPlayer(track: Track, playing: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onOpen), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumTile(track, Modifier.size(42.dp)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play") }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
        }
    }
}

@Composable
private fun GroupList(groups: Map<String, List<Track>>, label: String, onSelect: (List<Track>) -> Unit) {
    if (groups.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your " + label + " will appear here") }
    else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(groups.entries.toList(), key = { it.key }) { (name, songs) ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(songs) }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumTile(songs.first(), Modifier.size(54.dp)); Spacer(Modifier.width(14.dp))
                Column {
                    Text(name.ifBlank { "Unknown" }, fontWeight = FontWeight.Medium)
                    Text(songs.size.toString() + " songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlaylistScreen(playlists: List<String>, onCreate: () -> Unit, count: Int) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Create listening collections", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onCreate) { Text("New") }
        }
        Spacer(Modifier.height(12.dp))
        if (playlists.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No playlists yet. Create your first playlist.") }
        else LazyColumn { items(playlists, key = { it }) { name ->
            ListItem(headlineContent = { Text(name) }, supportingContent = { Text("Local playlist · " + count + " library tracks available") },
                leadingContent = { Text("♫", color = MaterialTheme.colorScheme.primary) })
        } }
    }
}

@Composable
private fun SettingsScreen(count: Int, onScan: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item { SettingRow("Scan music", "Refresh the device media index", onScan) }
        item { SettingRow("Audio effects", "Standard Android playback; no extra DSP library", {}) }
        item { SettingRow("Storage", count.toString() + " indexed tracks · stored locally", {}) }
        item { Divider(Modifier.padding(vertical = 6.dp)) }
        item { Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item { SettingRow("Material 3", "Follows system light or dark appearance", {}) }
        item { SettingRow("About Klang", "Offline-first · no account or analytics SDK", {}) }
    }
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
                Text("Changes update Android's media library metadata when write access is available.", style = MaterialTheme.typography.bodySmall)
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
    track: Track, playing: Boolean, onClose: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, isFavorite: Boolean, onFavorite: () -> Unit, onSeek: (Long) -> Unit
) {
    var progress by remember(track.id) { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = onClose, confirmButton = {},
        title = { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Close player") }; Text("Now playing")
        } },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text("♫", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(18.dp))
                Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Slider(value = progress, onValueChange = { progress = it }, onValueChangeFinished = { onSeek((track.durationMs * progress).toLong()) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0:00", style = MaterialTheme.typography.labelSmall); Text(track.durationLabel, style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Shuffle") }
                    IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous") }
                    FilledIconButton(onClick = onToggle, modifier = Modifier.size(60.dp)) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", Modifier.size(32.dp))
                    }
                    IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
                    IconButton(onClick = onRepeat) { Icon(Icons.Default.Repeat, "Repeat") }
                }
                TextButton(onClick = onFavorite) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.width(6.dp)); Text(if (isFavorite) "Liked" else "Add to liked songs")
                }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}
