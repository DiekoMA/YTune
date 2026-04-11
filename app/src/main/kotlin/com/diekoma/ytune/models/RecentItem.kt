package com.diekoma.ytune.models

import com.diekoma.ytune.innertube.models.SongItem

sealed class RecentItem {
    data class Query(val text: String) : RecentItem()
    data class Song(val songItem: SongItem) : RecentItem()
}