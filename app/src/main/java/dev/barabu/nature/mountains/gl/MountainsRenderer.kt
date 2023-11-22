package dev.barabu.nature.mountains.gl

import android.content.Context
import android.opengl.GLES30.GL_BLEND
import android.opengl.GLES30.GL_COLOR_BUFFER_BIT
import android.opengl.GLES30.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES30.GL_DEPTH_TEST
import android.opengl.GLES30.GL_LESS
import android.opengl.GLES30.GL_ONE_MINUS_SRC_ALPHA
import android.opengl.GLES30.GL_SRC_ALPHA
import android.opengl.GLES30.glBlendFunc
import android.opengl.GLES30.glClear
import android.opengl.GLES30.glClearColor
import android.opengl.GLES30.glDepthFunc
import android.opengl.GLES30.glEnable
import android.opengl.GLES30.glViewport
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.base.TextureLoader
import dev.barabu.base.geometry.Point
import dev.barabu.nature.R
import dev.barabu.nature.mountains.domain.Skybox
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MountainsRenderer(private val context: Context): Renderer {

    private lateinit var shaderProgram: SkyboxShaderProgram

    private val cubeMMatrix = FloatArray(16)
    private val cubeVMatrix = FloatArray(16)
    private val cubePMatrix = FloatArray(16)
    private val cubeVPMatrix = FloatArray(16)
    private val cubeRotationMatrix = FloatArray(16)
    private val cubeMVPMatrix = FloatArray(16)
    private val invertedCubeVPMatrix = FloatArray(16)

    // Descriptor нативного буфера с битмапой
    private var skyTexDescriptor: Int = INVALID_DESCRIPTOR

    private var cubeCenter = Point(0f, 0f, 0f)

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

        shaderProgram = SkyboxShaderProgram(context)

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

        Matrix.setIdentityM(cubeRotationMatrix, 0)

        // Определяем перспективу
        updateProjectionMatrix(width.toFloat() / height.toFloat())

        // Позиция и направление камеры
        Matrix.setLookAtM(cubeVMatrix, 0, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // кэшируем результат умножения (PMatrix * VMatrix) в VPMatrix
        Matrix.multiplyMM(cubeVPMatrix, 0, cubePMatrix, 0, cubeVMatrix, 0)

        // сразу делаем инвертированную viewProjectionMatrix
        /*Matrix.invertM(invertedCubeVPMatrix, 0, cubeVPMatrix, 0)*/

        positionCubeInWorld(cubeCenter)
        drawCube()
    }

    private fun updateProjectionMatrix(aspectRatio: Float) {
        Logging.d("$TAG.updateProjectionMatrix")
        Matrix.perspectiveM(cubePMatrix, 0, 90f, aspectRatio, 1f, 10f)
    }

    private fun positionCubeInWorld(center: Point) {
        /*val tempM = FloatArray(16)*/

        Matrix.setIdentityM(cubeMMatrix, 0)
        /*Matrix.translateM(cubeMMatrix, 0, center.x, center.y, center.z)*/

        // Apply transformations: rotation first, translation last
        /*Matrix.multiplyMM(
            tempM,
            0,
            cubeMMatrix,
            0,
            cubeRotationMatrix,
            0
        )*/

        Matrix.multiplyMM(
            cubeMVPMatrix, 0, cubeVPMatrix, 0, cubeMMatrix, 0
        )
    }

    private fun drawCube() {
        shaderProgram.apply {
            useProgram()
            bindMatrixUniform(cubeMVPMatrix)
            bindTexUniform(skyTexDescriptor)
            draw()
        }
    }

    companion object {
        private const val TAG = "MountainsRenderer"
    }
}