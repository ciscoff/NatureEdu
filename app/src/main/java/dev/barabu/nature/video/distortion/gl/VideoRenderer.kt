package dev.barabu.nature.video.distortion.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLES20.glViewport
import android.opengl.GLES32.GL_CLAMP_TO_BORDER
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.view.Surface
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.nature.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRenderer(
    private val context: Context,
    private val mediaPlayer: MediaPlayer,
    private val videoUri: String = "android.resource://${context.packageName}/${R.raw.cat}"
) : Renderer, SurfaceTexture.OnFrameAvailableListener {

    private lateinit var program: VideoProgram
    private lateinit var surfaceTexture: SurfaceTexture

    private var videoTexDescriptor: Int = INVALID_DESCRIPTOR
    private var isSurfaceUpdated = false
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val onPreparedListenerInternal = MediaPlayer.OnPreparedListener { player ->
        Logging.d("$TAG.OnPreparedListener w/h=${player.videoWidth}/${player.videoHeight}")

        videoWidth = player.videoWidth
        videoHeight = player.videoHeight
        mediaPlayer.start()
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        Logging.d("$TAG.onSurfaceCreated")
        program = VideoProgram(context)
        setupOffScreenTexture()
        setupMediaPlayer()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        Logging.d("$TAG.onSurfaceChanged w/h=${width}/${height}")
        glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        synchronized(this) {
            if (isSurfaceUpdated) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(stMatrix)
                isSurfaceUpdated = false

                if (videoWidth != 0 && videoHeight != 0) {
                    glViewport(0, 0, videoWidth, videoHeight)
                }
                // NOTE: Здесь нельзя ставить drawVideo(). Будет моргать.
            }
        }

        drawVideo()
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {
        synchronized(this) {
            isSurfaceUpdated = true
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

    private fun drawVideo() {
        Matrix.setIdentityM(mvpMatrix, 0)

        program.apply {
            useProgram()
            bindVideoTexUniform(videoTexDescriptor)
            bindStMatrixUniform(stMatrix)
            bindMvpMatrixUniform(mvpMatrix)
            draw()
        }
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
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        surfaceTexture = SurfaceTexture(videoTexDescriptor)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    private fun setupMediaPlayer() {
        Logging.d("$TAG.setupMediaPlayer")

        Surface(surfaceTexture).also { mediaPlayer.setSurface(it) }//.release()
        synchronized(this) { isSurfaceUpdated = false }
        mediaPlayer.apply {
            setOnPreparedListener(onPreparedListenerInternal)
            setDataSource(videoUri)
            prepareAsync()
        }
    }

    companion object {
        private const val TAG = "VideoRenderer"
    }
}