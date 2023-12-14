package dev.barabu.nature.sphere.planet.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLSurfaceView.Renderer
import android.opengl.Matrix
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.base.TexLoadListener
import dev.barabu.base.TextureLoader
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.nature.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PlanetRenderer(private val context: Context) : Renderer, TexLoadListener {

    private lateinit var program: PlanetProgram

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private val lightPosition = Point(-2.0f, 2.0f, 1.5f)
    private val viewerPosition = Point(-1.0f, 15.0f, 7.0f)

    // Descriptor нативного буфера с битмапой
    private var texBuffDescriptor: Int = INVALID_DESCRIPTOR

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        program = PlanetProgram(context, subdivisions = 3, radius = 1f)

        // Текстура для заливки
        texBuffDescriptor = TextureLoader.loadTexture(context, R.drawable.earth_daymap, this)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        // Model
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, 45f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, 45f, 1f, 0f, 0f)

        // View
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 7f, 0f, 0f, 0f, 0f, 1f, 0f)

        // Projection
        Matrix.perspectiveM(projectionMatrix, 0, 45f, width.toFloat() / height.toFloat(), 1f, 10f)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        drawPlanet()
    }

    override fun onTexPreload(texId: Int) {
        Logging.d("$TAG.onTexPreload")
        adjustTex()
    }

    override fun onTexLoaded(texId: Int) {
        Logging.d("$TAG.onTexLoaded")
        // todo: nothing
    }

    private fun adjustTex() {
        // Wrapping
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        // Filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    private fun drawPlanet() {
        program.apply {
            useProgram()
            bindModelMatrixUniform(modelMatrix)
            bindViewMatrixUniform(viewMatrix)
            bindProjMatrixUniform(projectionMatrix)

            bindLightPositionUniform(lightPosition)
            bindViewerPositionUniform(viewerPosition)

            bindTexUniform(texBuffDescriptor)

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

            draw()
        }
    }

    companion object {
        private const val TAG = "PlanetRenderer"
    }
}