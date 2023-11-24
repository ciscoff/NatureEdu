package dev.barabu.base.gl

import android.opengl.GLES30.glBindVertexArray
import android.opengl.GLES30.glGenVertexArrays
import dev.barabu.base.INVALID_DESCRIPTOR

/**
 * Vertex Array Object (VBO) является "контейнером" для отдельного Vertex Buffer Object (VBO)
 * и является хранилкой всех настроек, сделанных с помощью VBO.
 *
 * ref: https://learnopengl.com/Getting-started/Hello-Triangle
 */
class VertexArray(private val vertexBuffer: VertexBuffer, elementBuffer: ElementBuffer?) {

    private val arrayId: Int

    init {
        val arrays = IntArray(1)
        glGenVertexArrays(arrays.size, arrays, 0)

        if (arrays[0] == INVALID_DESCRIPTOR) {
            throw RuntimeException("Could not create a new vertex array object.")
        }
        arrayId = arrays[0]

        bind()
        vertexBuffer.pushData()
        elementBuffer?.pushData()
        release()
    }

    fun bind() {
        glBindVertexArray(arrayId)
    }

    fun release() {
        glBindVertexArray(0)
    }

    fun bindAttribute(attrDescriptor: Int, offset: Int, componentCount: Int, stride: Int) {
        vertexBuffer.bindAttribute(attrDescriptor, offset, componentCount, stride)
    }

    companion object {
        private const val TAG = "  VertexArray"
    }
}