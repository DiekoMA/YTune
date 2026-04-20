/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.diekoma.ytune.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.media3.exoplayer.offline.Download.STATE_STOPPED
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.diekoma.ytune.innertube.YouTube
import com.diekoma.ytune.LocalDatabase
import com.diekoma.ytune.LocalDownloadUtil
import com.diekoma.ytune.LocalPlayerConnection
import com.diekoma.ytune.R
import com.diekoma.ytune.constants.ArtistSeparatorsKey
import com.diekoma.ytune.constants.ListItemHeight
import com.diekoma.ytune.constants.ListThumbnailSize
import com.diekoma.ytune.db.entities.Album
import com.diekoma.ytune.db.entities.Song
import com.diekoma.ytune.db.entities.SpeedDialItem
import com.diekoma.ytune.extensions.toMediaItem
import com.diekoma.ytune.playback.ExoDownloadService
import com.diekoma.ytune.playback.queues.ListQueue
import com.diekoma.ytune.ui.component.AlbumListItem
import com.diekoma.ytune.ui.component.ListDialog
import com.diekoma.ytune.ui.component.ListItem
import com.diekoma.ytune.ui.component.Material3SettingsGroup
import com.diekoma.ytune.ui.component.MenuSurfaceSection
import com.diekoma.ytune.ui.component.NewAction
import com.diekoma.ytune.ui.component.NewActionGrid
import com.diekoma.ytune.ui.component.SongListItem
import com.diekoma.ytune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("MutableCollectionMutableState")
@Composable
fun AlbumMenu(
    originalAlbum: Album,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val libraryAlbum by database.album(originalAlbum.id).collectAsState(initial = originalAlbum)

    val album = libraryAlbum ?: originalAlbum
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }


    val isPinned by database.speedDialDao.isPinned(album.id).collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.albumSongs(album.id).collect {
            songs = it
        }
    }

    var downloadState by remember {
        mutableStateOf(STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                    STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == STATE_QUEUED ||
                                downloads[it.id]?.state == STATE_DOWNLOADING ||
                                downloads[it.id]?.state == STATE_COMPLETED
                    }
                ) {
                    STATE_DOWNLOADING
                } else {
                    STATE_STOPPED
                }
        }
    }

    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: com.diekoma.ytune.db.entities.ArtistEntity?
    )

    val splitArtists = remember(album.artists, artistSeparators) {
        if (artistSeparators.isEmpty()) {
            album.artists.map { SplitArtist(it.name, it) }
        } else {
            val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
            album.artists.flatMap { artist ->
                val parts = artist.name.split(separatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    parts.mapIndexed { index, name ->
                        SplitArtist(name, if (index == 0) artist else null)
                    }
                } else {
                    listOf(SplitArtist(artist.name, artist))
                }
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            songs.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message = when {
                songCount == 1 && playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                songCount > 1 && playlistNames.size == 1 -> context.getString(R.string.added_n_songs_to_playlist, songCount, playlistNames.first())
                songCount == 1 -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
                else -> context.getString(R.string.added_n_songs_to_n_playlists, songCount, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                    Modifier
                        .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                SongListItem(song = song)
            }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = splitArtists.distinctBy { it.name },
                key = { it.name },
            ) { splitArtist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .height(ListItemHeight)
                        .clickable {
                            splitArtist.originalArtist?.let { artist ->
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = splitArtist.originalArtist?.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                            Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        text = splitArtist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)){
        AlbumListItem(
            album = album,
            showLikedIcon = false,
            badges = {},
            trailingContent = {
                IconButton(
                    onClick = {
                        database.query {
                            update(album.album.toggleLike())
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(if (album.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (album.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null,
                    )
                }
            },
        )
    }

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            // Enhanced Action Grid using NewMenuComponents
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)){
                NewActionGrid(
                    actions = listOf(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.play),
                            onClick = {
                                onDismiss()
                                if (songs.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = album.album.title,
                                            items = songs.map(Song::toMediaItem)
                                        )
                                    )
                                }
                            }
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.shuffle),
                            onClick = {
                                onDismiss()
                                if (songs.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = album.album.title,
                                            items = songs.shuffled().map(Song::toMediaItem)
                                        )
                                    )
                                }
                            },
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.share),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.share),
                            onClick = {
                                onDismiss()
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${album.album.playlistId}")
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        )
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
                )
            }
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play_next)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playNext(songs.map { it.toMediaItem() })
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        playerConnection.addToQueue(songs.map { it.toMediaItem() })
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        showChoosePlaylistDialog = true
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = {
                        Text(
                            text = if (isPinned) stringResource(R.string.unpin_from_speed_dial) else stringResource(
                                R.string.pin_to_speed_dial
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (isPinned) {
                                database.speedDialDao.delete(album.id)
                            } else {
                                database.speedDialDao.insert(
                                    SpeedDialItem(
                                        id = album.id,
                                        secondaryId = album.album.playlistId,
                                        title = album.album.title,
                                        subtitle = album.artists.joinToString(", ") { it.name },
                                        thumbnailUrl = album.album.thumbnailUrl,
                                        type = "ALBUM",
                                        explicit = album.album.explicit,
                                    ),
                                )
                            }
                        }
                        onDismiss()
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
        item {
            when (downloadState) {
                STATE_COMPLETED -> {
                    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.remove_download),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.offline),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
                STATE_QUEUED, STATE_DOWNLOADING -> {
                    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.downloading)) },
                            leadingContent = {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            modifier = Modifier.clickable {
                                songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
                else -> {
                    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.action_download)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                songs.forEach { song ->
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.song.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                            navController.navigate("artist/${splitArtists[0].originalArtist!!.id}")
                            onDismiss()
                        } else {
                            showSelectArtistDialog = true
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp, end = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.refetch)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                        )
                    },
                    modifier = Modifier.clickable {
                        refetchIconDegree -= 360
                        scope.launch(Dispatchers.IO) {
                            YouTube.album(album.id).onSuccess {
                                database.transaction {
                                    update(album.album, it, album.artists)
                                }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
