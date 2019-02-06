# PTVSDK

PTV SDK for Android.

## Example

To run the example project, clone the repo, and compile and build with gradle.

## Requirements

* minSdkVersion 17

## Installation

## Usage

The PTV SDK for Android allows for the creation and teardown of the overlay manager, which controls the visibility of the overlays.

Initialize the PTV SDK in just a few steps:
1) Add the OverlayView to your player layout.
2) Create an overlay data object with your channel id and stream id, or platform information.
3) Initialize the overlay manager.
4) Add an event listener to your player to control overlay visibility.
5) For VOD streams, add the PTVSDK player change listener to set the player's progress position.

### Example

#### Layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="tv.promethean.ptvsdk.demo.MainActivity"
    android:background="@color/background"
    android:id="@+id/activity_player_layout">

    <com.google.android.exoplayer2.ui.SimpleExoPlayerView
        android:id="@+id/simple_exo_player_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <tv.promethean.ptvsdk.ui.OverlayView
        android:id="@+id/ptv_overlay_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</RelativeLayout>
```

#### Initialize

```kotlin
// Create the overlay data object. Note that `streamType` is either LIVE or VOD.
val overlayData = OverlayData(
    channelId = "your-channel-id", 
    streamId = "your-stream-id", 
    streamType = OverlayData.StreamType.LIVE)

// Alternatively, for platform-supported integrations.
val platformData = OverlayData.Platform(id = "001", name = "truetv", type = "channelcode")
val overlayData = OverlayData(platform = platformData, streamType = OverlayData.StreamType.LIVE)

// Instantiate overlay manager inside the player activity.
overlayManager = OverlayManager(this, R.id.ptv_overlay_view, overlayData)

// For VOD playback, add the player listener and set the player position.
overlayManager?.addPlayerListener(object: PlayerChangeListener {
    override val currentPosition: Long?
        get() = exoplayer?.currentPosition // in milliseconds
})

// Add the SimpleExoPlayer listener to determine overlay visibility.
exoplayer?.addListener(object : Player.DefaultEventListener() {
  override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      overlayManager?.isVisible = playWhenReady && playbackState == Player.STATE_READY
  }
})
```

#### Teardown

```kotlin
// Stop and remove all overlays.
overlayManager?.release()
```

## License

PTVSDK is available under the Promethean TV, Inc. license.
