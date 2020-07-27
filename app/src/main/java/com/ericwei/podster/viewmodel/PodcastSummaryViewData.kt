/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.viewmodel

data class PodcastSummaryViewData(
    var name: String? = "",
    var lastUpdated: String? = "",
    var imageUrl: String? = "",
    var feedUrl: String? = ""
)