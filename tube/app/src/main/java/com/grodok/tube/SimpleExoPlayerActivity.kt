package com.grodok.tube;

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.Util
import java.lang.Exception


class SimpleExoPlayerActivity : AppCompatActivity(), Player.EventListener {
    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var mPath:String? = null
    private var mPlayerView:PlayerView? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        mPlayerView = findViewById<PlayerView>(R.id.player_view)
        mPath = intent.getStringExtra("path")
        if(mPath == null){
            this.finish()
        }
        findViewById<Button>(R.id.close).setOnClickListener {
            finish();
        }
    }
    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }
    public override fun onResume() {
        super.onResume()
     //   hideSystemUi()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }
    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }
    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }
    private fun initializePlayer() {
        if (player == null) {
            val trackSelector = DefaultTrackSelector(this)
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )
            player = SimpleExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
        }
        mPlayerView?.player = player
        mPlayerView?.requestFocus()


        player?.playWhenReady = playWhenReady
        player?.seekTo(currentWindow, playbackPosition)
        player?.addListener(this)
        player?.setRepeatMode(Player.REPEAT_MODE_ALL)

        val dataSpec = DataSpec(Uri.parse(mPath));
        val fileDataSource =  FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (e:Exception) {
            e.printStackTrace();
        }

        val factory =  object:DataSource.Factory {
            override fun createDataSource(): DataSource {
                return fileDataSource
            }
        };
        val audioSource =  ExtractorMediaSource(fileDataSource.getUri()!!,
            factory,  DefaultExtractorsFactory(), null, null);
        player?.prepare(audioSource)
    }

    private fun releasePlayer() {
        if (player != null) {
            playbackPosition = player?.currentPosition!!
            currentWindow = player?.currentWindowIndex!!
            playWhenReady = player?.playWhenReady!!
            player?.removeListener(this)
            player?.release()
            player = null
        }
    }

    /**
     * set fullscreen
     */
    private fun hideSystemUi() {
        mPlayerView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        error.printStackTrace()
        super.onPlayerError(error)
        finish()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        val stateString: String = when (playbackState) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE "
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
            else -> "UNKNOWN_STATE"
        }
        mPlayerView?.keepScreenOn = !(playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED || !playWhenReady)
        Log.d(
            TAG, "changed state to " + stateString
                    + " playWhenReady: " + playWhenReady
        )
    }

    companion object {
        private val TAG = "DIPANKAR"
        fun startPlayerActivity(activity: Activity, path: String) {
            val myIntent = Intent(activity, SimpleExoPlayerActivity::class.java)
            myIntent.putExtra("path", path) //Optional parameters
            activity.startActivity(myIntent)
        }
    }
}
