package dev.barabu.nature.sphere.gl

import android.content.Context
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUniform2f
import android.opengl.GLES20.glUniform3f
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLES20.glUseProgram
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.sphere.domain.PolygonSphere
import dev.barabu.nature.sphere.domain.StrokeSphere

class SphereProgram(
    context: Context,
    radius: Float,
    isPolygon: Boolean = false,
    vertexShaderResourceId: Int = R.raw.sphere_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.sphere_fragment_shader
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {
    override val model: Model = if (isPolygon) PolygonSphere(radius) else StrokeSphere(radius)

    private var uMatrixDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_MVP_MATRIX)

    private var uModelMatrixDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_MODEL_MATRIX)

    private var uColorDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_COLOR)

    private var uLightColorDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_LIGHT_COLOR)

    private var uLightPositionDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_LIGHT_POSITION)

    private val uIlluminateDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_ILLUMINATED)

    private val uResolution: Int =
        glGetUniformLocation(programDescriptor, U_RESOLUTION)

    private val aPositionDescriptor: Int =
        glGetAttribLocation(programDescriptor, A_POSITION)

    private var aNormalDescriptor: Int =
        glGetAttribLocation(programDescriptor, A_NORMAL)

    init {
        model.bindAttributes(
            listOf(
                Attribute(aPositionDescriptor, Attribute.Type.Position),
                Attribute(aNormalDescriptor, Attribute.Type.Normal)
            )
        )
    }

    override fun draw() {
        model.draw()
        glUseProgram(0)
    }

    fun bindColorUniform(color: Vector) {
        glUniform3f(uColorDescriptor, color.r, color.g, color.b)
    }

    fun bindMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindModelMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uModelMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindLightPositionUniform(position: Point) {
        glUniform3f(uLightPositionDescriptor, position.x, position.y, position.z)
    }

    fun bindLightColorUniform(color: Vector) {
        glUniform3f(uLightColorDescriptor, color.r, color.g, color.b)
    }

    fun bindIlluminateUniform(isIlluminate: Int) {
        glUniform1i(uIlluminateDescriptor, isIlluminate)
    }

    fun bindResolutionUniform(w: Float, h: Float) {
        glUniform2f(uResolution, w, h)
    }

    companion object {

        private const val TAG = "SphereProgram"

        private const val A_POSITION = "a_Position"
        private const val A_NORMAL = "a_Normal"
        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val U_MODEL_MATRIX = "u_ModelMatrix"
        private const val U_COLOR = "u_Color"
        private const val U_LIGHT_POSITION = "u_LightPos"
        private const val U_LIGHT_COLOR = "u_LightColor"
        private const val U_ILLUMINATED = "u_Illuminated"
        private const val U_RESOLUTION = "u_Resolution"
    }
}