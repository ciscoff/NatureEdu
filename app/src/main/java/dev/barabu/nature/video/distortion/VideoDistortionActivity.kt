package dev.barabu.nature.video.distortion

import android.media.MediaPlayer
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/**
 * ref: https://www.shadertoy.com/view/ldjGzV
 *
 * Алгоритм:
 * https://stackoverflow.com/a/14999912
 * https://stackoverflow.com/a/48080224
 */
class VideoDistortionActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: VideoSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = VideoSurfaceView(this, MediaPlayer(), VIDEO_URI).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    /**
     * Управляем thread'ом glSurfaceView
     */
    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    companion object {
        private const val VIDEO_URI =
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    }
}