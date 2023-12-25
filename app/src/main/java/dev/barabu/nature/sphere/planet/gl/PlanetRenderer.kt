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
import android.os.SystemClock
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

    private val lightPosition = Point(0.0f, 0.2f, -15.0f)
    private val viewerPosition = Point(0.0f, 0.0f, 5.0f)

    // Descriptor нативного буфера с битмапой
    private var dayTexDescriptor: Int = INVALID_DESCRIPTOR
    private var nightTexDescriptor: Int = INVALID_DESCRIPTOR

    private var startedTime: Long = 0L

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        program = PlanetProgram(context, subdivisions = 3, radius = 1f)

        // Текстура для заливки
        dayTexDescriptor =
            TextureLoader.loadTexture(context, R.drawable.earth_daymap, this).descriptor

        nightTexDescriptor =
            TextureLoader.loadTexture(context, R.drawable.earth_nightmap, this).descriptor

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        startedTime = SystemClock.currentThreadTimeMillis()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }

        // Model
        Matrix.setIdentityM(modelMatrix, 0)
//        Matrix.rotateM(modelMatrix, 0, 30f, 0f, 0f, 1f)
//        Matrix.rotateM(modelMatrix, 0, 30f, 1f, 0f, 0f)

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
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    }

    private fun drawPlanet() {
        program.apply {
            val elapsedSecs = (SystemClock.currentThreadTimeMillis() - startedTime).toFloat() / 100

//            val a: Float = elapsedSecs * 0.8f
//            val dayNightRatio: Float = (a % 6.2831855f)/6.2831855f
//            Logging.d("$TAG $a ratio $dayNightRatio")

            val angle = (elapsedSecs/100) % 360
//            Matrix.rotateM(modelMatrix, 0, angle, 0f, 0.8f, 0.1f)

            useProgram()
            bindModelMatrixUniform(modelMatrix)
            bindViewMatrixUniform(viewMatrix)
            bindProjMatrixUniform(projectionMatrix)

            bindLightPositionUniform(lightPosition)
            bindViewerPositionUniform(viewerPosition)

            bindDayTexUniform(dayTexDescriptor)
            bindNightTexUniform(nightTexDescriptor)

            bindTimeUniform(elapsedSecs)

            bindMaterialUniform(
                ambient = Vector(0.7f, 0.7f, 0.7f),
                diffuse = Vector(0.7f, 0.7f, 0.7f),
                specular = Vector(1.0f, 1.0f, 1.0f),
                shininess = 32f
            )

            bindLightUniform(
                ambient = Vector(0.7f, 0.7f, 0.7f),
                diffuse = Vector(1.0f, 1.0f, 1.0f),
                specular = Vector(1.0f, 1.0f, 1.0f),
            )

            draw()
        }
    }

    companion object {
        private const val TAG = "PlanetRenderer"
    }
}