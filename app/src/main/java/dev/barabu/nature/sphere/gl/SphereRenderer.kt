package dev.barabu.nature.sphere.gl

import android.content.Context
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_CULL_FACE
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
import dev.barabu.base.geometry.Vector
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL10.GL_LINE_SMOOTH

class SphereRenderer(private val context: Context) : Renderer {

    // Сфера-каркас
    private lateinit var strokeSphereProgram: SphereProgram

    // Сфера-заливка
    private lateinit var fillSphereProgram: SphereProgram

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glClearColor(1f, 1f, 1f, 1f)

        /**
         * INFO: Маленький трюк.
         *  Радиус Fill-сферы немного меньше радиуса Stroke-сферы. В этом случае Fill-сфера
         *  оказывается полностью внутри каркаса и z-buffer отлично работает. Мы не видим
         *  заднюю часть скелетона, потому что он закрыт Fill-сферой.
         */
        strokeSphereProgram = SphereProgram(context, 1.2f, false)
        fillSphereProgram = SphereProgram(context, 1.19f, true)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        // Сглаженные линии, но для этого нужна поддержка прозрачности (см. далее)
        glEnable(GL_LINE_SMOOTH)

        // Поддержка transparency. В данном случаем для line antialiasing
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_CULL_FACE)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        Matrix.perspectiveM(projectionMatrix, 0, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 8f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, 45f, 1f, 0f, 0f)

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        drawFillSphere()
        drawStrokeSphere()
    }

    private fun drawStrokeSphere() {
        strokeSphereProgram.apply {
            useProgram()
            bindMatrixUniform(modelViewProjectionMatrix)
            bindColorUniform(Vector(1f, 1f, 1f))
            model.draw()
        }
    }

    private fun drawFillSphere() {
        fillSphereProgram.apply {
            useProgram()
            bindMatrixUniform(modelViewProjectionMatrix)
            bindColorUniform(Vector(0f, 0f, 1f))
            model.draw()
        }
    }
}