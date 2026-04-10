package com.diekoma.ytune.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diekoma.ytune.innertube.models.AlbumItem
import com.diekoma.ytune.innertube.models.Artist
import com.diekoma.ytune.innertube.models.ArtistItem
//import moe.koiverse.archivetune.innertube.models.EpisodeItem
import com.diekoma.ytune.innertube.models.PlaylistItem
//import moe.koiverse.archivetune.innertube.models.PodcastItem
import com.diekoma.ytune.innertube.models.SongItem
import com.diekoma.ytune.innertube.models.YTItem

@Entity(tableName = "speed_dial_item")
data class SpeedDialItem (
    @PrimaryKey val id: String,
    val secondaryId: String? = null,
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val type: String, // "SONG", "ALBUM", "ARTIST", "PLAYLIST", "LOCAL_PLAYLIST"
    val explicit: Boolean = false,
    val createDate: Long = System.currentTimeMillis()
) {
    fun toYTItem(): YTItem {
        return when (type) {
            "SONG" -> SongItem(
                id = id,
                title = title,
                artists = subtitle?.split(", ")?.map { Artist(name = it, id = null) } ?: emptyList(),
                thumbnail = thumbnailUrl ?: "",
                explicit = explicit
            )
            "ALBUM" -> AlbumItem(
                browseId = id,
                playlistId = secondaryId ?: "",
                title = title,
                artists = subtitle?.split(", ")?.map { Artist(name = it, id = null) },
                thumbnail = thumbnailUrl ?: "",
                explicit = explicit
            )
            "ARTIST" -> ArtistItem(
                id = id,
                title = title,
                thumbnail = thumbnailUrl,
                shuffleEndpoint = null,
                radioEndpoint = null
            )
            "PLAYLIST", "LOCAL_PLAYLIST" -> PlaylistItem(
                id = id,
                title = title,
                author = subtitle?.let { Artist(name = it, id = null) },
                songCountText = null,
                thumbnail = thumbnailUrl,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null
            )
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }

    companion object {
        fun fromYTItem(item: YTItem): SpeedDialItem {
            return when (item) {
                is SongItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    subtitle = item.artists.joinToString(", ") { it.name },
                    thumbnailUrl = item.thumbnail,
                    type = "SONG",
                    explicit = item.explicit
                )
                is AlbumItem -> SpeedDialItem(
                    id = item.browseId,
                    secondaryId = item.playlistId,
                    title = item.title,
                    subtitle = item.artists?.joinToString(", ") { it.name },
                    thumbnailUrl = item.thumbnail,
                    type = "ALBUM",
                    explicit = item.explicit
                )
                is ArtistItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    type = "ARTIST"
                )
                is PlaylistItem -> SpeedDialItem(
                    id = item.id,
                    title = item.title,
                    subtitle = item.author?.name,
                    thumbnailUrl = item.thumbnail,
                    type = "PLAYLIST"
                )
//                is PodcastItem -> SpeedDialItem(
//                    id = item.id,
//                    title = item.title,
//                    subtitle = item.author?.name,
//                    thumbnailUrl = item.thumbnail,
//                    type = "PLAYLIST"
//                )
//                is EpisodeItem -> SpeedDialItem(
//                    id = item.id,
//                    title = item.title,
//                    subtitle = item.author?.name,
//                    thumbnailUrl = item.thumbnail,
//                    type = "SONG",
//                    explicit = item.explicit
//                )
            }
        }
    }
}