/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ericwei.podster.R
import com.ericwei.podster.adapter.EpisodeListAdapter
import com.ericwei.podster.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*

class PodcastDetailsFragment : Fragment() {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var episodeListAdapter: EpisodeListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl)
                .into(feedImageView)
        }
    }

    private fun setupControls() {
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episodeRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(
            episodeRecyclerView.context, layoutManager.orientation
        )
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)
        episodeListAdapter = EpisodeListAdapter(
            podcastViewModel.activePodcastViewData?.episodes
        )
        episodeRecyclerView.adapter = episodeListAdapter
    }

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }
}