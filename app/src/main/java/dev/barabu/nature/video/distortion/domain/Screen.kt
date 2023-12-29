package dev.barabu.nature.video.distortion.domain

import android.opengl.GLES20
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_TRIANGLE_STRIP
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.TEX_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexBuffer

class Screen : Model {

    private val vertexArray: ScreenVertexArray = ScreenVertexArray(
        VertexBuffer(vertices),
        ElementBuffer(elements)
    )

    override fun bindAttributes(attributes: List<Attribute>) {
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

                Attribute.Type.Tex -> {
                    Triple(
                        TEX_COMPONENT_COUNT,
                        (POSITION_COMPONENT_COUNT + TEX_COMPONENT_COUNT) * BYTES_PER_FLOAT,
                        STRIDE
                    )
                }
            }
            vertexArray.bindAttribute(attr.descriptor, offset, componentCount, stride)
        }
        vertexArray.release()
    }

    override fun draw() {
        vertexArray.bind()
        drawPolygons()
        vertexArray.release()
    }

    private fun drawPolygons() {
        vertexArray.bindPolygons()
        GLES20.glDrawElements(GL_TRIANGLES, elements.size, GLES20.GL_UNSIGNED_INT, 0)
//        GLES20.glDrawElements(GL_TRIANGLE_STRIP, elements.size, GLES20.GL_UNSIGNED_INT, 0)

//        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }

    companion object {
        //      x       y     z     s     t
        private val vertices = floatArrayOf(
            -1.0f,  1.0f, 0.0f, 0.0f, 1.0f, // (0) Top-left
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, // (1) Bottom-left
             1.0f, -1.0f, 0.0f, 1.0f, 0.0f, // (2) Bottom-right
             1.0f,  1.0f, 0.0f, 1.0f, 1.0f  // (3) Top-right
        )

        private val elements = intArrayOf(
            0, 1, 2,
            0, 2, 3,
        )

        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + TEX_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
}