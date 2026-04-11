package com.diekoma.ytune.ui.component

import android.service.autofill.OnClickAction
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.room.Query
import coil3.compose.AsyncImage
import coil3.decode.BlackholeDecoder
import com.diekoma.ytune.R
import com.diekoma.ytune.db.entities.SearchHistory
import com.diekoma.ytune.innertube.models.Artist
import com.diekoma.ytune.innertube.models.SongItem
import com.diekoma.ytune.models.RecentItem
import com.diekoma.ytune.ui.menu.YouTubeSongMenu

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentSearchCard(
    history: SearchHistory,
    pureBlack: Boolean,
    navController: NavController,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current

    Surface(
        modifier = Modifier
            .width(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)


                    if (history.videoId != null && history.thumbnailUrl != null) {
                        menuState.show {
                            if (history.videoId != null && history.thumbnailUrl != null) {
                                val song = SongItem(
                                    id = history.videoId,
                                    title = history.title ?: history.query,
                                    artists = listOf(Artist(id = null, name = history.subtitle ?: "")),
                                    thumbnail = history.thumbnailUrl ?: "",
                                    album = null,
                                    duration = null
                                )
                                YouTubeSongMenu(
                                    song = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        }
                    }

                }
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .width(110.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (pureBlack) Color.DarkGray else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (history.videoId != null && history.thumbnailUrl != null) {
                    AsyncImage(
                        model = history.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }else {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = null,
                        tint = if (pureBlack) Color.White.copy(0.7f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = history.query,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = (if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface).copy(0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
    }
    }
}