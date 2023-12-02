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
import dev.barabu.base.domain.Vertex
import dev.barabu.base.extentions.asVector
import dev.barabu.base.geometry.Vector
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
    sectors: Int = DEFAULT_SECTOR_COUNT,
    isFlat: Boolean = true
) : Model {

    private class Data(
        val vertices: FloatArray,
        val triangles: IntArray,
        val lines: IntArray
    )

    /**
     * Режим отрисовки.
     */
    enum class Mode {
        Solid,
        Line,
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

    private val vertexArray: SphereVertexArray

    init {
        vertexArray = if (isFlat) {
            val data = buildVerticesFlat()
            SphereVertexArray(
                VertexBuffer(data.vertices),
                ElementBuffer(data.triangles),
                ElementBuffer(data.lines)
            )
        } else {
            SphereVertexArray(
                VertexBuffer(buildVertices()),
                ElementBuffer(buildTriangles()),
                ElementBuffer(buildLines())
            )
        }
    }

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
        drawLines()
        drawPolygons()
        vertexArray.release()
    }

    fun draw(mode: Mode) {
        vertexArray.bind()

        when (mode) {
            Mode.Solid -> drawPolygons()
            Mode.Line -> drawLines()
            Mode.Both -> {
                drawPolygons()
                drawLines()
            }
        }
        vertexArray.release()
    }

    /**
     * Закрашиваем треугольниками
     */
    private fun drawPolygons() {
        vertexArray.bindPolygons()
        glDrawElements(GL_TRIANGLES, triangleElementCount, GL_UNSIGNED_INT, 0)
    }

    /**
     * Рисуем линии по сфере
     */
    private fun drawLines() {
        vertexArray.bindLines()
        glLineWidth(1.2f)
        glDrawElements(GL_LINES, lineElementsCount, GL_UNSIGNED_INT, 0)
    }

    private fun buildVerticesFlat(): Data {

        var vi1: Int
        var vi2: Int
        var k = 0
        var vi = 0
        var ti = 0
        var li = 0
        // Массив вертексов (только координаты)
        val vertices = getVertices()

        val triangleCount = (stackCount - 1) * sectorCount * 2

        // Каждый треугольник - 3 вертекса
        // Каждый вертекс - 3 координаты Float и 3 координаты нормали
        val vertexDataSize = triangleCount * 3 * (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT)

        val vertexData = FloatArray(vertexDataSize)
        val triangleIndices = IntArray(triangleElementCount)
        val lineIndices = IntArray(lineElementsCount)

        repeat(stackCount) { i ->

            // Это v1 и v2 на рисунке ниже
            vi1 = i * (sectorCount + 1)
            vi2 = (i + 1) * (sectorCount + 1)

            // 4 вертекса на сектор
            //  v1--v3
            //  |    |
            //  v2--v4
            repeat(sectorCount) { _ ->
                val v1 = vertices[vi1]
                val v2 = vertices[vi2]
                val v3 = vertices[vi1 + 1]
                val v4 = vertices[vi2 + 1]

                when (i) {
                    // Самый первый стек
                    0 -> {
                        val n = computeFaceNormal(v1.asVector, v2.asVector, v4.asVector)

                        vertexData[k++] = v1.x
                        vertexData[k++] = v1.y
                        vertexData[k++] = v1.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v2.x
                        vertexData[k++] = v2.y
                        vertexData[k++] = v2.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v4.x
                        vertexData[k++] = v4.y
                        vertexData[k++] = v4.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        triangleIndices[ti++] = vi
                        triangleIndices[ti++] = vi + 1
                        triangleIndices[ti++] = vi + 2

                        lineIndices[li++] = vi
                        lineIndices[li++] = vi + 1
                        vi += 3
                    }
                    // Последний стек
                    (stackCount - 1) -> {

                        val n = computeFaceNormal(v1.asVector, v2.asVector, v3.asVector)

                        vertexData[k++] = v1.x
                        vertexData[k++] = v1.y
                        vertexData[k++] = v1.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v2.x
                        vertexData[k++] = v2.y
                        vertexData[k++] = v2.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v3.x
                        vertexData[k++] = v3.y
                        vertexData[k++] = v3.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        triangleIndices[ti++] = vi
                        triangleIndices[ti++] = vi + 1
                        triangleIndices[ti++] = vi + 2

                        lineIndices[li++] = vi
                        lineIndices[li++] = vi + 1
                        lineIndices[li++] = vi
                        lineIndices[li++] = vi + 2

                        vi += 3
                    }

                    else -> {
                        val n = computeFaceNormal(v1.asVector, v2.asVector, v3.asVector)

                        vertexData[k++] = v1.x
                        vertexData[k++] = v1.y
                        vertexData[k++] = v1.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v2.x
                        vertexData[k++] = v2.y
                        vertexData[k++] = v2.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v3.x
                        vertexData[k++] = v3.y
                        vertexData[k++] = v3.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        vertexData[k++] = v4.x
                        vertexData[k++] = v4.y
                        vertexData[k++] = v4.z

                        vertexData[k++] = n.x
                        vertexData[k++] = n.y
                        vertexData[k++] = n.z

                        triangleIndices[ti++] = vi      // v1
                        triangleIndices[ti++] = vi + 1  // v2
                        triangleIndices[ti++] = vi + 2  // v3

                        triangleIndices[ti++] = vi + 2  // v3
                        triangleIndices[ti++] = vi + 1  // v2
                        triangleIndices[ti++] = vi + 3  // v4

                        lineIndices[li++] = vi       // v1,v2 вертикальная линия
                        lineIndices[li++] = vi + 1
                        lineIndices[li++] = vi       // v1,v3 горизонтальная линия
                        lineIndices[li++] = vi + 2
                        vi += 4
                    }
                }

                vi1++
                vi2++
            }
        }

        return Data(vertexData, triangleIndices, lineIndices)
    }

    private fun computeFaceNormal(v1: Vector, v2: Vector, v3: Vector): Vector {
        val v21 = v2 - v1
        val v31 = v3 - v1
        val norm = v31.crossProduct(v21)
        return if (norm.length() > 0.000001f) norm.unit else Vector(0f, 0f, 0f)
    }

    /**
     * Вертексы сериализуются по горизонтальным слоям (Stacks), чтобы потом при обходе инкремент
     * индекса вертекса перемещал к следующему вертексу на том же горизонтальном уровне. Либо
     * на следующий горизонтальный уровень, если все вертексы текущего уровня пройдены.
     */
    private fun buildVertices(withNormals: Boolean = true): FloatArray {
        Logging.d("$TAG.buildVertices")
        // Количество вертексов
        val vertexCount = (stackCount + 1) * (sectorCount + 1)
        // Объем vertex data
        val normalComponentCount = if (withNormals) NORMAL_COMPONENT_COUNT else 0
        val vertexDataSize = vertexCount * (POSITION_COMPONENT_COUNT + normalComponentCount)

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
                if (withNormals) {
                    floatArray[index++] = x * lengthInv
                    floatArray[index++] = y * lengthInv
                    floatArray[index++] = z * lengthInv
                }
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

    private fun getVertices(): List<Vertex> =
        with(buildVertices(withNormals = false)) {
            (0 until size / POSITION_COMPONENT_COUNT).map { i ->
                Vertex(
                    get(i * POSITION_COMPONENT_COUNT),
                    get(i * POSITION_COMPONENT_COUNT + 1),
                    get(i * POSITION_COMPONENT_COUNT + 2)
                )
            }
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