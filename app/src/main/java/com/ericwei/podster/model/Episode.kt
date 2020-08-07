/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

//for display, management or playback of an episode
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class Episode(
    @PrimaryKey var guid: String = "",
    var podcastId: Long? = null,
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "",
    var mimeType: String = "",
    var releaseDate: Date = Date(),
    var duration: String = ""
)