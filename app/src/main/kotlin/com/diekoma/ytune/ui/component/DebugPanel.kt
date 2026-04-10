/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.diekoma.ytune.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.diekoma.ytune.R

/**
 * Returns a `Material3SettingsItem` that can be placed inside a `Material3SettingsGroup`.
 * The caller should supply composables or values for the dynamic content.
 */
@Composable
fun DebugPanelItem(
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
): Material3SettingsItem {
    return Material3SettingsItem(
        icon = painterResource(R.drawable.info),
        title = title,
        description = description,
        trailingContent = trailingContent,
        isHighlighted = true
    )
}
