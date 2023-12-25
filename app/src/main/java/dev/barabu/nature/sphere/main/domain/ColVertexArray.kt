package dev.barabu.nature.sphere.main.domain

import android.opengl.GLES30
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexBuffer

/**
 * GlobeSphere Vertex Array Object (VAO)
 */
class ColVertexArray(
    private val vertexBuffer: VertexBuffer,
    private val polygonElements: ElementBuffer,
    private val lineElements: ElementBuffer
) {

    private val arrayId: Int

    init {
        val arrays = IntArray(1)
        GLES30.glGenVertexArrays(arrays.size, arrays, 0)

        if (arrays[0] == INVALID_DESCRIPTOR) {
            throw RuntimeException("Could not create a new vertex array object.")
        }
        arrayId = arrays[0]

        bind()
        vertexBuffer.pushData()
        polygonElements.pushData()
        lineElements.pushData()
        release()
    }

    fun bind() {
        GLES30.glBindVertexArray(arrayId)
    }

    fun release() {
        GLES30.glBindVertexArray(0)
    }

    fun bindPolygons() {
        polygonElements.bind()
    }

    fun bindLines() {
        lineElements.bind()
    }

    fun bindAttribute(attrDescriptor: Int, offset: Int, componentCount: Int, stride: Int) {
        vertexBuffer.bindAttribute(attrDescriptor, offset, componentCount, stride)
    }
}