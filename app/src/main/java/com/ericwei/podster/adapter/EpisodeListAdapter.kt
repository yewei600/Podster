/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericwei.podster.R
import com.ericwei.podster.util.DateUtils
import com.ericwei.podster.util.HtmlUtils
import com.ericwei.podster.viewmodel.EpisodeViewData
import kotlinx.android.synthetic.main.episode_item.view.*

class EpisodeListAdapter(
    private var episodeViewList: List<EpisodeViewData>?
) :
    RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var episodeViewData: EpisodeViewData? = null
        val titleTextView: TextView = v.titleView
        val descTextView: TextView = v.descView
        val durationTextView: TextView = v.durationView
        val releaseDateTextView: TextView =
            v.releaseDateView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            EpisodeListAdapter.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.episode_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]
        holder.episodeViewData = episodeView
        holder.titleTextView.text = episodeView.title
        holder.descTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releaseDate?.let {
            DateUtils.dateToShortDate(it)
        }
    }

    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }
}