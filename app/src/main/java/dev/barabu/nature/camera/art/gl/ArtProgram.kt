package dev.barabu.nature.camera.art.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1fv
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.camera.art.domain.ArtScreen
import java.nio.FloatBuffer

class ArtProgram(
    context: Context,
    vertexShaderResourceId: Int = R.raw.camera_art_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.camera_art_fragment_shader,
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    override val model: Model = ArtScreen()

    private val aPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_POSITION)

    private val aTexPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_TEX_POSITION)

    private var uVideoTexUnitDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_TEX_UNIT_VIDEO)

    private var uMvpMatrixDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_MVP_MATRIX)

    private var uStMatrixDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_ST_MATRIX)

    private var uFilterNumDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_FILTER_NUM)

    private var uBlurKernelDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_BLUR_KERNEL)

    private var uBlurRadiusDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_BLUR_RADIUS)

    init {
        model.bindAttributes(
            listOf(
                Attribute(aPositionDescriptor, Attribute.Type.Position),
                Attribute(aTexPositionDescriptor, Attribute.Type.Tex)
            )
        )
    }

    override fun draw() {
        model.draw()

        // Disable program after draw finished
        GLES20.glUseProgram(0)
    }

    fun bindOesTexSamplerUniform(textureId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(uVideoTexUnitDescriptor, 1)
    }

    fun bindBlurKernelUniform(buffer: FloatBuffer, count: Int) {
        glUniform1fv(uBlurKernelDescriptor, count, buffer)
    }

    fun bindBlurRadiusUniform(radius: Int) {
        GLES20.glUniform1i(uBlurRadiusDescriptor, radius)
    }

    fun bindMvpMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uMvpMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindStMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uStMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindEffectIntUniform(effectNum: Int) {
        GLES20.glUniform1i(uFilterNumDescriptor, effectNum)
    }

    companion object {
        private const val U_TEX_UNIT_VIDEO = "u_TexUnitVideo"
        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val U_ST_MATRIX = "u_StMatrix"
        private const val U_FILTER_NUM = "u_Filter"
        private const val U_BLUR_KERNEL = "u_BlurKernel"
        private const val U_BLUR_RADIUS = "u_BlurRadius"
        private const val A_POSITION = "a_Position"
        private const val A_TEX_POSITION = "a_TexPos"
    }
}