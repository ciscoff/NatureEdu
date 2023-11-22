package dev.barabu.base.gl

import android.opengl.GLES30.GL_ELEMENT_ARRAY_BUFFER
import android.opengl.GLES30.GL_STATIC_DRAW
import android.opengl.GLES30.glBindBuffer
import android.opengl.GLES30.glBufferData
import android.opengl.GLES30.glGenBuffers
import dev.barabu.base.BYTES_PER_INT
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ElementBuffer(private val indices: IntArray) {

    private val bufferId: Int

    init {
        // 1. Создаем буфер в GPU
        val buffers = IntArray(1)
        glGenBuffers(buffers.size, buffers, 0)

        if (buffers[0] == INVALID_DESCRIPTOR) {
            throw RuntimeException("Could not create a new element buffer object.")
        }
        bufferId = buffers[0]
    }

    fun pushData() {
        Logging.d("$TAG.pushData")

        // 2.1 Выделяем временный нативный буфер и заливаем туда вертексы
        val elementBuffer = ByteBuffer
            .allocateDirect(indices.size * BYTES_PER_INT)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer().apply {
                put(indices)
                position(0)
            }

        // 2.3 Привязка буфера
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferId)

        // 2.3 Копируем временный нативный буфер в буфер GPU.
        // Копирование вертексов в буфер GPU выполняется через временный буфер в нативной памяти.
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            elementBuffer.capacity() * BYTES_PER_INT,
            elementBuffer,
            GL_STATIC_DRAW
        )
    }

    companion object {
        private const val TAG = "   ElementBuffer"
    }
}