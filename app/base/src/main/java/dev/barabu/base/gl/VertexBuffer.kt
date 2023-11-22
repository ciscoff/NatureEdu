package dev.barabu.base.gl

import android.opengl.GLES30.GL_ARRAY_BUFFER
import android.opengl.GLES30.GL_FLOAT
import android.opengl.GLES30.GL_STATIC_DRAW
import android.opengl.GLES30.glBindBuffer
import android.opengl.GLES30.glBufferData
import android.opengl.GLES30.glEnableVertexAttribArray
import android.opengl.GLES30.glGenBuffers
import android.opengl.GLES30.glVertexAttribPointer
import android.opengl.GLES30
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vertex Buffer Object (VBO) - это буфер для хранения vertex data.
 * Он умеет сохранять данные в буфере GPU и нацеливать на них атрибуты shader-программы
 *
 * ref: https://learnopengl.com/Getting-started/Hello-Triangle
 */
class VertexBuffer(private val vertexData: FloatArray) {

    private val bufferId: Int

    init {
        // 1. Создаем буфер в GPU
        val buffers = IntArray(1)
        glGenBuffers(buffers.size, buffers, 0)

        if (buffers[0] == INVALID_DESCRIPTOR) {
            throw RuntimeException("Could not create a new vertex buffer object.")
        }
        bufferId = buffers[0]
    }

    fun pushData() {
        Logging.d("$TAG.pushData")

        // 2.1 Выделяем временный нативный буфер и заливаем туда вертексы
        val vertexArray = ByteBuffer
            .allocateDirect(vertexData.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexData)
                position(0)
            }

        // 2.3 Привязка буфера
        glBindBuffer(GL_ARRAY_BUFFER, bufferId)

        // 2.3 Копируем временный нативный буфер в буфер GPU.
        // Копирование вертексов в буфер GPU выполняется через временный буфер в нативной памяти.
        glBufferData(
            GL_ARRAY_BUFFER,
            vertexArray.capacity() * BYTES_PER_FLOAT,
            vertexArray,
            GL_STATIC_DRAW
        )
    }

    /**
     * Нацеливает атрибут [attrDescriptor] на позицию [offset] буфера [bufferId]
     */
    fun bindAttribute(attrDescriptor: Int, offset: Int, componentCount: Int, stride: Int) {
        Logging.d("$TAG.bindAttribute")
        glVertexAttribPointer(
            attrDescriptor,
            componentCount,
            GL_FLOAT,
            false,
            stride,
            offset
        )
        glEnableVertexAttribArray(attrDescriptor)
    }

    companion object {
        private const val TAG = "   VertexBuffer"
    }
}