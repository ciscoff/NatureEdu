package dev.barabu.nature.sphere.ico.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.nature.sphere.Mode
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class IcoSphereRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var programFlat: IcoSphereProgram
    private lateinit var programSmooth: IcoSphereProgram

    private val modelMatrix = FloatArray(16)
    private val modelMatrix2 = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix2 = FloatArray(16)

    private val lightPosition = Point(-2.0f, 2.0f, 1.5f)

    private val viewerPosition = Point(-1.0f, 15.0f, 7.0f)

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        programFlat = IcoSphereProgram(context, radius = 1.0f, subdivisions = 3, isFlat = true)
        programSmooth = IcoSphereProgram(context, radius = 1.0f, subdivisions = 3, isFlat = false)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        Matrix.perspectiveM(projectionMatrix, 0, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 7f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 1.1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 45f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, 45f, 1f, 0f, 0f)

        Matrix.setIdentityM(modelMatrix2, 0)
        Matrix.translateM(modelMatrix2, 0, 0f, -1.1f, 0f)
        Matrix.rotateM(modelMatrix2, 0, 45f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix2, 0, 45f, 1f, 0f, 0f)

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix2, 0, viewProjectionMatrix, 0, modelMatrix2, 0)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        drawSphere()
    }

    private fun drawSphere() {
        programFlat.apply {
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
            GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
            GLES20.glPolygonOffset(1.0f, 1.0f)
            draw(Mode.Polygon, false)
            GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)

            bindDrawPolygonUniform(false)
            draw(Mode.Line, true)
        }

        programSmooth.apply {
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
            GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)
            GLES20.glPolygonOffset(1.0f, 1.0f)
            draw(Mode.Polygon, false)
            GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)

            bindDrawPolygonUniform(false)
            draw(Mode.Line, true)
        }
    }
}