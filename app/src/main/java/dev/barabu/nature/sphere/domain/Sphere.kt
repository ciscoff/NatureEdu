package dev.barabu.nature.sphere.domain

import android.opengl.GLES20.GL_LINES
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import android.opengl.GLES20.glLineWidth
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.Logging
import dev.barabu.base.NORMAL_COMPONENT_COUNT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.gl.ElementBuffer
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
 * Реализовано два способа отрисовки - сеткой треугольников (сплошная заливка) и сеткой линий.
 * Один VAO использует один VBO и два EBO (для треугольников и для линий).
 *
 * Количество индексов для треугольников
 * =====================================
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
class Sphere(
    radius: Float,
    stacks: Int = DEFAULT_STACK_COUNT,
    sectors: Int = DEFAULT_SECTOR_COUNT
) : Model {

    /**
     * Режим отрисовки.
     */
    enum class Mode {
        Solid,
        Mesh,
        Both
    }

    private val radius =
        if (radius == 0f) throw IllegalArgumentException("radius cannot be 0.0")
        else if (radius < 0) abs(radius)
        else radius


    private val stackCount = if (stacks < MIN_STACK_COUNT) MIN_STACK_COUNT else stacks

    private val sectorCount = if (sectors < MIN_SECTOR_COUNT) MIN_SECTOR_COUNT else sectors

    private val lineCount = 2 * stackCount * sectorCount + stackCount - sectorCount

    private val triangleCount = 2 * sectorCount * (stackCount - 1)

    // Количество индексов для отрисовки всех линий
    private val lineElementsCount = lineCount * 2

    // Количество индексов для отрисовки всех треугольников
    private val triangleElementCount = triangleCount * 3

    private val vertexArray: SphereVertexArray = SphereVertexArray(
        VertexBuffer(buildVertices()),
        ElementBuffer(buildTriangles()),
        ElementBuffer(buildLines())
    )

    override fun bindAttributes(attributes: List<Attribute>) {
        Logging.d("$TAG.bindAttributes")
        vertexArray.bind()
        attributes.forEach { attr ->
            val (componentCount, offset, stride) = when (attr.type) {
                Attribute.Type.Position, Attribute.Type.Color -> {
                    Triple(
                        POSITION_COMPONENT_COUNT,
                        0,
                        STRIDE
                    )
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
        drawMesh()
        drawSolid()
        vertexArray.release()
    }

    fun draw(mode: Mode) {
        vertexArray.bind()

        when (mode) {
            Mode.Solid -> drawSolid()
            Mode.Mesh -> drawMesh()
            Mode.Both -> {
                drawMesh()
                drawSolid()
            }
        }
        vertexArray.release()
    }

    /**
     * Закрашиваем треугольниками
     */
    private fun drawSolid() {
        vertexArray.bindPolygons()
        glDrawElements(GL_TRIANGLES, triangleElementCount, GL_UNSIGNED_INT, 0)
    }

    /**
     * Рисуем линии по сфере
     */
    private fun drawMesh() {
        vertexArray.bindLines()
        glLineWidth(1.6f)
        glDrawElements(GL_LINES, lineElementsCount, GL_UNSIGNED_INT, 0)
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
                val sectorAngle = j * sectorStep  // от 0 до 2*PI
                val x = xz * cos(sectorAngle)
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
    private fun buildTriangles(): IntArray {
        val elements = IntArray(triangleElementCount)

        var index = 0
        repeat(stackCount) { i ->

            var k1 = i * (sectorCount + 1)    // индекс вертекса на текущем уровне
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

    /**
     * На каждую линию по 2 индекса
     */
    private fun buildLines(): IntArray {
        Logging.d("$TAG.buildLines")
        val elements = IntArray(2 * lineCount)

        var index = 0
        repeat(stackCount) { i ->
            var k1 = i * (sectorCount + 1)    // индекс вертекса на текущем уровне
            var k2 = k1 + sectorCount + 1   // индекс вертекса на следующем уровне

            repeat(sectorCount) { j ->

                // Вертикальная линия
                elements[index++] = k1
                elements[index++] = k2

                // Горизонтальные линии для каждого сектора на всех уровнях кроме первого
                if (i != 0) {
                    elements[index++] = k1
                    elements[index++] = k1 + 1
                }
                k1++
                k2++
            }
        }
        Logging.d("$TAG.buildLines lineCount=$lineCount index=$index")
        return elements
    }

    companion object {

        private const val TAG = "Sphere"

        private const val DEFAULT_STACK_COUNT = 18
        private const val DEFAULT_SECTOR_COUNT = 36

        private const val MIN_SECTOR_COUNT = 3
        private const val MIN_STACK_COUNT = 2

        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
}