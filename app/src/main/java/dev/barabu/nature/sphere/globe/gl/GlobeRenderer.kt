package dev.barabu.nature.sphere.globe.gl

import android.content.Context
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_LEQUAL
import android.opengl.GLES20.GL_POLYGON_OFFSET_FILL
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glPolygonOffset
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.nature.sphere.globe.domain.GlobeSphere
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlobeRenderer(private val context: Context) : Renderer {

    private lateinit var globeProgram: GlobeProgram
    private lateinit var globeProgram2: GlobeProgram

    private val modelMatrix = FloatArray(16)
    private val modelMatrix2 = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix2 = FloatArray(16)

    // NOTE: Именно Point, потому что это координата светильника в пространстве, а не направление
    //  к нему. У меня была ошибка: использовал Vector, да еще v.unit. Вот чтобы случайно не
    //  применить unit используем Point, а не Vector.
    private val lightPosition = Point(-2.0f, 2.0f, 1.5f)

    private val viewerPosition = Point(-1.0f, 15.0f, 7.0f)

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 1f)

        globeProgram = GlobeProgram(context, radius = 1.0f, isFlat = true)
        globeProgram2 = GlobeProgram(context, radius = 1.0f, isFlat = false)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)

        // Сглаженные линии, но для этого нужна поддержка прозрачности (см. далее)
//        glEnable(GL_LINE_SMOOTH)

        // Поддержка transparency. В данном случаем для line antialiasing
//        glEnable(GL_BLEND)
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // NOTE: Вот это дает неожиданный результат. Надо изучить его действие.
        /*glEnable(GL_CULL_FACE)*/
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        Matrix.perspectiveM(projectionMatrix, 0, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 7f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 1.1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 30f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, 60f, 1f, 0f, 0f)

        Matrix.setIdentityM(modelMatrix2, 0)
        Matrix.translateM(modelMatrix2, 0, 0f, -1.1f, 0f)
        Matrix.rotateM(modelMatrix2, 0, 30f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix2, 0, 60f, 1f, 0f, 0f)

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix2, 0, viewProjectionMatrix, 0, modelMatrix2, 0)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        drawSphere()
    }

    private fun drawSphere() {
        globeProgram.apply {
            useProgram()
            bindMvpMatrixUniform(modelViewProjectionMatrix)
            bindModelMatrixUniform(modelMatrix)
            bindLightPositionUniform(lightPosition)
            bindViewerPositionUniform(viewerPosition)
            bindColorUniform(Vector(0f, 0f, 0f))

            bindMaterialUniform(
                ambient = Vector(0.7f, 0.7f, 0.7f),
                diffuse = Vector(0.7f, 0.7f, 0.7f),
                specular = Vector(1.0f, 1.0f, 1.0f),
                shininess = 32f
            )

            bindLightUniform(
                ambient = Vector(0.7f, 0.7f, 0.7f),
                diffuse = Vector(0.7f, 0.7f, 0.7f),
                specular = Vector(1.0f, 1.0f, 1.0f),
            )

            bindDrawPolygonUniform(true)
            glEnable(GL_POLYGON_OFFSET_FILL)
            glPolygonOffset(1.0f, 1.0f)
            draw(GlobeSphere.Mode.Polygon, false)
            glDisable(GL_POLYGON_OFFSET_FILL)

            bindDrawPolygonUniform(false)
            draw(GlobeSphere.Mode.Line, true)
        }

        globeProgram2.apply {
            useProgram()
            bindMvpMatrixUniform(modelViewProjectionMatrix2)
            bindModelMatrixUniform(modelMatrix2)
            bindLightPositionUniform(lightPosition)
            bindViewerPositionUniform(viewerPosition)
            bindColorUniform(Vector(0f, 0f, 0f))

            bindMaterialUniform(
                ambient = Vector(0.7f, 0.7f, 0.7f),
                diffuse = Vector(0.7f, 0.7f, 0.7f),
                specular = Vector(1.0f, 1.0f, 1.0f),
                shininess = 32f
            )

            bindLightUniform(
                ambient = Vector(0.7f, 0.7f, 0.7f),
                diffuse = Vector(0.7f, 0.7f, 0.7f),
                specular = Vector(1.0f, 1.0f, 1.0f),
            )

            bindDrawPolygonUniform(true)
            glEnable(GL_POLYGON_OFFSET_FILL)
            glPolygonOffset(1.0f, 1.0f)
            draw(GlobeSphere.Mode.Polygon, false)
            glDisable(GL_POLYGON_OFFSET_FILL)

            bindDrawPolygonUniform(false)
            draw(GlobeSphere.Mode.Line, true)
        }
    }
}