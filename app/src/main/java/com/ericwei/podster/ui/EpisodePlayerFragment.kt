/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.ericwei.podster.R
import com.ericwei.podster.service.PodplayMediaCallback
import com.ericwei.podster.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.ericwei.podster.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.ericwei.podster.service.PodplayMediaService
import com.ericwei.podster.util.HtmlUtils
import com.ericwei.podster.viewmodel.EpisodeViewData
import com.ericwei.podster.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_episode_player.*

class EpisodePlayerFragment : Fragment() {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var playerSpeed: Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            podcastViewModel.activeEpisodeViewData?.isVideo ?: false
        } else {
            false
        }
        if (!isVideo) {
            initMediaBrowser()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_episode_player, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        if (isVideo) {
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo) {
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }
        if (isVideo) {
            mediaPlayer?.setDisplay(null)
        }
        if (!fragmentActivity.isChangingConfigurations) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun setupControls() {
        playToggleButton.setOnClickListener {
            togglePlayPause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            speedButton.visibility = View.INVISIBLE
        }
        forwardButton.setOnClickListener { seekBy(30) }
        replayButton.setOnClickListener { seekBy(-10) }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                currentTimeTextView.text = DateUtils.formatElapsedTime((progress / 1000).toLong())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                draggingScrubber = false
                val fragmentActivity = activity as FragmentActivity
                val controller =
                    MediaControllerCompat.getMediaController(fragmentActivity)
                if (controller.playbackState != null) {
                    controller.transportControls.seekTo(seekBar.progress.toLong())
                } else {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun updateControls() {
        episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title
        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        episodeDescTextView.text = descSpan
        episodeDescTextView.movementMethod = ScrollingMovementMethod()
        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(podcastViewModel.activePodcastViewData?.imageUrl)
            .into(episodeImageView)
        speedButton.text = "$playerSpeed"
        mediaPlayer?.let {
            updateControlsFromController()
        }
    }

    private fun startPlaying(episodeViewData: EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.imageUrl)
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(
            fragmentActivity,
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            MediaBrowserCallBacks(), null
        )
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun togglePlayPause() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
        }
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        playToggleButton.isActivated = isPlaying
        val progress = position.toInt()
        seekBar.progress = progress
        speedButton.text = "${playerSpeed}x"
        if (isPlaying) {
            if (isVideo) {
                setupVideoUI()
            }
            animateSrubber(progress, speed)
        }
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f
        if (playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }
        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)
        speedButton.text = "${playerSpeed}x"
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(episodeDuration / 1000)
        seekBar.max = episodeDuration.toInt()
    }

    private fun animateSrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()
        if (timeRemaining < 0) {
            return;
        }
        progressAnimator = ValueAnimator.ofInt(
            progress, episodeDuration.toInt()
        )
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(
                    controller.playbackState.state,
                    controller.playbackState.position, playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")
            mediaSession?.setMediaButtonReceiver(null)
        }
        registerMediaController(mediaSession!!.sessionToken)
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let {
                it.setAudioStreamType(AudioManager.STREAM_MUSIC)
                it.setDataSource(
                    podcastViewModel.activeEpisodeViewData?.mediaUrl
                )
                it.setOnPreparedListener {
                    val fragmentActivity = activity as FragmentActivity
                    val episodeMediaCallback = PodplayMediaCallback(
                        fragmentActivity, mediaSession!!, it
                    )
                    mediaSession!!.setCallback(episodeMediaCallback)
                    setSurfaceSize()
                    if (playOnPrepare) {
                        togglePlayPause()
                    }
                }
                it.prepareAsync()
            }
        } else {
            setSurfaceSize()
        }
    }

    private fun setSurfaceSize() {
        val mediaPlayer = mediaPlayer ?: return
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight
        val parent = videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height
        val layoutAspectRatio = containerWidth.toFloat() / containerHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight
        val layoutParams = videoSurfaceView.layoutParams
        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width = (containerHeight * videoAspectRatio).toInt()
        }
        videoSurfaceView.layoutParams = layoutParams
    }

    private fun initVideoPlayer() {
        videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = videoSurfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(
                var1: SurfaceHolder, var2: Int,
                var3: Int, var4: Int
            ) {
            }

            override fun surfaceDestroyed(var1: SurfaceHolder) {
            }
        })
    }

    private fun setupVideoUI() {
        episodeDescTextView.visibility = View.INVISIBLE
        headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()
        playerControls.setBackgroundColor(Color.argb(255 / 2, 0, 0, 0))
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println(
                "metadata changed to ${
                    metadata?.getString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI
                    )
                }"
            )
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            println("state changed to $state")
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)
        }
    }

    inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
            updateControlsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
        }
    }

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }
}