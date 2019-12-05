package tv.promethean.sdk.sample

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import tv.promethean.ptvsdk.OverlayManager
import tv.promethean.ptvsdk.interfaces.PlayerChangeListener
import tv.promethean.ptvsdk.models.OverlayData

private const val SAMPLE_VIDEO_URL = "http://184.72.239.149/vod/smil:BigBuckBunny.smil/playlist.m3u8"
private const val SAMPLE_CHANNEL_ID = "5c50eefce6f94249a2e104b3"
private const val SAMPLE_STREAM_ID = "5c5273cd5b88da1e6943200b"

class MainActivity : AppCompatActivity() {

    private var exoplayerView: SimpleExoPlayerView? = null
    private var exoplayer: SimpleExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var playbackStateBuilder: PlaybackStateCompat.Builder? = null
    private var overlayManager: OverlayManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        exoplayerView = findViewById(R.id.simple_exo_player_view)
        init(savedInstanceState)
    }

    private fun init(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            initializePlayer()
            initializeOverlays()
        }
    }

    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector()
        val userAgent = Util.getUserAgent(baseContext, "Exo")
        val mediaUri = Uri.parse(SAMPLE_VIDEO_URL)
        val mediaSource = HlsMediaSource(mediaUri, DefaultDataSourceFactory(baseContext, userAgent), null, null)
        val componentName = ComponentName(baseContext, "Exo")

        exoplayer = ExoPlayerFactory.newSimpleInstance(baseContext, trackSelector)
        exoplayerView?.player = exoplayer
        exoplayerView?.useController = true
        exoplayer?.prepare(mediaSource)

        playbackStateBuilder = PlaybackStateCompat.Builder()
        playbackStateBuilder?.setActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_FAST_FORWARD)

        mediaSession = MediaSessionCompat(baseContext, "ExoPlayer", componentName, null)
        mediaSession?.setPlaybackState(playbackStateBuilder?.build())
        mediaSession?.isActive = true
    }

    private fun initializeOverlays() {
        val overlayData = OverlayData(
            channelId = SAMPLE_CHANNEL_ID,
            streamId = SAMPLE_STREAM_ID)

        // Instantiate overlay manager.
        overlayManager = OverlayManager(this, R.id.ptv_overlay_view, overlayData)

        // Set the player position for VOD playback.
        overlayManager?.addPlayerListener(object : PlayerChangeListener {
            override val currentPosition: Long?
                get() = exoplayer?.currentPosition
        })

        // Add player event listeners to determine overlay visibility.
        exoplayer?.addListener(object : Player.DefaultEventListener() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                overlayManager?.isVisible = playWhenReady && playbackState == Player.STATE_READY
            }
        })
    }

    private fun releasePlayer() {
        if (exoplayer != null) {
            exoplayer?.stop()
            exoplayer?.release()
            exoplayer = null
        }
    }

    private fun releaseOverlays() {
        if (overlayManager != null) {
            overlayManager?.release()
            overlayManager = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        releaseOverlays()
    }

    override fun onPause() {
        super.onPause()
        exoplayer?.playWhenReady = false
    }
}
