package dev.barabu.nature.camera.preview.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.camera.preview.domain.PreviewScreen

class PreviewProgram(
    context: Context,
    vertexShaderResourceId: Int = R.raw.camera_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.camera_fragment_shader,
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    override val model: Model = PreviewScreen()

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
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(uVideoTexUnitDescriptor, 1)
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