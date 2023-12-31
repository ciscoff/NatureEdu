package dev.barabu.nature.sphere.ico.gl

import android.content.Context
import android.opengl.GLES20
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.sphere.Mode
import dev.barabu.nature.sphere.ico.domain.IcoSphere

class IcoSphereProgram(
    context: Context,
    isFlat: Boolean,
    subdivisions: Int = 1,
    radius: Float = 1.0f,
    vertexShaderResourceId: Int = R.raw.icosphere_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.icosphere_fragment_shader
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    private var uViewMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_VIEW_MATRIX)

    private var uProjMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_PROJECTION_MATRIX)

    private var uModelMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_MODEL_MATRIX)

    private var uColorDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_COLOR)

    private var uLightPositionDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_LIGHT_POSITION)

    private var uViewerPositionDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_VIEWER_POSITION)

    private var uDrawPolygonDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_DRAW_POLYGON)

    private var uTexUnitDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_TEX_UNIT)

    private val aPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_POSITION)

    private val aTexPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_TEX_POSITION)

    private var aNormalDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_NORMAL)

    override val model: Model =
        IcoSphere(radius = radius, subdivisions = subdivisions, isFlat = isFlat)

    init {
        model.bindAttributes(
            listOf(
                Attribute(aPositionDescriptor, Attribute.Type.Position),
                Attribute(aNormalDescriptor, Attribute.Type.Normal),
                Attribute(aTexPositionDescriptor, Attribute.Type.Tex)
            )
        )
    }

    override fun draw() {
        model.draw()
        GLES20.glUseProgram(0)
    }

    fun draw(mode: Mode, isFinal: Boolean) {
        (model as IcoSphere).draw(mode)
        if (isFinal) {
            GLES20.glUseProgram(0)
        }
    }

    fun bindColorUniform(color: Vector) {
        GLES20.glUniform3f(uColorDescriptor, color.r, color.g, color.b)
    }

    fun bindModelMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uModelMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindViewMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uViewMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindProjMatrixUniform(matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(uProjMatrixDescriptor, 1, false, matrix, 0)
    }

    fun bindLightPositionUniform(position: Point) {
        GLES20.glUniform3f(uLightPositionDescriptor, position.x, position.y, position.z)
    }

    fun bindViewerPositionUniform(position: Point) {
        GLES20.glUniform3f(uViewerPositionDescriptor, position.x, position.y, position.z)
    }

    fun bindDrawPolygonUniform(isPolygon: Boolean) {
        GLES20.glUniform1i(uDrawPolygonDescriptor, if (isPolygon) 1 else 0)
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
            GLES20.glUniform3f(
                GLES20.glGetUniformLocation(programDescriptor, e.key),
                e.value.r,
                e.value.g,
                e.value.b
            )
        }
        GLES20.glUniform1f(
            GLES20.glGetUniformLocation(programDescriptor, "u_Material.shininess"),
            shininess
        )
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
            GLES20.glUniform3f(
                GLES20.glGetUniformLocation(programDescriptor, e.key),
                e.value.r,
                e.value.g,
                e.value.b
            )
        }
    }

    fun bindTexUniform(textureId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTexUnitDescriptor, 0)
    }

    companion object {
        private const val A_POSITION = "a_Position"
        private const val A_TEX_POSITION = "a_TexPos"
        private const val A_NORMAL = "a_Normal"
        private const val U_MODEL_MATRIX = "u_ModelMatrix"
        private const val U_COLOR = "u_Color"
        private const val U_DRAW_POLYGON = "u_DrawPolygon"
        private const val U_LIGHT_POSITION = "u_LightPos"
        private const val U_VIEWER_POSITION = "u_ViewerPos"
        private const val U_TEX_UNIT = "u_TexUnit"
        private const val U_VIEW_MATRIX = "u_ViewMatrix"
        private const val U_PROJECTION_MATRIX = "u_ProjMatrix"
    }
}