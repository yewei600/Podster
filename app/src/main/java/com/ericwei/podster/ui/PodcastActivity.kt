/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ericwei.podster.R
import com.ericwei.podster.repository.ItunesRepo
import com.ericwei.podster.service.ItunesService

class PodcastActivity : AppCompatActivity() {
    private val TAG = PodcastActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)

        val itunesService = ItunesService.instance
        val itunesRepo = ItunesRepo(itunesService)
        itunesRepo.searchByTerm("Android developer") {
            Log.i(TAG, "Results = $it")
        }
    }
}