/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.repository

import androidx.lifecycle.LiveData
import com.ericwei.podster.db.PodcastDao
import com.ericwei.podster.model.Episode
import com.ericwei.podster.model.Podcast
import com.ericwei.podster.service.FeedService
import com.ericwei.podster.service.RssFeedResponse
import com.ericwei.podster.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(
    private var feedService: FeedService,
    private var podcastDao: PodcastDao
) {
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)
            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {
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
        }
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()
        val podcasts = podcastDao.loadPodcastsStatic()
        for (podcast in podcasts) {
            getNewEpisodes(podcast) { newEpisodes ->
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatedPodcasts.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl,
                            podcast.feedTitle, newEpisodes.count()
                        )
                    )
                }
            }
        }
        callback(updatedPodcasts)
    }

    private fun rssResponseToPodcast(
        feedUrl: String,
        imageUrl: String, rssResponse: RssFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description = if (rssResponse.description == "") rssResponse.summary else
            rssResponse.description
        return Podcast(
            null, feedUrl, rssResponse.title, description, imageUrl,
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
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    //take a single podcast, returns a list of new episodes available
    private fun getNewEpisodes(localPodcast: Podcast, callBack: (List<Episode>) -> Unit) {
        feedService.getFeed(localPodcast.feedUrl) { response ->
            if (response != null) {
                val remotePodcast = rssResponseToPodcast(
                    localPodcast.feedUrl,
                    localPodcast.imageUrl, response
                )
                remotePodcast?.let {
                    val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
                    val newEpisodes = remotePodcast.episodes.filter { episode ->
                        localEpisodes.find {
                            episode.guid == it.guid
                        } == null
                    }
                    callBack(newEpisodes)
                }
            } else {
                callBack(listOf())
            }
        }
    }

    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)
}