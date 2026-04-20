/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.diekoma.ytune.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.diekoma.ytune.LocalDatabase
import com.diekoma.ytune.R
import com.diekoma.ytune.constants.ChipSortTypeKey
import com.diekoma.ytune.constants.DisableBlurKey
import com.diekoma.ytune.constants.LibraryFilter
import com.diekoma.ytune.constants.PlaylistTagsFilterKey
import com.diekoma.ytune.constants.ShowTagsInLibraryKey
import com.diekoma.ytune.ui.component.ChipsRow
import com.diekoma.ytune.ui.component.TagsFilterChips
import com.diekoma.ytune.utils.rememberEnumPreference
import com.diekoma.ytune.utils.rememberPreference
import kotlin.math.exp

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    var expanded by remember { mutableStateOf(false) }

    var currentFilterInfo = remember(filterType) {
        when (filterType) {
            LibraryFilter.PLAYLISTS -> R.string.filter_playlists to R.drawable.playlist_add
            LibraryFilter.SONGS -> R.string.filter_songs to R.drawable.library_music
            LibraryFilter.ALBUMS -> R.string.filter_albums to R.drawable.album
            LibraryFilter.ARTISTS -> R.string.filter_artists to R.drawable.artist
            else -> R.string.filter_library to R.drawable.library_filled
        }
    }
    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val filterContent = @Composable {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Box {
                    Row(
                        modifier = Modifier
                            .height(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { filterType = LibraryFilter.LIBRARY },
                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 12.dp),
                            enabled = filterType != LibraryFilter.LIBRARY
                        ) {
                            Icon(
                                painter = painterResource(currentFilterInfo.second),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(currentFilterInfo.first), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(2.dp))

                        FilledTonalButton(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp,
                                topEnd = 20.dp, bottomEnd = 20.dp),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_downward),
                                contentDescription = "Switch filter",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf(
                                LibraryFilter.LIBRARY to stringResource(R.string.libary),
                                LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                                LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                                LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                                LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                            ).forEach { (type, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(labelRes) },
                                    leadingIcon =  {
                                        val iconRes = when (type) {
                                            LibraryFilter.PLAYLISTS -> R.drawable.queue_music
                                            LibraryFilter.SONGS -> R.drawable.music_note
                                            LibraryFilter.ALBUMS -> R.drawable.album
                                            LibraryFilter.ARTISTS -> R.drawable.person
                                            else -> R.drawable.library_filled
                                        }
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (filterType == type) {
                                            Icon(
                                                painter = painterResource(R.drawable.check),
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        filterType = type
                                        expanded = false
                                    },
                                )
                            }
                        }
                        if (showTagsInLibrary) {
                            TagsFilterChips(
                                database = database,
                                selectedTags = selectedTagIds,
                                onTagToggle = { tag ->
                                    val newTags = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                    onSelectedTagsFilterChange(newTags.joinToString(","))
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Capture M3 Expressive colors from theme outside drawBehind
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // M3E Mesh gradient background layer at the top
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f) // Cover top 70% of screen
                    .align(Alignment.TopCenter)
                    .zIndex(-1f) // Place behind all content
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    
                    // Create mesh gradient with 5 color blobs for more variation
                    // First color blob - top left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color1.copy(alpha = 0.38f),
                                color1.copy(alpha = 0.24f),
                                color1.copy(alpha = 0.14f),
                                color1.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.15f, height * 0.1f),
                            radius = width * 0.55f
                        )
                    )
                    
                    // Second color blob - top right
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color2.copy(alpha = 0.34f),
                                color2.copy(alpha = 0.2f),
                                color2.copy(alpha = 0.11f),
                                color2.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.85f, height * 0.2f),
                            radius = width * 0.65f
                        )
                    )
                    
                    // Third color blob - middle left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color3.copy(alpha = 0.3f),
                                color3.copy(alpha = 0.17f),
                                color3.copy(alpha = 0.09f),
                                color3.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.3f, height * 0.45f),
                            radius = width * 0.6f
                        )
                    )
                    
                    // Fourth color blob - middle right
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color4.copy(alpha = 0.26f),
                                color4.copy(alpha = 0.14f),
                                color4.copy(alpha = 0.08f),
                                color4.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.7f, height * 0.5f),
                            radius = width * 0.7f
                        )
                    )
                    
                    // Fifth color blob - bottom center (helps with smooth fade)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color5.copy(alpha = 0.22f),
                                color5.copy(alpha = 0.12f),
                                color5.copy(alpha = 0.06f),
                                color5.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(width * 0.5f, height * 0.75f),
                            radius = width * 0.8f
                        )
                    )
                    
                    // Add a final vertical gradient overlay to ensure smooth bottom fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.22f),
                                surfaceColor.copy(alpha = 0.55f),
                                surfaceColor
                            ),
                            startY = height * 0.4f,
                            endY = height
                        )
                    )
                }
        ) {}
        }

        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                filterContent
            )

            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                filterContent
            )

            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                filterContent
            )
        }
    }
}
