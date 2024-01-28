package dev.barabu.base.gl

import android.opengl.GLES20.GL_COMPILE_STATUS
import android.opengl.GLES20.GL_LINK_STATUS
import android.opengl.GLES20.GL_TRUE
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glGetProgramInfoLog
import android.opengl.GLES20.glGetProgramiv
import android.opengl.GLES20.glGetShaderInfoLog
import android.opengl.GLES20.glGetShaderiv
import android.opengl.GLES20.glLinkProgram
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES20.glUseProgram
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.domain.Model
import dev.barabu.base.domain.Shader

/**
 * Базовый класс для программ
 */
abstract class ShaderProgram(
    vertexShaderSrc: String,
    fragmentShaderSrc: String
) {

    val programDescriptor: Int by lazy {
        build(vertexShaderSrc, fragmentShaderSrc)
    }

    /**
     * Что рисовать
     */
    protected abstract val model: Model

    /**
     * Как рисовать
     */
    abstract fun draw()

    open fun useProgram() {
        glUseProgram(programDescriptor)
    }

    companion object {

        private fun build(
            vertexShaderSrc: String,
            fragmentShaderSrc: String
        ): Int {

            val vertexShader = compileShader(Shader.Vertex, vertexShaderSrc)
            val fragmentShader = compileShader(Shader.Fragment, fragmentShaderSrc)
            val programDescriptor = linkProgram(vertexShader, fragmentShader)

            val linkStatus = IntArray(1)
            glGetProgramiv(programDescriptor, GL_LINK_STATUS, linkStatus, 0)

            return if (linkStatus[0] != ERROR_CODE) {
                programDescriptor
            } else throw RuntimeException("Could not link program.")
        }

        private fun compileShader(shader: Shader, shaderCode: String): Int {

            // Создать объект шейдера в нативном пространстве и получить его дескриптор
            val shaderDescriptor: Int = glCreateShader(shader.type)

            if (shaderDescriptor == ERROR_CODE) {
                return INVALID_DESCRIPTOR
            }

            // Загрузить в созданный объект наш исходный код для шейдера
            glShaderSource(shaderDescriptor, shaderCode)

            // Скомпилировать исходный код в исполняемый
            glCompileShader(shaderDescriptor)

            // Проверить результат компиляции
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderDescriptor, GL_COMPILE_STATUS, compileStatus, 0)

            return if (compileStatus[0] != GL_TRUE) {
                throw RuntimeException(
                    "Could not compile ${shader.desc} shader. ${glGetShaderInfoLog(shaderDescriptor)}"
                )
            } else {
                shaderDescriptor
            }
        }

        private fun linkProgram(vertexShaderDescriptor: Int, fragmentShaderDescriptor: Int): Int {
            val programDescriptor = glCreateProgram()
            if (programDescriptor == ERROR_CODE) {
                return INVALID_DESCRIPTOR
            }

            glAttachShader(programDescriptor, vertexShaderDescriptor)
            glAttachShader(programDescriptor, fragmentShaderDescriptor)
            glLinkProgram(programDescriptor)

            val linkStatus = IntArray(1)
            glGetProgramiv(programDescriptor, GL_LINK_STATUS, linkStatus, 0)

            return if (linkStatus[0] != GL_TRUE) {
                throw RuntimeException(
                    "Could not link program. ${glGetProgramInfoLog(programDescriptor)}"
                )
            } else {
                programDescriptor
            }
        }
    }
}