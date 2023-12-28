package dev.barabu.nature.sphere.planet.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.glUseProgram
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.sphere.planet.domain.PlanetSphere

class PlanetProgram(
    context: Context,
    subdivisions: Int = 1,
    radius: Float = 1.0f,
    vertexShaderResourceId: Int = R.raw.planet_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.planet_fragment_shader,
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    private var uModelMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_MODEL_MATRIX)

    private var uViewMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_VIEW_MATRIX)

    private var uProjMatrixDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_PROJECTION_MATRIX)

    private var uLightPositionDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_LIGHT_POSITION)

    private var uViewerPositionDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_VIEWER_POSITION)

    private var uDayTexUnitDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_TEX_UNIT_DAY)

    private var uNightTexUnitDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_TEX_UNIT_NIGHT)

    private var uCloudsTexUnitDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_TEX_UNIT_CLOUDS)

    private var uTimeDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_TIME)

    private var uResolutionDescriptor: Int =
        GLES20.glGetUniformLocation(programDescriptor, U_RESOLUTION)

    private val aPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_POSITION)

    private val aTexPositionDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_TEX_POSITION)

    private var aNormalDescriptor: Int =
        GLES20.glGetAttribLocation(programDescriptor, A_NORMAL)

    override val model: Model = PlanetSphere(radius = radius, subdivisions = subdivisions)

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
        glUseProgram(0)
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

    fun bindTimeUniform(time: Float) {
        GLES20.glUniform1f(uTimeDescriptor, time)
    }

    fun bindDayTexUniform(textureId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uDayTexUnitDescriptor, 0)
    }

    fun bindNightTexUniform(textureId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uNightTexUnitDescriptor, 1)
    }

    fun bindCloudsTexUniform(textureId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uCloudsTexUnitDescriptor, 2)
    }

    fun bindResolutionUniform(width: Float, height: Float) {
        GLES20.glUniform2f(uResolutionDescriptor, width, height)
    }

    companion object {
        private const val A_POSITION = "a_Position"
        private const val A_NORMAL = "a_Normal"
        private const val A_TEX_POSITION = "a_TexPos"
        private const val U_MODEL_MATRIX = "u_ModelMatrix"
        private const val U_VIEW_MATRIX = "u_ViewMatrix"
        private const val U_PROJECTION_MATRIX = "u_ProjMatrix"
        private const val U_LIGHT_POSITION = "u_LightPos"
        private const val U_VIEWER_POSITION = "u_ViewerPos"
        private const val U_TEX_UNIT_DAY = "u_TexUnitDay"
        private const val U_TEX_UNIT_NIGHT = "u_TexUnitNight"
        private const val U_TEX_UNIT_CLOUDS = "u_TexUnitClouds"
        private const val U_RESOLUTION = "u_Resolution"
        private const val U_TIME = "u_Time"
    }
}