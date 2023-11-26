package dev.barabu.nature.sphere.gl

import android.content.Context
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLES20.glUseProgram
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.sphere.domain.Sphere

class SphereProgram(
    context: Context,
    vertexShaderResourceId: Int = R.raw.sphere_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.sphere_fragment_shader
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {
    override val model: Model = Sphere(radius = 1f)

    private var uMatrixDescriptor: Int = glGetUniformLocation(programDescriptor, U_MATRIX)

    private val aPositionDescriptor: Int = glGetAttribLocation(programDescriptor, A_POSITION)

    init {
        model.bindAttributes(listOf(Attribute(aPositionDescriptor, Attribute.Type.Position)))
    }

    override fun draw() {
        model.draw()
        glUseProgram(0)
    }

    fun bindMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uMatrixDescriptor, 1, false, matrix, 0)
    }

    companion object {

        private const val TAG = "SphereProgram"

        private const val A_POSITION = "a_Position"
        private const val U_MATRIX = "u_Matrix"
    }
}