/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.ericwei.podster.R
import com.ericwei.podster.adapter.PodcastListAdapter
import com.ericwei.podster.db.PodPlayDatabase
import com.ericwei.podster.repository.ItunesRepo
import com.ericwei.podster.repository.PodcastRepo
import com.ericwei.podster.service.FeedService
import com.ericwei.podster.service.ItunesService
import com.ericwei.podster.viewmodel.PodcastSummaryViewData
import com.ericwei.podster.viewmodel.PodcastViewModel
import com.ericwei.podster.viewmodel.SearchViewModel
import com.ericwei.podster.worker.EpisodeUpdateWorker
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailsListener {
    private val TAG = PodcastActivity::class.simpleName
    private val searchViewModel by viewModels<SearchViewModel>()
    private val podcastViewModel by viewModels<PodcastViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setSupportActionBar(toolbar)
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        addBackStackListener()
        scheduleJobs()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu!!.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })
        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            podcastRecyclerView.visibility = View.INVISIBLE
        }
        return true
    }

    override fun onShowDetails(podcastSummaryViewData: PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar(true)
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            showProgressBar(false)
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
    }

    private fun performSearch(term: String) {
        showProgressBar(true)
        searchViewModel.searchPodcasts(term) { results ->
            showProgressBar(false)
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun showProgressBar(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    private fun setupViewModels() {
        searchViewModel.iTunesRepo = ItunesRepo(ItunesService.instance)
        podcastViewModel.podcastRepo = PodcastRepo(
            FeedService.instance,
            PodPlayDatabase.getInstance(this).podcastDao()
        )
    }

    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context, layoutManager.orientation
        )
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)
        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) showSubscribedPodcasts()
        })
    }

    private fun showSubscribedPodcasts() {
        podcastViewModel.getPodcasts()?.value?.let { podcasts ->
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as
                PodcastDetailsFragment?
        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT
        )
            .addToBackStack("DetailsFragment").commit()

        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create().show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun scheduleJobs() {
        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()
        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.REPLACE, request
        )
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.ericwei.podster.episodes"
    }
}