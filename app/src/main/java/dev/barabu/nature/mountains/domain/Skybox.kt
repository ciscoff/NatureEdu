package dev.barabu.nature.mountains.domain

import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import dev.barabu.base.Logging
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexArray
import dev.barabu.base.gl.VertexBuffer

class Skybox : Model {

    private val vertexArray: VertexArray =
        VertexArray(VertexBuffer(vertices), ElementBuffer(indices))

    /**
     * Здесь будет только один атрибут 'a_Position'
     */
    override fun bindAttributes(attributes: List<Attribute>) {
        Logging.d("$TAG.bindAttributes")

        vertexArray.bind()
        attributes.forEach { attr ->
            val (componentCount, offset, stride) = when (attr.type) {
                Attribute.Type.Position, Attribute.Type.Color, Attribute.Type.Normal -> {
                    Triple(
                        POSITION_COMPONENT_COUNT,
                        0,
                        STRIDE
                    )
                }
            }
            vertexArray.bindAttribute(attr.descriptor, offset, componentCount, stride)
        }
        vertexArray.release()
    }

    override fun draw() {
        Logging.d("$TAG.draw")
        vertexArray.bind()
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0)
        vertexArray.release()
    }

    companion object {

        private const val TAG = "Skybox"

        private const val STRIDE = 0

        /** 8 вершин куба */
        private val vertices = floatArrayOf(

            -1f, 1f, 1f, // (0) Top-left near
            1f, 1f, 1f, // (1) Top-right near
            -1f, -1f, 1f, // (2) Bottom-left near
            1f, -1f, 1f, // (3) Bottom-right near
            -1f, 1f, -1f, // (4) Top-left far
            1f, 1f, -1f, // (5) Top-right far
            -1f, -1f, -1f, // (6) Bottom-left far
            1f, -1f, -1f // (7) Bottom-right far
        )

        private val indices = intArrayOf(
            // Front (два треугольника фронтальной стороны)
            1, 3, 0,
            0, 3, 2,
            // Back (два треугольника задней стороны)
            4, 6, 5,
            5, 6, 7,
            // Left
            0, 2, 4,
            4, 2, 6,
            // Right
            5, 7, 1,
            1, 7, 3,
            // Top
            5, 1, 4,
            4, 1, 0,
            // Bottom
            6, 2, 7,
            7, 2, 3
        )
    }
}