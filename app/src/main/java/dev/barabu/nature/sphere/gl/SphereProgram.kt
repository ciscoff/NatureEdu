package dev.barabu.nature.sphere.gl

import android.content.Context
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1f
import android.opengl.GLES20.glUniform1i
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
import dev.barabu.nature.sphere.domain.Sphere

class SphereProgram(
    context: Context,
    radius: Float,
    isFlat: Boolean,
    vertexShaderResourceId: Int = R.raw.sphere_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.sphere_fragment_shader
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {
    override val model: Model = Sphere(radius = radius, isFlat = isFlat)

    private var uMvpMatrixDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_MVP_MATRIX)

    private var uModelMatrixDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_MODEL_MATRIX)

    private var uColorDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_COLOR)

    private var uLightPositionDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_LIGHT_POSITION)

    private var uViewerPositionDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_VIEWER_POSITION)

    private var uDrawPolygonDescriptor: Int =
        glGetUniformLocation(programDescriptor, U_DRAW_POLYGON)

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

    fun draw(mode: Sphere.Mode, isFinal: Boolean) {
        (model as Sphere).draw(mode)
        if (isFinal) {
            glUseProgram(0)
        }
    }

    fun bindColorUniform(color: Vector) {
        glUniform3f(uColorDescriptor, color.r, color.g, color.b)
    }

    fun bindMvpMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uMvpMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindModelMatrixUniform(matrix: FloatArray) {
        glUniformMatrix4fv(uModelMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindLightPositionUniform(position: Point) {
        glUniform3f(uLightPositionDescriptor, position.x, position.y, position.z)
    }

    fun bindViewerPositionUniform(position: Point) {
        glUniform3f(uViewerPositionDescriptor, position.x, position.y, position.z)
    }

    fun bindDrawPolygonUniform(isPolygon: Boolean) {
        glUniform1i(uDrawPolygonDescriptor, if (isPolygon) 1 else 0)
    }

    fun bindMaterialUniform(
        ambient: Vector,
        diffuse: Vector,
        specular: Vector,
        shininess: Float
    ) {
        mapOf(
            "u_Material.ambient" to ambient,
            "u_Material.diffuse" to diffuse,
            "u_Material.specular" to specular
        ).forEach { e ->
            glUniform3f(
                glGetUniformLocation(programDescriptor, e.key),
                e.value.r,
                e.value.g,
                e.value.b
            )
        }
        glUniform1f(glGetUniformLocation(programDescriptor, "u_Material.shininess"), shininess)
    }

    fun bindLightUniform(
        ambient: Vector,
        diffuse: Vector,
        specular: Vector,
    ) {
        mapOf(
            "u_Light.ambient" to ambient,
            "u_Light.diffuse" to diffuse,
            "u_Light.specular" to specular
        ).forEach { e ->
            glUniform3f(
                glGetUniformLocation(programDescriptor, e.key),
                e.value.r,
                e.value.g,
                e.value.b
            )
        }
    }

    companion object {

        private const val TAG = "SphereProgram"

        private const val A_POSITION = "a_Position"
        private const val A_NORMAL = "a_Normal"
        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val U_MODEL_MATRIX = "u_ModelMatrix"
        private const val U_COLOR = "u_Color"
        private const val U_DRAW_POLYGON = "u_DrawPolygon"
        private const val U_LIGHT_POSITION = "u_LightPos"
        private const val U_VIEWER_POSITION = "u_ViewerPos"
    }
}