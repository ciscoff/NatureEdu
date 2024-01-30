package dev.barabu.nature.camera.art.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_COLOR_ATTACHMENT0
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_RGBA
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glDeleteFramebuffers
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glFramebufferTexture2D
import android.opengl.GLES20.glGenFramebuffers
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glTexImage2D
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLES20.glUniform1fv
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLES30.GL_RGBA16F
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.camera.art.domain.ArtScreen
import dev.barabu.nature.camera.art.domain.BlurKernel
import java.nio.FloatBuffer

/**
 * INFO: Если поставить radius=20, то заметно тормозит. В общем нужен баланс между
 *  радиусом и количеством итераций.
 */
class BlurProgram(
    context: Context,
    val radius: Int = 8,
    vertexShaderResourceId: Int = R.raw.camera_art_blur_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.camera_art_blur_portrait_fragment_shader,
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {
    override val model: Model = ArtScreen()

    private val blurKernel = BlurKernel(radius)

    private var viewPortWidth = 0
    private var viewPortHeight = 0

    private val frameBuffers = intArrayOf(INVALID_DESCRIPTOR, INVALID_DESCRIPTOR)
    private val texBuffers = intArrayOf(INVALID_DESCRIPTOR, INVALID_DESCRIPTOR)

    private val aPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_POSITION)

    private val aTexPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_TEX_POSITION)

    private var uOesTexSamplerDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_OES_TEX_SAMPLER)

    private var uFboTexSamplerDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_FBO_TEX_SAMPLER)

    private var uMvpMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_MVP_MATRIX)

    private var uStMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_ST_MATRIX)

    private var uHorizontalDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_HORIZONTAL)

    private var uBlurKernelDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_BLUR_KERNEL)

    private var uBlurRadiusDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_BLUR_RADIUS)

    private var uFirstIterationDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_FIRST_ITERATION)

    init {
        model.bindAttributes(
            listOf(
                Attribute(aPositionDescriptor, Attribute.Type.Position),
                Attribute(aTexPositionDescriptor, Attribute.Type.Tex)
            )
        )
    }

    private fun clearFbo() {
        glDeleteTextures(2, texBuffers, 0)
        glDeleteFramebuffers(2, frameBuffers, 0)
    }


    /**
     * glTexImage2D: [https://stackoverflow.com/a/34497547]
     * - format (7) / type (8) описывают данные, которые мы передаем в текстуру. Это влияет
     *   на memory layout буфера.
     * - internalFormat (3) - определяет внутренний формат в котором OpenGL должен хранить
     *   данные
     */
    fun setupFbo(width: Int, height: Int) {
        Logging.d("$TAG.setupFbo")

        viewPortWidth = width
        viewPortHeight = height

        clearFbo()

        glGenFramebuffers(2, frameBuffers, 0);
        glGenTextures(2, texBuffers, 0);

        for (i in 0 until BUFFERS) {
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers[i])
            glBindTexture(GL_TEXTURE_2D, texBuffers[i])
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, null)

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            glFramebufferTexture2D(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texBuffers[i], 0
            )
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    /**
     * Первая итерация:
     *  - Берём цвет из самплера GL_TEXTURE_EXTERNAL_OES.
     *  - Рисуем в текстуру FB0[0].
     *  - Горизонтальный blur.
     * Вторая итерация:
     *  - Берём цвет из самплера GL_TEXTURE_2D буфера FB0[0].
     *  - Рисуем в текстуру FB0[1].
     *  - Вертикальный blur.
     * Третья итерация:
     *  - Берём цвет из самплера GL_TEXTURE_2D  буфера FB0[1].
     *  - Рисуем в текстуру FB0[0].
     *  - Горизонтальный blur
     * Четвертая итерация:
     *   . . . .
     * Последняя итерация:
     *  - Устанавливаем FBO экрана и рисуем туда
     *
     * INFO: Матрицы для трансформации текстуры и вертексов используем только на последней
     *  итерации, чтобы правильно вывести картинку на экран. До последней итерации гоняем
     *  между буферами raw-картинку, которую получили на первой итерации из OES_TEX.
     */
    override fun draw() {
        var isFirstIteration = true
        var isHorizontal = true

        GLES20.glViewport(0, 0, viewPortWidth, viewPortHeight)

        bindBlurKernelUniform(blurKernel.gaussian1D(), radius + 1)
        bindBlurRadiusUniform(radius)

        for (i in 0 until ITERATIONS) {
            bindFirstIterationUniform(isFirstIteration)
            bindHorizontalUniform(isHorizontal)

            // Куда рисуем (в какой FBO, в какую Tex)
            // На последней итерации рисуем в FBO экрана.
            // И только на последней итерации применяем матрицы к вертексам и текстуре
            if (i == (ITERATIONS - 1)) {
                glBindFramebuffer(GL_FRAMEBUFFER, 0)
            } else {
                glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers[if (isHorizontal) 0 else 1])
            }

            // Откуда брать цвет для текущего рисования.
            // На первой итерации берем цвет из OES Tex.
            if (!isFirstIteration) {
                bindFboTexSamplerUniform(texBuffers[(i - 1) % 2])
            }

            isHorizontal = !isHorizontal
            isFirstIteration = false
            model.draw()
        }
    }

    fun bindOesTexSamplerUniform(textureId: Int) {
        glActiveTexture(GLES20.GL_TEXTURE1)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        glUniform1i(uOesTexSamplerDescriptor, 1)
    }

    fun bindMvpMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uMvpMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindStMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uStMatrixDescriptor, 1, false, matrix, 0)
    }

    private fun bindFboTexSamplerUniform(textureId: Int) {
        glActiveTexture(GLES20.GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glUniform1i(uFboTexSamplerDescriptor, 0)
    }

    private fun bindHorizontalUniform(value: Boolean) {
        glUniform1i(uHorizontalDescriptor, if (value) 1 else 0)
    }

    private fun bindFirstIterationUniform(value: Boolean) {
        glUniform1i(uFirstIterationDescriptor, if (value) 1 else 0)
    }

    private fun bindBlurKernelUniform(buffer: FloatBuffer, count: Int) {
        glUniform1fv(uBlurKernelDescriptor, count, buffer)
    }

    private fun bindBlurRadiusUniform(radius: Int) {
        glUniform1i(uBlurRadiusDescriptor, radius)
    }

    companion object {

        private const val TAG = "BlurProgram"

        private const val BUFFERS = 2
        private const val ITERATIONS = 10

        private const val U_OES_TEX_SAMPLER = "u_OesTexSampler"
        private const val U_FBO_TEX_SAMPLER = "u_FboTexSampler"
        private const val U_FIRST_ITERATION = "u_FirstIteration"
        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val U_ST_MATRIX = "u_StMatrix"
        private const val U_HORIZONTAL = "u_Horizontal"
        private const val U_BLUR_KERNEL = "u_BlurKernel"
        private const val U_BLUR_RADIUS = "u_BlurRadius"
        private const val A_POSITION = "a_Position"
        private const val A_TEX_POSITION = "a_TexPos"
    }
}