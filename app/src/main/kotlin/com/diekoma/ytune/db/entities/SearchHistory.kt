/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.diekoma.ytune.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diekoma.ytune.innertube.models.Thumbnail

@Entity(
    tableName = "search_history",
    indices = [
        Index(
            value = ["query"],
            unique = true,
        ),
    ],
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val videoId: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
