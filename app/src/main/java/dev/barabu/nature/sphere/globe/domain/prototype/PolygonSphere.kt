package dev.barabu.nature.sphere.globe.domain.prototype

import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.Logging
import dev.barabu.base.NORMAL_COMPONENT_COUNT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexArray
import dev.barabu.base.gl.VertexBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * ref: https://www.songho.ca/opengl/gl_sphere.html
 *
 * Stacks - это горизонтальные срезы сферы.
 * Sectors - это вертикальные "дольки апельсина"
 *
 * Реализовано два способа представления сферы - сеткой треугольников и сеткой линий.
 * Для каждой реализации создается отдельный интанс GlobeSphere, потому что один VAO не может
 * хранить несколько EBO.
 *
 * Как альтернатива - использовать один EBO и хранить в нем оба типа индексов, но не вперемешку,
 * а последовательно два блока. Нужно определить размеры каждого блока, чтобы выделить массив.
 *
 * Количество индексов для Polygon
 * ==============================
 * ((stackCount - 1) * sectorCount) * 6
 *
 * Количество индексов для Lines
 * ===============================
 * На каждом уровне по (sectorCount + 1) вертикальных линий.
 * Всего вертикальных линий ((sectorCount + 1) * stackCount)
 * На каждом уровне (кроме первого) по sectorCount горизонтальных линий
 * Всего горизонтальных линий ((sectorCount * (stackCount - 1))
 * Итого линий: 2 * stackCount * sectorCount + stackCount - sectorCount
 * Итого индексов для рисования линий: 2 * lineCount
 */
class PolygonSphere(
    radius: Float,
    stacks: Int = DEFAULT_STACK_COUNT,
    sectors: Int = DEFAULT_SECTOR_COUNT
) : Model {

    private val radius =
        if (radius == 0f) throw IllegalArgumentException("radius cannot be 0.0")
        else if (radius < 0) abs(radius)
        else radius

    private val stackCount = if (stacks < MIN_STACK_COUNT) MIN_STACK_COUNT else stacks

    private val sectorCount = if (sectors < MIN_SECTOR_COUNT) MIN_SECTOR_COUNT else sectors

    private val triangleCount = 2 * sectorCount * (stackCount - 1)

    // Количество индексов для отрисовки всех треугольников
    private val triangleElementCount = triangleCount * 3

    private val vertexArray: VertexArray = VertexArray(
        VertexBuffer(buildVertices()),
        ElementBuffer(buildElements())
    )

    override fun bindAttributes(attributes: List<Attribute>) {
        Logging.d("$TAG.bindAttributes")
        vertexArray.bind()
        attributes.forEach { attr ->
            val (componentCount, offset, stride) = when (attr.type) {
                Attribute.Type.Position, Attribute.Type.Color, Attribute.Type.Tex -> {
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
        glDrawElements(GL_TRIANGLES, triangleElementCount, GL_UNSIGNED_INT, 0)
        vertexArray.release()
    }

    /**
     * Вертексы сериализуются по горизонтальным слоям (Stacks), чтобы потом при обходе инкремент
     * индекса вертекса перемещал к следующему вертексу на том же горизонтальном уровне. Либо
     * на следующий горизонтальный уровень, если все вертексы текущего уровня пройдены.
     */
    private fun buildVertices(): FloatArray {
        Logging.d("$TAG.buildVertices")
        // Количество вертексов
        val vertexCount = (stackCount + 1) * (sectorCount + 1)
        // Объем vertex data
        val vertexDataSize = vertexCount * (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT)

        val floatArray = FloatArray(vertexDataSize)

        val lengthInv = 1.0f / radius
        val sectorStep = (2f * PI / sectorCount).toFloat()
        val stackStep = (PI / stackCount).toFloat()
        var index = 0

        repeat(stackCount + 1) { i ->
            val stackAngle = (PI / 2 - i * stackStep).toFloat() // от +PI/2 до -PI/2

            val xz: Float = (radius * cos(stackAngle))
            val y = radius * sin(stackAngle)

            repeat(sectorCount + 1) { j ->
                // Вокруг оси Y потому что 'x' получаем через cos
                val sectorAngle = j * sectorStep  // от 0 до 2*PI
                val x = xz * cos(sectorAngle)
                // note: вот тут можно со знаком поиграться
                val z = xz * sin(sectorAngle)

                // Координаты вертекса
                floatArray[index++] = x
                floatArray[index++] = y
                floatArray[index++] = z

                // Нормаль к вертексу
                floatArray[index++] = x * lengthInv
                floatArray[index++] = y * lengthInv
                floatArray[index++] = z * lengthInv
            }
        }

        return floatArray
    }

    private fun buildElements(): IntArray = buildPolygons()

    // Генерим CCW индексы вертексов для треугольников. Вся сфера разбита на набор секторов,
    // образованных пересечением горизонтальный и вертикальных линий. Точки пересечения - это
    // вертексы. На рисунке ниже:
    //  k1 - индекс первого вертекса сектора
    //  k1+1 - индекс следующего вертекса на той же горизонтальной линии
    //  k2 - индекс первого вертекса сектора на следующей горизонтальной линии
    //  k2+1 - по аналогии
    //
    //     k1--k1+1
    //     |  / |
    //     | /  |
    //     k2--k2+1
    private fun buildPolygons(): IntArray {
        val elements = IntArray(triangleElementCount)

        var index = 0
        repeat(stackCount) { i ->

            var k1 = i * (sectorCount + 1)  // индекс вертекса на текущем уровне
            var k2 = k1 + sectorCount + 1   // индекс вертекса на следующем уровне

            repeat(sectorCount) { j ->

                if (i != 0) {
                    elements[index++] = k1
                    elements[index++] = k2
                    elements[index++] = k1 + 1
                }

                if (i != (stackCount - 1)) {
                    elements[index++] = k1 + 1
                    elements[index++] = k2
                    elements[index++] = k2 + 1
                }

                k1++
                k2++
            }
        }

        return elements
    }

    companion object {

        private const val TAG = "GlobeSphere"

        private const val DEFAULT_STACK_COUNT = 18
        private const val DEFAULT_SECTOR_COUNT = 36

        private const val MIN_SECTOR_COUNT = 3
        private const val MIN_STACK_COUNT = 2

        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
}