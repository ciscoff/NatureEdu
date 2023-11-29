package dev.barabu.nature.sphere.gl.prototype

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
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL10.GL_LINE_SMOOTH

class SphereRenderer(private val context: Context) : Renderer {

    // Сфера-каркас
    private lateinit var strokeSphereProgram: SphereProgram

    // Сфера-заливка
    private lateinit var polygonSphereProgram: SphereProgram

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    // NOTE: Именно Point, потому что это координата светильника в пространстве, а не направление
    //  к нему. У меня была ошибка: использовал Vector, да еще v.unit. Вот чтобы случайно не
    //  применить unit используем Point, а не Vector.
    private val lightPosition = Point(2.0f, 4.0f, 2.0f)
    private val lightColor = Vector(1f, 1f, 1f)

    private var resolution = Vector(0f, 0f, 0f)

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 1f)

        /**
         * INFO: Маленький трюк.
         *  Радиус Polygon-сферы немного меньше радиуса Stroke-сферы. В этом случае Polygon-сфера
         *  оказывается полностью внутри каркаса и z-buffer отлично работает. Мы не видим заднюю
         *  часть скелетона, потому что он закрыт Polygon-сферой.
         */
        strokeSphereProgram = SphereProgram(context, 1.01f, false)
        polygonSphereProgram = SphereProgram(context, 1.0f, true)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        // Сглаженные линии, но для этого нужна поддержка прозрачности (см. далее)
        glEnable(GL_LINE_SMOOTH)

        // Поддержка transparency. В данном случаем для line antialiasing
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // NOTE: Вот это дает неожиданный результат. Надо изучить его действие.
        /*glEnable(GL_CULL_FACE)*/
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        // Не используется.
        resolution = Vector(width.toFloat(), height.toFloat(), 0f)

        Matrix.perspectiveM(projectionMatrix, 0, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 6f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, 30f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, 60f, 1f, 0f, 0f)

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        drawPolygonSphere()
        drawStrokeSphere()
    }

    private fun drawStrokeSphere() {
        strokeSphereProgram.apply {
            useProgram()
            bindColorUniform(Vector(0.0f, 0.0f, 0.0f))
            bindMatrixUniform(modelViewProjectionMatrix)
            bindModelMatrixUniform(modelMatrix)
            bindLightPositionUniform(lightPosition)
            bindLightColorUniform(lightColor)
            bindIlluminateUniform(0)
            bindResolutionUniform(resolution.x, resolution.y)
            draw()
        }
    }

    private fun drawPolygonSphere() {
        polygonSphereProgram.apply {
            useProgram()
            bindColorUniform(Vector(0.7f, 0.7f, 0.7f))
            bindMatrixUniform(modelViewProjectionMatrix)
            bindModelMatrixUniform(modelMatrix)
            bindLightPositionUniform(lightPosition)
            bindLightColorUniform(lightColor)
            bindIlluminateUniform(1)
            bindResolutionUniform(resolution.x, resolution.y)
            draw()
        }
    }
}