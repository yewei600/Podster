/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.repository

import com.ericwei.podster.model.Podcast

class PodcastRepo {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        callback(Podcast(feedUrl, "No Name", "No description", "No image"))
    }
}