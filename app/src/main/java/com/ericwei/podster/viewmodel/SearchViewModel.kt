/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ericwei.podster.repository.ItunesRepo
import com.ericwei.podster.service.PodcastResponse
import com.ericwei.podster.util.DateUtils

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    var iTunesRepo: ItunesRepo? = null

    private fun itunesPodcastToPodcastSummaryView(itunesPodcast: PodcastResponse.ItunesPodcast):
            PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl100,
            itunesPodcast.feedUrl
        )
    }

    fun searchPodcasts(term: String, callback: (List<PodcastSummaryViewData>) -> Unit) {
        iTunesRepo?.searchByTerm(term) { results ->
            if (results == null) {
                callback(emptyList())
            } else {
                val searchViews = results.map { podcast ->
                    itunesPodcastToPodcastSummaryView(podcast)
                }
                callback(searchViews)
            }
        }
    }

}