package dev.barabu.nature.video.distortion

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.nature.R

/**
 * ref: https://www.shadertoy.com/view/ldjGzV
 *
 * Алгоритм:
 * https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/VideoSurfaceView.java
 * https://github.com/CharonChui/AndroidNote/blob/master/VideoDevelopment/OpenGL/10.GLSurfaceView%2BMediaPlayer%E6%92%AD%E6%94%BE%E8%A7%86%E9%A2%91.md
 * https://github.com/satish13131/Android_Programs/blob/master/graphics/opengl/video/surfaceview/src/com/example/custom/view/CustomVideoView.java
 * https://stackoverflow.com/a/14999912
 * https://stackoverflow.com/a/48080224
 */
class VideoDistortionActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: VideoSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localVideoUri = "android.resource://${packageName}/${R.raw.cat}"

        val uri = localVideoUri

        glSurfaceView = VideoSurfaceView(this, uri).apply {
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

        private const val VIDEO_URI_1 =
            "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
    }
}