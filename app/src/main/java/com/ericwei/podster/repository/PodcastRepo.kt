/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.repository

import com.ericwei.podster.model.Episode
import com.ericwei.podster.model.Podcast
import com.ericwei.podster.service.FeedService
import com.ericwei.podster.service.RssFeedResponse
import com.ericwei.podster.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: FeedService) {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        feedService.getFeed(feedUrl) { feedResponse ->
            var podcast: Podcast? = null
            if (feedResponse != null) {
                podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
            }
            GlobalScope.launch(Dispatchers.Main) {
                callback(podcast)
            }
        }
    }

    private fun rssResponseToPodcast(
        feedUrl: String,
        imageUrl: String, rssResponse: RssFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description = if (rssResponse.description == "") rssResponse.summary else
            rssResponse.description
        return Podcast(
            feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, rssItemsToEpisodes(items)
        )
    }

    private fun rssItemsToEpisodes(
        episodeResponses:
        List<RssFeedResponse.EpisodeResponse>
    ): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }
}