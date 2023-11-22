package dev.barabu.nature.mountains.gl

import android.content.Context
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_LESS
import android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA
import android.opengl.GLES20.GL_SRC_ALPHA
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import android.opengl.Matrix.multiplyMM
import android.opengl.Matrix.rotateM
import android.opengl.Matrix.setIdentityM
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.base.TextureLoader
import dev.barabu.nature.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MountainsRenderer(private val context: Context) : Renderer {

    private lateinit var skyboxShaderProgram: SkyboxShaderProgram

    private val skyMMatrix = FloatArray(16)
    private val skyVMatrix = FloatArray(16)
    private val skyPMatrix = FloatArray(16)
    private val skyVPMatrix = FloatArray(16)
    private val skyMVPMatrix = FloatArray(16)

    // Descriptor нативного буфера с битмапой
    private var skyTexDescriptor: Int = INVALID_DESCRIPTOR

    private var xRotation: Float = 0f
    private var yRotation: Float = 0f

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glClearColor(0.9f, 0.9f, 0.9f, 1f)

        // Текстура Skybox
        skyTexDescriptor = TextureLoader.loadCubeMap(
            context,
            intArrayOf(
                R.drawable.sky_left, R.drawable.sky_right,
                R.drawable.sky_bottom, R.drawable.sky_top,
                R.drawable.sky_front, R.drawable.sky_back
            )
        )

        skyboxShaderProgram = SkyboxShaderProgram(context)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе к камере.
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        // Поддержка transparency в текстурах (PNG например)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        // Определяем перспективу
        updateProjectionMatrix(width.toFloat() / height.toFloat())
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        setupCameraPosition()

        setupSkyPosition()
        drawSky()
    }

    fun handleTouchDrag(dX: Float, dY: Float) {
        Logging.d("$TAG.handleTouchDrag")

        xRotation += dX / 16f
        yRotation += dY / 16f

        if (yRotation < -90f) {
            yRotation = -90f
        } else if (yRotation > 90) {
            yRotation = 90f
        }
    }

    private fun updateProjectionMatrix(aspectRatio: Float) {
        Logging.d("$TAG.updateProjectionMatrix")
        Matrix.perspectiveM(skyPMatrix, 0, 90f, aspectRatio, 1f, 10f)
    }

    /**
     * Позиционируем камеру с учетом накопленного поворота тачем.
     */
    private fun setupCameraPosition() {
        setIdentityM(skyVMatrix, 0)
        rotateM(skyVMatrix, 0, -yRotation, 1f, 0f, 0f)
        rotateM(skyVMatrix, 0, -xRotation, 0f, 1f, 0f)
        multiplyMM(skyVPMatrix, 0, skyPMatrix, 0, skyVMatrix, 0)
    }

    private fun setupSkyPosition() {
        setIdentityM(skyMMatrix, 0)
        multiplyMM(skyMVPMatrix, 0, skyVPMatrix, 0, skyMMatrix, 0)
    }

    /**
     * Привязка униформов выполняется на АКТИВНОЙ программе.
     */
    private fun drawSky() {
        skyboxShaderProgram.apply {
            useProgram()
            bindMatrixUniform(skyMVPMatrix)
            bindTexUniform(skyTexDescriptor)
            draw()
        }
    }

    companion object {
        private const val TAG = "MountainsRenderer"
    }
}