package dev.barabu.nature.video.distortion.gl

import android.content.Context
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE1
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glUniform1i
import dev.barabu.base.TextResourceReader
import dev.barabu.base.checkGlError
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.video.distortion.domain.Screen

class VideoProgram(
    context: Context,
    vertexShaderResourceId: Int = R.raw.video_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.video_fragment_shader,
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    override val model: Model = Screen()

    private val aPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_POSITION)

    private val aTexPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_TEX_POSITION)

    private var uVideoTexUnitDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_TEX_UNIT_VIDEO)

    private var uMvpMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_MVP_MATRIX)

    private var uStMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_ST_MATRIX)

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

    fun bindVideoTexUniform(textureId: Int) {
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)
        glUniform1i(uVideoTexUnitDescriptor, 1)
    }

    fun bindMvpMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uMvpMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindStMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uStMatrixDescriptor, 1, false, matrix, 0)
    }

    companion object {
        private const val U_TEX_UNIT_VIDEO = "u_TexUnitVideo"
        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val U_ST_MATRIX = "u_StMatrix"
        private const val A_POSITION = "a_Position"
        private const val A_TEX_POSITION = "a_TexPos"
    }
}