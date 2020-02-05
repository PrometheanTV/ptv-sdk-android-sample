package tv.promethean.sdk.sample

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import tv.promethean.ptvsdk.OverlayManager
import tv.promethean.ptvsdk.interfaces.DefaultOverlayEventListener
import tv.promethean.ptvsdk.interfaces.PlayerChangeListener
import tv.promethean.ptvsdk.models.ConfigData
import tv.promethean.ptvsdk.models.OverlayData


private const val SAMPLE_CHANNEL_ID = "5c701be7dc3d20080e4092f4"
private const val SAMPLE_STREAM_ID = "5de7e7c2a6adde5211684519"

class MainActivity : AppCompatActivity() {

    private var exoplayerView: PlayerView? = null
    private var exoplayer: SimpleExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var concatenatingMediaSource: ConcatenatingMediaSource? = null
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
        val componentName = ComponentName(baseContext, "Exo")

        exoplayer = ExoPlayerFactory.newSimpleInstance(baseContext, trackSelector)
        exoplayerView?.player = exoplayer
        exoplayerView?.useController = true

        playbackStateBuilder = PlaybackStateCompat.Builder()
        playbackStateBuilder?.setActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_FAST_FORWARD)

        mediaSession = MediaSessionCompat(baseContext, "ExoPlayer", componentName, null)
        mediaSession?.setPlaybackState(playbackStateBuilder?.build())
        mediaSession?.isActive = true

        concatenatingMediaSource = ConcatenatingMediaSource()
    }

    private fun initializeOverlays() {
        val overlayData = OverlayData(
            channelId = SAMPLE_CHANNEL_ID,
            streamId = SAMPLE_STREAM_ID)

        // Instantiate overlay manager.
        overlayManager = OverlayManager(this, R.id.ptv_overlay_view, overlayData)

        overlayManager?.addOverlayListener(object : DefaultOverlayEventListener {
            override fun onVisibilityChange(hasVisibleOverlays: Boolean, numVisibleOverlays: Int) {
                // Letting you know there are overlays on the screen, and how many.
            }

            override fun onConfigReady(config: ConfigData) {
                // Run on UI thread so player controls function correctly.
                this@MainActivity.runOnUiThread {
                    // Create a collection of media sources from the array of config
                    // sources returned from the API.
                    for (source in config.streamSources) {
                        val mediaSource = buildMediaSource(source.url)
                        concatenatingMediaSource?.addMediaSource(mediaSource)
                    }
                    // Immediately load media sources and play.
                    exoplayer?.playWhenReady = true
                    exoplayer?.prepare(concatenatingMediaSource)
                }
            }
        })

        // Set the player position for VOD playback.
        overlayManager?.addPlayerListener(object : PlayerChangeListener {
            override val currentPosition: Long?
                get() = exoplayer?.currentPosition
        })

        // Add player event listeners to determine overlay visibility.
        exoplayer?.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                overlayManager?.isVisible = playWhenReady && playbackState == Player.STATE_READY
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                if (error?.type == ExoPlaybackException.TYPE_SOURCE) {
                    // Example fallback strategy for cycling through media sources if different
                    // ones are specified in the Broadcast Center.
                    playNextMediaSource()
                }
            }
        })
    }

    private fun buildMediaSource(url: String) : MediaSource {
        val userAgent = Util.getUserAgent(baseContext, "Exo")
        val dataSourceFactory = DefaultDataSourceFactory(baseContext, userAgent)
        val uri = Uri.parse(url)

        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource
                .Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_OTHER -> ExtractorMediaSource
                .Factory(dataSourceFactory)
                .setExtractorsFactory(DefaultExtractorsFactory())
                .createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type :: $type")
        }

    }

    private fun playNextMediaSource() {
        // Play next media source by removing current from collection.
        exoplayer?.currentWindowIndex?.let {
            concatenatingMediaSource?.removeMediaSource(it)
        }
        concatenatingMediaSource?.let {
            exoplayer?.playWhenReady = true
            exoplayer?.prepare(concatenatingMediaSource, true, true)
        }
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
