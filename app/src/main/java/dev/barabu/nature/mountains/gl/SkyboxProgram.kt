package dev.barabu.nature.mountains.gl

import android.content.Context
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.GLES20.glUseProgram
import dev.barabu.base.Logging
import dev.barabu.base.TextResourceReader
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ShaderProgram
import dev.barabu.nature.R
import dev.barabu.nature.mountains.domain.Skybox

class SkyboxProgram(
    context: Context,
    vertexShaderResourceId: Int = R.raw.skybox_vertex_shader,
    fragmentShaderResourceId: Int = R.raw.skybox_fragment_shader
) : ShaderProgram(
    TextResourceReader.readTexFromResource(context, vertexShaderResourceId),
    TextResourceReader.readTexFromResource(context, fragmentShaderResourceId)
) {

    private var uMatrixDescriptor: Int = glGetUniformLocation(programDescriptor, U_MATRIX)

    private var uTexDescriptor: Int = glGetUniformLocation(programDescriptor, U_TEXTURE_UNIT)

    /*private val aPositionDescriptor: Int = 0*/
    private val aPositionDescriptor: Int = glGetAttribLocation(programDescriptor, A_POSITION)

    override val model: Model = Skybox()

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

    /**
     * Здесь [texDescriptor] - дескриптор текстуры (буфера), загруженной в нативное пространство,
     * [uTexDescriptor] - дескриптор униформа из шейдера. Мы свяжем фактические данные (буфер)
     * текстуры из [texDescriptor] с потребителем [uTexDescriptor] в шейдере.
     */
    fun bindTexUniform(texDescriptor: Int) {
        Logging.d("$TAG.bindTexUniform")
        // Делаем активным texture unit 0 и следующая операция отработает на активном texture unit.
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_CUBE_MAP, texDescriptor)
        // Записываем значение 0 в uniform u_TextureUnit шейдера текущей активной программы.
        // Фактически мы говорим texture sampler'у чтобы он использовал текстуру из texture unit 0.
        // Мля, почему тут пишем 0, а не GL_TEXTURE0 ??? Видимо это работает как индекс texture
        // unit'а в нативном пространстве, а не как его id в java-коде.
        glUniform1i(uTexDescriptor, 0)
    }

    override fun draw() {
        Logging.d("$TAG.draw")
        model.draw()
        glUseProgram(0)
    }

    companion object {

        private const val TAG = "SkyboxProgram"

        private const val A_POSITION = "a_Position"
        private const val U_MATRIX = "u_Matrix"
        private const val U_TEXTURE_UNIT = "u_TextureUnit"
    }
}