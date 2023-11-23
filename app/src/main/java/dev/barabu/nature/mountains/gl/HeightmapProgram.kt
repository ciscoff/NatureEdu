package dev.barabu.nature.mountains.gl

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLES20.glUseProgram
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import dev.barabu.base.Logging
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.mountains.domain.Heightmap

class HeightmapProgram(
    context: Context,
    @DrawableRes drawableId: Int,
    vertexShaderResourceId: Int = R.raw.heightmap_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.heightmap_fragment_shader
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    override val model: Model =
        Heightmap((ContextCompat.getDrawable(context, drawableId) as BitmapDrawable).bitmap)

    private var uMatrixDescriptor: Int = glGetUniformLocation(programDescriptor, U_MATRIX)

    private val aPositionDescriptor: Int = glGetAttribLocation(programDescriptor, A_POSITION)

    init {
        model.bindAttributes(listOf(Attribute(aPositionDescriptor, Attribute.Type.Position)))
    }

    /**
     * Загрузить матрицу из массива в нативный uniform нашей программы.
     */
    fun bindMatrixUniform(matrix: FloatArray) {
        Logging.d("$TAG.bindMatrixUniform")
        glUniformMatrix4fv(uMatrixDescriptor, 1, false, matrix, 0)
    }

    override fun draw() {
        model.draw()
        glUseProgram(0)
    }

    companion object {
        private const val TAG = "HeightmapProgram"

        private const val A_POSITION = "a_Position"
        private const val U_MATRIX = "u_Matrix"
    }
}