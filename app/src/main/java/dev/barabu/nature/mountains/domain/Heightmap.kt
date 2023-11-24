package dev.barabu.nature.mountains.domain

import android.graphics.Bitmap
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.ELEMENTS_PER_TRIANGLE
import dev.barabu.base.Logging
import dev.barabu.base.NORMAL_COMPONENT_COUNT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.TRIANGLES_PER_GROUP
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.extentions.toElements
import dev.barabu.base.extentions.toVertexData
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexArray
import dev.barabu.base.gl.VertexBuffer

class Heightmap(bitmap: Bitmap) : Model {

    init {
        if (bitmap.width * bitmap.height > 65536) {
            throw RuntimeException("Heightmap is too large for the index buffer")
        }
    }

    /**
     * Число ИНДЕКСОВ, которые нам понадобятся. У каждого pix битмапы есть свой 1D индекс и общее
     * количество индексов равно количеству вертексов. Однако один вертекс может участвовать в
     * нескольких треугольниках. Поэтому вычисляем именно число индексов для описания всех треугольников.
     */
    private val numElements =
        (bitmap.width - 1) * (bitmap.height - 1) * TRIANGLES_PER_GROUP * ELEMENTS_PER_TRIANGLE

    private val vertexArray = VertexArray(
        VertexBuffer(bitmap.toVertexData(TOTAL_COMPONENT_COUNT)),
        ElementBuffer(bitmap.toElements(numElements))
    )

    /**
     * Здесь будет только один атрибут 'a_Position'
     */
    override fun bindAttributes(attributes: List<Attribute>) {
        Logging.d("$TAG.bindAttributes")
        vertexArray.bind()
        attributes.forEach { attr ->
            val (componentCount, offset, stride) = when (attr.type) {
                Attribute.Type.Position, Attribute.Type.Color -> {
                    Triple(POSITION_COMPONENT_COUNT, 0, STRIDE)
                }

                Attribute.Type.Normal -> {
                    Triple(
                        NORMAL_COMPONENT_COUNT,
                        POSITION_COMPONENT_COUNT * BYTES_PER_FLOAT,
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
        glDrawElements(GL_TRIANGLES, numElements, GL_UNSIGNED_INT, 0)
        vertexArray.release()
    }

    companion object {
        private const val TAG = "Heightmap"

        private const val TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * BYTES_PER_FLOAT
    }
}