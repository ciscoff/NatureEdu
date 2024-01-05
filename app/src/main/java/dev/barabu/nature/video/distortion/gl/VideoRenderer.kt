package dev.barabu.nature.video.distortion.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import android.view.Surface
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

class VideoRenderer(
    private val glSurfaceView: GLSurfaceView,
    private val context: Context,
    private val videoUri: String
) : Renderer, SurfaceTexture.OnFrameAvailableListener {

    private lateinit var program: VideoProgram

    /** https://source.android.com/docs/core/graphics/arch-st?hl=en */
    private lateinit var surfaceTexture: SurfaceTexture

    private var videoTexDescriptor: Int = INVALID_DESCRIPTOR
    private var isSurfaceUpdated = false
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val mediaPlayer: MediaPlayer = MediaPlayer()

    private val onPreparedListenerInternal = MediaPlayer.OnPreparedListener { player ->
        Logging.d("$TAG.OnPreparedListener w/h=${player.videoWidth}/${player.videoHeight}")
        synchronized(this) {
            videoWidth = player.videoWidth
            videoHeight = player.videoHeight
        }
        mediaPlayer.start()
    }

    private val onVideoSizeChangeListener =
        MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
            Logging.d("$TAG.OnVideoSizeChangedListener w/h=${width}/${height}")
            synchronized(this) {
                videoWidth = width
                videoHeight = height
            }
        }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        Logging.d("$TAG.onSurfaceCreated")
        GLES20.glClearColor(0f, 1.0f, 0f, 1.0f)

        program = VideoProgram(context)
        setupOffScreenTexture()
        setupMediaPlayer()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        Logging.d("$TAG.onSurfaceChanged w/h=${width}/${height}")
        surfaceWidth = width
        surfaceHeight = height
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        //  stMatrix (https://stackoverflow.com/a/30635539)
        //  1  0  0  0
        //  0 -1  0  1
        //  0  0  1  0
        //  0  0  0  1

        synchronized(this) {
            if (isSurfaceUpdated) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(stMatrix)
                isSurfaceUpdated = false

                // NOTE: Здесь нельзя ставить drawVideo(). Будет моргать.
            }
        }

        drawVideo()
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {
        synchronized(this) {
            isSurfaceUpdated = true
            glSurfaceView.requestRender()
        }
    }

    fun play() {
        Logging.d("$TAG.play")
        if (!mediaPlayer.isPlaying && videoWidth != 0 && videoHeight != 0) {
            mediaPlayer.start()
        }
    }

    fun pause() {
        Logging.d("$TAG.pause")
        mediaPlayer.pause()
    }

    fun release() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    private fun drawVideo() {
        Matrix.setIdentityM(mvpMatrix, 0)

        program.apply {
            useProgram()
            adjustVideoPort()
            bindVideoTexUniform(videoTexDescriptor)
            bindStMatrixUniform(stMatrix)
            bindMvpMatrixUniform(mvpMatrix)
            draw()
        }
    }


    /**
     * ref: https://ru.stackoverflow.com/a/1191436
     */
    private fun adjustVideoPort() {
        val widthRatio = surfaceWidth.toFloat() / videoWidth
        val heightRatio = surfaceHeight.toFloat() / videoHeight
        val ratio = min(widthRatio, heightRatio)

        val targetVideoWidth = (ratio * videoWidth).toInt()
        val targetVideoHeight = (ratio * videoHeight).toInt()

        glViewport(
            (surfaceWidth - targetVideoWidth),
            (surfaceHeight - targetVideoHeight) / 2,
            targetVideoWidth,
            targetVideoHeight
        )
    }

    /**
     * Текстура, в которую будет рисовать MediaPlayer
     */
    private fun setupOffScreenTexture() {
        Logging.d("$TAG.setupOffScreenTexture")
        val descriptor = IntArray(1)
        glGenTextures(1, descriptor, 0)

        if (descriptor[0] == ERROR_CODE) throw RuntimeException("glGenTextures failed")

        videoTexDescriptor = descriptor[0]
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, videoTexDescriptor)
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        surfaceTexture = SurfaceTexture(videoTexDescriptor)
        surfaceTexture.setDefaultBufferSize(glSurfaceView.width, glSurfaceView.height)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    private fun setupMediaPlayer() {
        Logging.d("$TAG.setupMediaPlayer")

        val surface = Surface(surfaceTexture)

        synchronized(this) { isSurfaceUpdated = false }
        mediaPlayer.apply {
            setOnPreparedListener(onPreparedListenerInternal)
            setOnVideoSizeChangedListener(onVideoSizeChangeListener)
            setScreenOnWhilePlaying(true)
            setSurface(surface)
            if (videoUri.startsWith("android")) {
                setDataSource(context, Uri.parse(videoUri))
                prepare()
            } else {
                setDataSource(videoUri)
                prepareAsync()
            }
        }

        surface.release()
    }

    companion object {
        private const val TAG = "VideoRenderer"
    }
}