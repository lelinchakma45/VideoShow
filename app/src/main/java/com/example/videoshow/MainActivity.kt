package com.example.videoshow

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.videoshow.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private val isPlaying get() = player?.isPlaying ?: false
    private val handler = Handler(Looper.getMainLooper())
    private var isFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializePlayer()
        initializeControls()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
//        val mediaItem = MediaItem.Builder().setUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4").setMimeType(MimeTypes.APPLICATION_MP4).build()
//        val mediaItem = MediaItem.Builder().setUri("https://hls05.videodelivery.shop/hls05/My-Demon-Ep1/index.m3u8").setMimeType(MimeTypes.APPLICATION_MP4).build()

//        val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(mediaItem)
        val mediaItem = MediaItem.Builder().setUri("https://n8.dramaticreadings.com:7066/hls/maloskyscric.m3u8?md5=eZAH7bQcjl50i2Ks44Ha5A&expires=1718226515").setMimeType(MimeTypes.APPLICATION_M3U8).build()

        // Create a media source for HLS and pass the media item
        val mediaSource = HlsMediaSource.Factory(DefaultDataSource.Factory(this)).createMediaSource(mediaItem)

        player!!.apply {
            setMediaSource(mediaSource)
            playWhenReady = true
            seekTo(0, 0L)
            prepare()
        }.also {
            binding.playerView.player = it
        }

        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButtons()
            }
            override fun onPositionDiscontinuity(reason: Int) {
                updateSeekBar()
            }
            override fun onPlaybackStateChanged(state: Int) {
                updateSeekBar()
            }
        })
    }

    private fun initializeControls() {
        binding.playerView.findViewById<ImageView>(R.id.btn_pause).setOnClickListener {
            if (isPlaying) {
                player?.pause()
            } else {
                player?.play()
            }
        }

        binding.playerView.findViewById<ImageView>(R.id.btn_rewind).setOnClickListener {
            player?.seekBack()
        }

        binding.playerView.findViewById<ImageView>(R.id.btn_forward).setOnClickListener {
            player?.seekForward()
        }

        binding.playerView.findViewById<ImageView>(R.id.maximize).setOnClickListener {
            if (isFullScreen) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            isFullScreen = !isFullScreen
        }

        val seekBar = binding.playerView.findViewById<SeekBar>(R.id.seek_bar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateSeekBar()
    }

    private fun updateSeekBar() {
        val seekBar = binding.playerView.findViewById<SeekBar>(R.id.seek_bar)
        val currentTimeTextView = binding.playerView.findViewById<TextView>(R.id.tv_current_time)
        val totalTimeTextView = binding.playerView.findViewById<TextView>(R.id.tv_total_duration)

        player?.let { player ->
            seekBar.max = player.duration.toInt()
            handler.post(object : Runnable {
                override fun run() {
                    seekBar.progress = player.currentPosition.toInt()
                    currentTimeTextView.text = formatTime(player.currentPosition)
                    totalTimeTextView.text = formatTime(player.duration)
                    handler.postDelayed(this, 1000)
                }
            })
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun updatePlayPauseButtons() {
        val pauseButton = binding.playerView.findViewById<ImageView>(R.id.btn_pause)
        if (isPlaying) {
            pauseButton.setImageResource(R.drawable.pause_24px) // Assuming pause_24px is the pause icon
        } else {
            pauseButton.setImageResource(R.drawable.play_arrow_24px_fill) // Assuming play_arrow_24px is the play icon
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.run {
            playWhenReady = false
            release()
        }
        player = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val fullIcon = binding.playerView.findViewById<ImageView>(R.id.maximize)
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fullIcon.setImageResource(R.drawable.close_fullscreen_24px)
            binding.playerView.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
            binding.playerView.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            fullIcon.setImageResource(R.drawable.fullscreen_24px)
            binding.playerView.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            binding.playerView.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }
}
