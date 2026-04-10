/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.diekoma.ytune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.diekoma.ytune.innertube.utils.parseCookieString
import com.diekoma.ytune.LocalPlayerAwareWindowInsets
import com.diekoma.ytune.LocalPlayerConnection
import com.diekoma.ytune.R
import com.diekoma.ytune.constants.InnerTubeCookieKey
import com.diekoma.ytune.constants.DisableBlurKey
import com.diekoma.ytune.constants.RandomizeHomeOrderKey
import com.diekoma.ytune.constants.ShowHomeCategoryChipsKey
import com.diekoma.ytune.ui.component.ChipsRow
import com.diekoma.ytune.ui.component.LocalBottomSheetPageState
import com.diekoma.ytune.ui.component.SpeedDialGridItem
import com.diekoma.ytune.ui.component.LocalMenuState
import com.diekoma.ytune.ui.component.NavigationTitle
import com.diekoma.ytune.ui.utils.SnapLayoutInfoProvider
import com.diekoma.ytune.utils.rememberPreference
import com.diekoma.ytune.viewmodels.HomeViewModel
import kotlin.random.Random



sealed class HomeSection(
    val id: String,
    val baseWeight: Int,
) {
    data object SpeedDial : HomeSection("speed_dial", 100)

    data object QuickPicks : HomeSection("quick_picks", 90)

    data object DailyDiscover : HomeSection("daily_discover", 80)

    data object KeepListening : HomeSection("keep_listening", 50)

    data object AccountPlaylists : HomeSection("account_playlists", 40)

    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)

    data object FromTheCommunity : HomeSection("from_the_community", 20)

    data class SimilarRecommendation(
        val index: Int,
    ) : HomeSection("similar_recommendation_$index", 10)

    data class HomePageSection(
        val index: Int,
    ) : HomeSection("home_page_section_$index", 10)

    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, true)
    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }


    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDialItems by viewModel.speedDialItems.collectAsState()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()

    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val sections = remember(
        speedDialItems, quickPicks, keepListening, forgottenFavorites, homePage, randomizeHomeOrder, randomSeed
    ) {
        val list = mutableListOf<HomeSection>()

        if (speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
        if (quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        homePage?.sections?.forEachIndexed { index, _ ->
            list.add(HomeSection.HomePageSection(index))
        }

        if (randomizeHomeOrder) {
            val random = Random(randomSeed)
            list.sortByDescending { it.baseWeight * random.nextFloat() }
        } else {
            list.sortByDescending { it.baseWeight }
        }
        list
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) {
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
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
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        // Create mesh gradient with 5 color blobs for more variation
                        // First color blob - top left
                        val brush1 = Brush.radialGradient(
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

                        // Second color blob - top right
                        val brush2 = Brush.radialGradient(
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

                        // Third color blob - middle left
                        val brush3 = Brush.radialGradient(
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

                        // Fourth color blob - middle right
                        val brush4 = Brush.radialGradient(
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

                        // Fifth color blob - bottom center (helps with smooth fade)
                        val brush5 = Brush.radialGradient(
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

                        // Add a final vertical gradient overlay to ensure smooth bottom fade
                        val overlayBrush = Brush.verticalGradient(
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

                        onDrawBehind {
                            drawRect(brush = brush1)
                            drawRect(brush = brush2)
                            drawRect(brush = brush3)
                            drawRect(brush = brush4)
                            drawRect(brush = brush5)
                            drawRect(brush = overlayBrush)
                        }
                    }
            ) {}
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                if (showHomeCategoryChips) {
                    item {
                        ChipsRow(
                            chips = homePage?.chips.orEmpty().map { it to it.title },
                            currentValue = selectedChip,
                            onValueUpdate = {
                                viewModel.toggleChip(it)
                            }
                        )
                    }
                }

                items(sections, key = { it.id }) { section ->
                    when (section) {
                        is HomeSection.SpeedDial -> {
                            Column(Modifier.animateItem()) {
                                NavigationTitle(title = stringResource(R.string.speed_dial), modifier = Modifier.animateItem())
                                SpeedDialSection(
                                    speedDialItems = speedDialItems,
                                    pinnedSpeedDialItems = pinnedSpeedDialItems,
                                    mediaMetadata = mediaMetadata,
                                    isPlaying = isPlaying,
                                    navController = navController,
                                    playerConnection = playerConnection,
                                    menuState = menuState,
                                    haptic = haptic
                                )
                            }
                        }

                        is HomeSection.QuickPicks -> {
//                            NavigationTitle(
//                                title = stringResource(R.string.keep_listening),
//                                modifier = Modifier.animateItem()
//                            )
                            QuickPicksSection(
                                quickPicks = quickPicks!!,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic
                            )
                        }

                        is HomeSection.KeepListening -> {
                            Column(Modifier.animateItem()) {
                                NavigationTitle(title = stringResource(R.string.keep_listening))
                                KeepListeningSection(
                                    keepListening = keepListening!!,
                                    mediaMetadata = mediaMetadata,
                                    isPlaying = isPlaying,
                                    navController = navController,
                                    playerConnection = playerConnection,
                                    menuState = menuState,
                                    haptic = haptic,
                                    scope = scope
                                )
                            }
                        }
                        is HomeSection.ForgottenFavorites -> {
                            Column(Modifier.animateItem()) {
                                NavigationTitle(
                                    title = stringResource(R.string.forgotten_favorites),
                                    modifier = Modifier.animateItem()
                                )
                                ForgottenFavoritesSection(
                                    forgottenFavorites = forgottenFavorites!!,
                                    mediaMetadata = mediaMetadata,
                                    isPlaying = isPlaying,
                                    horizontalLazyGridItemWidth = horizontalLazyGridItemWidth,
                                    lazyGridState = forgottenFavoritesLazyGridState,
                                    snapLayoutInfoProvider = forgottenFavoritesSnapLayoutInfoProvider,
                                    navController = navController,
                                    playerConnection = playerConnection,
                                    menuState = menuState,
                                    haptic = haptic
                                )
                            }
                        }

                        is HomeSection.AccountPlaylists -> {
                            this@LazyColumn.AccountPlaylistsContainer(
                                viewModel = viewModel,
                                accountName = accountName,
                                accountImageUrl = url,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                scope = scope
                            )
                        }

                        is HomeSection.SimilarRecommendation -> {
                            this@LazyColumn.SimilarRecommendationsContainer(
                                viewModel = viewModel,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                scope = scope
                            )
                        }

                        is HomeSection.HomePageSection -> {
                            val data = homePage?.sections?.getOrNull(section.index) ?: return@items
                            Column(Modifier.animateItem()) {
                                HomePageSectionTitle(
                                    section = data,
                                    navController = navController,
                                    modifier = Modifier.animateItem()
                                )

                                HomePageSectionContent(
                                    section = data,
                                    mediaMetadata = mediaMetadata,
                                    isPlaying = isPlaying,
                                    navController = navController,
                                    playerConnection = playerConnection,
                                    menuState = menuState,
                                    haptic = haptic,
                                    scope = scope
                                )
                            }
                        }
                        else -> {}
                    }
                }
                if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                    item { HomeLoadingShimmer(modifier = Modifier.animateItem()) }
                }
            }


            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    }
}
