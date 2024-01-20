package dev.barabu.nature.camera.art.gl

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.Surface
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.nature.R
import dev.barabu.nature.camera.art.domain.CameraWrapper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ArtRenderer(
    private val glSurfaceView: GLSurfaceView,
    private val cameras: Flow<CameraWrapper>
) : GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {

    private lateinit var program: ArtProgram

    private lateinit var surfaceTexture: SurfaceTexture

    private var previewTexDescriptor: Int = INVALID_DESCRIPTOR
    private val context = glSurfaceView.context
    private var isSurfaceUpdated = false

    private val stMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val displayRotation: Int
        get() = DISPLAY_ROTATIONS[(context as Activity).windowManager.defaultDisplay.rotation]!!

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 1.0f, 0f, 1.0f)
        setupOffScreenGlTexture()
        program = ArtProgram(context)

        CoroutineScope(Dispatchers.Unconfined + CoroutineExceptionHandler { _, e ->
            Logging.e(e)
        }).launch {
            cameras.collect { wrapper ->
                wrapper.camera?.initializeCamera(
                    context.getSystemService(Context.CAMERA_SERVICE) as CameraManager,
                    surfaceTexture
                )
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        updateModelMatrix(width.toFloat(), height.toFloat())
        updateSurfaceBufferSize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        synchronized(this) {
            if (isSurfaceUpdated) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(stMatrix)
                isSurfaceUpdated = false
            }
        }

        drawPreview()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            isSurfaceUpdated = true
            glSurfaceView.requestRender()
        }
    }

    fun stopCapture(wrapper: CameraWrapper) {
        wrapper.camera?.close()
    }

    private fun drawPreview() {
        program.apply {
            useProgram()
            bindVideoTexUniform(previewTexDescriptor)
            bindStMatrixUniform(stMatrix)
            bindMvpMatrixUniform(modelMatrix)
            draw()
        }
    }

    private fun setupOffScreenGlTexture() {
        val descriptor = IntArray(1)
        GLES20.glGenTextures(1, descriptor, 0)

        if (descriptor[0] == ERROR_CODE) throw RuntimeException("glGenTextures failed")

        previewTexDescriptor = descriptor[0]
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, previewTexDescriptor)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(previewTexDescriptor)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    private fun updateSurfaceBufferSize(width: Int, height: Int) {

        if (context.resources.getBoolean(R.bool.isTab)) {

            // NOTE: Это работает на телефоне (кроме Samsung A12) и планшете (не фига не понял почему)
            when (context.resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> {
                    surfaceTexture.setDefaultBufferSize(width, width)
                }

                Configuration.ORIENTATION_LANDSCAPE -> {
                    surfaceTexture.setDefaultBufferSize(height, height)
                }

                else -> {
                    surfaceTexture.setDefaultBufferSize(width, height)
                }
            }
        } else {
            // NOTE: Это работает на телефоне, НО не работает на планшете
            when (context.resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> {
                    surfaceTexture.setDefaultBufferSize(width, height)
                }

                Configuration.ORIENTATION_LANDSCAPE -> {
                    surfaceTexture.setDefaultBufferSize(height, width)
                }

                else -> {
                    surfaceTexture.setDefaultBufferSize(width, height)
                }
            }
        }
    }

    private fun updateModelMatrix(width: Float, height: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        scaleModel(width, height)
        rotateModel()
    }

    private fun scaleModel(width: Float, height: Float) {
        if (height > width) {
            // В вертикальном положении за 1 берем высоту (Y) и скалируем вертексы по ширине (X)
            val ratio = height / width
            Matrix.scaleM(modelMatrix, 0, ratio, 1f, 1f)
        } else {
            // В горизонтальном положении за 1 берем ширину (X) и скалируем вертексы по высоте (Y)
            val ratio = width / height
            Matrix.scaleM(modelMatrix, 0, 1f, ratio, 1f)
        }
    }

    private fun rotateModel() {
        Matrix.rotateM(modelMatrix, 0, displayRotation.toFloat(), 0f, 0f, 1f)
    }

    companion object {
        private const val TAG = "ArtRenderer"

        private val DISPLAY_ROTATIONS = mapOf(
            Surface.ROTATION_0 to 0,
            Surface.ROTATION_90 to 90,
            Surface.ROTATION_180 to 180,
            Surface.ROTATION_270 to 270
        )
    }
}