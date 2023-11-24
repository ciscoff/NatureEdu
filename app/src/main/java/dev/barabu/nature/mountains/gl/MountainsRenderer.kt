package dev.barabu.nature.mountains.gl

import android.content.Context
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_CULL_FACE
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_LEQUAL
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
import android.opengl.Matrix.scaleM
import android.opengl.Matrix.setIdentityM
import android.opengl.Matrix.translateM
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.base.TextureLoader
import dev.barabu.base.geometry.Vector
import dev.barabu.nature.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Отрисовка неба (Skybox) и ландшафта (Heightmap).
 *
 * Skybox - это куб со сторонами 2x2 с центром в начале координат. Heightmap - это квадратная со
 * сторонами 1x1 рельефная поверхность, основание которой лежит на плоскости XZ, а "горы" растут
 * по оси +Y. И Skybox и Heightmap позиционируются матрицей [modelMatrix] которая всегда identity,
 * то есть эти объекты всегда по центру. Для отрисовки используем две камеры. Камера Heightmap
 * позиционируются матрицей [viewMatrix], камера Skybox позиционируются матрицей [viewMatrixForSky].
 *
 * Skybox - это небесная оболочка вокруг наблюдателя. Оно окружает все остальные предметы мира, а
 * значит должна находиться в самой дальней видимой позиции, на far плоскости нашего frustum.
 * Для того чтобы всегда размещать на far плоскости мы делаем трюк с z-координатой в коде vertex
 * шейдера 'gl_Position = gl_Position.xyww;' Мы принудительно записываем в z значение w. При
 * последующем perspective divide мы получим z = 1, то есть положение вертекса на плоскости far.
 */
class MountainsRenderer(private val context: Context) : Renderer {

    private lateinit var skyboxProgram: SkyboxProgram
    private lateinit var heightmapProgram: HeightmapProgram

    private val tempMatrix = FloatArray(16)

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val viewMatrixForSky = FloatArray(16)

    // Descriptor нативного буфера с битмапой
    private var skyTexDescriptor: Int = INVALID_DESCRIPTOR

    private var xRotation: Float = 0f
    private var yRotation: Float = 0f

    // Направление от НА источник света
    private val vectorToLight = Vector(0.6f, 1f, -0.6f).unit

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glClearColor(1f, 1f, 1f, 1f)

        // Текстура Skybox
        skyTexDescriptor = TextureLoader.loadCubeMap(
            context,
            intArrayOf(
                R.drawable.sky_left, R.drawable.sky_right,
                R.drawable.sky_bottom, R.drawable.sky_top,
                R.drawable.sky_front, R.drawable.sky_back
            )
        )

        skyboxProgram = SkyboxProgram(context)
        heightmapProgram = HeightmapProgram(context, R.drawable.heighmap)

        // Включаем Z-buffer, чтобы рисовать только те вертексы, которые ближе.
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)

        // Поддержка transparency в текстурах (PNG например)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_CULL_FACE)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        if (width == 0 || height == 0) {
            return
        }
        // Определяем перспективу
        updateProjectionMatrix(width.toFloat() / height.toFloat())
        // Обновляем камеры
        updateViewMatrices()
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        drawHeightmap()
        drawSky()
    }

    fun handleTouchDrag(dX: Float, dY: Float) {
        Logging.d("$TAG.handleTouchDrag")

        xRotation += dX / 16f
        yRotation += dY / 16f

        if (yRotation < -20f) {
            yRotation = -20f
        } else if (yRotation > 90) {
            yRotation = 90f
        }
        updateViewMatrices()
    }

    private fun drawHeightmap() {
        setIdentityM(modelMatrix, 0)
        scaleM(modelMatrix, 0, 20f, 2f, 20f)
        updateMvpMatrix()
        heightmapProgram.apply {
            useProgram()
            bindMatrixUniform(modelViewProjectionMatrix)
            bindLightVectorUniform(vectorToLight)
            draw()
        }
    }

    /**
     * Привязка униформов выполняется на АКТИВНОЙ программе.
     */
    private fun drawSky() {
        setIdentityM(modelMatrix, 0)
        updateSkyMvpMatrix()
        glDepthFunc(GL_LEQUAL)
        skyboxProgram.apply {
            useProgram()
            bindMatrixUniform(modelViewProjectionMatrix)
            bindTexUniform(skyTexDescriptor)
            draw()
        }
    }

    /**
     * Используем две камеры - одна для Sky, вторая для Heightmap.
     * Позиционируем их с учетом накопленного поворота касанием.
     * - Для Sky учитываем только вращение.
     * - Для Heightmap ДОБАВЛЯЕМ смещение.
     */
    private fun updateViewMatrices() {
        setIdentityM(viewMatrix, 0)

        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f)
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f)

        // Для Sky сохраняем только вращение
        System.arraycopy(viewMatrix, 0, viewMatrixForSky, 0, viewMatrix.size)

        // Для Heightmap еще добавляем translate
        translateM(viewMatrix, 0, 0f, -2.5f, -5f)
    }

    private fun updateProjectionMatrix(aspectRatio: Float) {
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspectRatio, 1f, 100f)
    }

    private fun updateMvpMatrix() {
        multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
    }

    private fun updateSkyMvpMatrix() {
        multiplyMM(tempMatrix, 0, viewMatrixForSky, 0, modelMatrix, 0)
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
    }

    companion object {
        private const val TAG = "MountainsRenderer"
    }
}