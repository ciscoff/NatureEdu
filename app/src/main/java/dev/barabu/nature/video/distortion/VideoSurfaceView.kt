package dev.barabu.nature.video.distortion

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.video.distortion.gl.VideoRenderer

@SuppressLint("ViewConstructor")
class VideoSurfaceView(
    context: Context,
    videoUri: String,
) : GLSurfaceView(context) {

    private var videoRenderer: VideoRenderer

    private var isRendererSet = false

    init {

        videoRenderer = VideoRenderer(context, videoUri)

        if (context.isActualGlEsSupporting) {
            setEGLContextClientVersion(3)

            holder.setFormat(PixelFormat.TRANSLUCENT)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)

            setRenderer(videoRenderer)
            isRendererSet = true
        } else {
            Logging.d("This device does not support OpenGL ES 3.0.")
        }
    }

    override fun onResume() {
        if (isRendererSet) {
            queueEvent {
                videoRenderer.play()
            }
        }
        super.onResume()
    }

    override fun onPause() {
        if (isRendererSet) {
            queueEvent {
                videoRenderer.pause()
            }
        }
        super.onPause()
    }

    override fun onDetachedFromWindow() {
        if (isRendererSet) {
            queueEvent {
                videoRenderer.release()
            }
        }
        super.onDetachedFromWindow()

    }
}