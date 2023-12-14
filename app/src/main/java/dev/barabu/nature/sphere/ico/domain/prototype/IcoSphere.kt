package dev.barabu.nature.sphere.ico.domain.prototype

import android.opengl.GLES20.GL_LINES
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import android.opengl.GLES20.glLineWidth
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.NORMAL_COMPONENT_COUNT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.domain.Vertex
import dev.barabu.base.extentions.asVector
import dev.barabu.base.extentions.toArray
import dev.barabu.base.extentions.toIntArray
import dev.barabu.base.geometry.Icosahedron
import dev.barabu.base.geometry.Icosahedron.Companion.TRIANGLES_COUNT
import dev.barabu.base.geometry.Icosahedron.Companion.VERTICES_COUNT
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexBuffer
import dev.barabu.nature.sphere.Mode
import dev.barabu.nature.sphere.ico.domain.IsoSphereVertexArray
import kotlin.math.pow


/**
 * INFO: Эта версия сферы имеет неоптимальный алгоритм формирования треугольников, индексов и линий.
 *  Один и тот же вертекс дублируется в нескольких треугольниках. Линии тоже повторяются.
 *
 * Тестовые классы см. в dev(test) IcoBuilder.kt
 *
 * [radius] - радиус окружности
 * [subdivisions] - количество "скруглений" Icosahedron'а
 */
class IcoSphere(
    private val radius: Float,
    private val subdivisions: Int = 1,
    isFlat: Boolean = true
) : Model {

    private class Data(
        val vertices: FloatArray,
        val triangles: IntArray,
        val lines: IntArray
    )

    private val vertexArray: IsoSphereVertexArray

    private val icosahedron = Icosahedron(radius)

    /** Про формулу смотри в (test) PowerTests.kt */
    private val trianglesCount = (TRIANGLES_COUNT * 2.0.pow(subdivisions * 2)).toInt()

    private var trianglesElementsCount: Int = trianglesCount * 3

    private var linesElementsCount: Int = 0

    /**
     * Собираем сферу из ромбиков Icosahedron'а на 12 вертексах.
     * Получаем массив вертексов для 20 треугольников. Каждый треугольник - 3 вертекса, всего 60
     * вертексов. Все треугольники идут последовательно, каждый вертекс участвует только в одном
     * треугольнике, поэтому индексы вертексов можно сразу использовать для рисования.
     * Все треугольники counter clockwise.
     */
    private val icoSphere: Array<Vertex> by lazy {
        Array(TRIANGLES_COUNT * 3) { Vertex(0f, 0f, 0f) }.also { vertices ->
            var index = 0

            //    00  00  00  00  00
            //    /\  /\  /\  /\  /\
            //   /  \/  \/  \/  \/  \
            //  01--02--03--04--05--01  <-- upDiamonds (индексы вертексов в icosahedron.vertices)
            //   \  /\  /\  /\  /\  /
            //    \/  \/  \/  \/  \/
            //    06  07  08  09  10
            val upDiamonds = IntArray(5) { it + 1 } + 1

            repeat(upDiamonds.size - 1) { i ->
                vertices[index++] = icosahedron.vertices[upDiamonds[i]]
                vertices[index++] = icosahedron.vertices[upDiamonds[i + 1]]
                vertices[index++] = icosahedron.vertices[0]

                vertices[index++] = icosahedron.vertices[upDiamonds[i]]
                vertices[index++] = icosahedron.vertices[upDiamonds[i] + 5]
                vertices[index++] = icosahedron.vertices[upDiamonds[i + 1]]
            }

            //    02  03  04  05  01  <-- это upDiamonds без первого элемента
            //    /\  /\  /\  /\  /\
            //   /  \/  \/  \/  \/  \
            //  06--07--08--09--10--06  <-- lowDiamonds (индексы вертексов в icosahedron.vertices)
            //   \  /\  /\  /\  /\  /
            //    \/  \/  \/  \/  \/
            //    11  11  11  11  11
            val lowDiamonds = IntArray(5) { it + 6 } + 6
            repeat(lowDiamonds.size - 1) { i ->
                vertices[index++] = icosahedron.vertices[lowDiamonds[i]]
                vertices[index++] = icosahedron.vertices[lowDiamonds[i + 1]]
                vertices[index++] = icosahedron.vertices[upDiamonds[i + 1]]

                vertices[index++] = icosahedron.vertices[lowDiamonds[i]]
                vertices[index++] = icosahedron.vertices[11]
                vertices[index++] = icosahedron.vertices[lowDiamonds[i + 1]]
            }
        }
    }

    init {
        val data = buildSphereSmooth()

        vertexArray = if (isFlat) {
            IsoSphereVertexArray(
                VertexBuffer(data.vertices),
                ElementBuffer(data.triangles),
                ElementBuffer(data.lines)
            )
        } else {
            IsoSphereVertexArray(
                VertexBuffer(data.vertices),
                ElementBuffer(data.triangles),
                ElementBuffer(data.lines)
            )
        }
    }

    override fun draw() {
        vertexArray.bind()
        drawPolygons()
        drawLines()
        vertexArray.release()
    }

    fun draw(mode: Mode) {
        vertexArray.bind()

        when (mode) {
            Mode.Polygon -> drawPolygons()
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
        glDrawElements(GL_TRIANGLES, trianglesElementsCount, GL_UNSIGNED_INT, 0)
    }

    /**
     * Рисуем линии по сфере
     */
    private fun drawLines() {
        vertexArray.bindLines()
        glLineWidth(1.2f)
        glDrawElements(GL_LINES, linesElementsCount, GL_UNSIGNED_INT, 0)
    }

    override fun bindAttributes(attributes: List<Attribute>) {
        vertexArray.bind()
        attributes.forEach { attr ->
            val (componentCount, offset, stride) = when (attr.type) {
                Attribute.Type.Position, Attribute.Type.Color, Attribute.Type.Tex -> {
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

    private fun buildSphereSmooth(): Data {

        val sphere = subdivideSphere(icoSphere, subdivisions)

        // Это вертексы и нормали
        val interleavedVertices = sphere.asSequence().map { vertex ->
            val normal = vertex.asVector.unit
            listOf(vertex.x, vertex.y, vertex.z, normal.x, normal.y, normal.z)
        }.flatten().toList().toFloatArray()

        // Это индексы вертексов для рисования треугольников
        val triangleElements: IntArray = (0..sphere.lastIndex).toIntArray()

        // Это индексы вертексов для рисования линий
        val lines = mutableMapOf<ULong, List<Int>>()

        triangleElements.asSequence()
            .windowed(3, 3)  // собираем треугольник из трех индексов
            .map { list -> list.sorted() }  // сортируем индексы по возрастанию
            // здесь пытаемся сохранять уникальные линии, чтобы не дублировать,
            // но сейчас это не работает, потому что нет shared вертексов.
            .forEach { list ->
                var key: ULong = (list[1].toULong() shl 32) or list[0].toULong()
                if (lines[key] == null) {
                    lines[key] = listOf(list[0], list[1])
                }
                key = (list[2].toULong() shl 32) or list[0].toULong()
                if (lines[key] == null) {
                    lines[key] = listOf(list[0], list[2])
                }
                key = (list[2].toULong() shl 32) or list[1].toULong()
                if (lines[key] == null) {
                    lines[key] = listOf(list[1], list[2])
                }
            }

        val lineElements = lines.values.flatten().toIntArray()
        linesElementsCount = lineElements.size
        return Data(interleavedVertices, triangleElements, lineElements)
    }

    /**
     * Разделяем каждый треугольник поверхности сферы [sphere] на 4 треугольника. Параметр
     * [level] содержит количество проходов деления. Проходы выполняются рекурсивно. После
     * каждого прохода количество вертексов возрастает в 4 раза по сравнению с предыдущим
     * уровнем рекурсии.
     */
    private fun subdivideSphere(sphere: Array<Vertex>, level: Int = 1): Array<Vertex> {
        // base case
        if (level <= 0) return sphere

        val subdividedSphere = sphere.asSequence()
            .windowed(3, 3) // берем по одному треугольнику
            .map { outerTriangle ->    // разделяем его на 4 треугольника
                val innerTriangle = getInnerTriangle(outerTriangle)
                zipTriangles(outerTriangle, innerTriangle)
            }.flatten().toArray(sphere.size * 4)

        return subdivideSphere(subdividedSphere, level - 1)
    }

    /**
     * Вставляем треугольник в треугольник [triangle] и возвращаем его вертексы.
     * Разбиваем треугольник на 4 внутренних. Делаем обход вершин 0 -> 1 -> 2 -> 0
     *
     *             V00
     *             / \
     *         v1 *---* v2
     *           / \ / \
     *      v01 *---*---* V02
     *             v1
     */
    private fun getInnerTriangle(triangle: List<Vertex>): List<Vertex> =
        // Обход v1 -> v2, v2 -> v3, v3 -> v1
        (triangle + triangle[0])
            .asSequence()
            .zipWithNext()
            .map { pair -> getMiddleVertex(pair.first, pair.second, radius) }
            .toList()

    /**
     *              V0
     *             / \
     *         v0 *---* v2
     *           / \ / \
     *       V1 *---*---* V2
     *             v1
     */
    private fun zipTriangles(outer: List<Vertex>, inner: List<Vertex>): List<Vertex> =
        listOf(
            outer[0], inner[0], inner[2],
            outer[1], inner[1], inner[0],
            outer[2], inner[2], inner[1],
            inner[0], inner[1], inner[2]
        )

    private fun getMiddleVertex(v1: Vertex, v2: Vertex, length: Float): Vertex {
        val tmp = v1.asVector + v2.asVector
        val scaleFactor = length / tmp.length()
        return Vertex(tmp.x * scaleFactor, tmp.y * scaleFactor, tmp.z * scaleFactor)
    }

    /**
     * Здесь пока не используются текстуры поэтому вертексы на полюсах могут использоваться
     * совместно в нескольких треугольниках. Напомню, что вертекс может быть shared, если
     * во всех треугольниках у него одинаковые:
     *  - позиция
     *  - нормаль
     *  - координаты текстуры
     *  Если натягивать текстуру на сферу, то вертекс полюса будет иметь разные координаты
     *  текстуры в каждом треугольнике.
     *
     *  Треугольники: 2xGL_TRIANGLE_FAN, 1xGL_TRIANGLE_STRIP
     *
     * 00      00  00  00  00  00
     *         /\  /\  /\  /\  /\
     *        /  \/  \/  \/  \/  \
     * 05    01--02--03--04--05--01
     *        \  /\  /\  /\  /\  /\
     *         \/  \/  \/  \/  \/  \
     * 11      06--07--08--09--10--06
     *          \  /\  /\  /\  /\  /
     *           \/  \/  \/  \/  \/
     * 17        11  11  11  11  11
     *
     *  Потом отрисовка выполнялась вот так:
     *   glDrawElements(GL_TRIANGLE_FAN, 7, GL_UNSIGNED_INT, TOP_TRIANGLE_FAN_OFFSET)
     *   glDrawElements(GL_TRIANGLE_STRIP, 12, GL_UNSIGNED_INT, CENTER_TRIANGLE_STRIP_OFFSET)
     *   glDrawElements(GL_TRIANGLE_FAN, 7, GL_UNSIGNED_INT, BOTTOM_TRIANGLE_FAN_OFFSET)
     */
    @Deprecated("The first worked version")
    private fun buildVerticesSmooth(): Data {
        val vertices = icosahedron.vertices

        val triangleIndices = listOf(
            listOf(0, 1, 2, 3, 4, 5, 1),  // top triangle fan
            listOf(11, 6, 10, 9, 8, 7, 6), // bottom triangle fan
            listOf(1, 6, 2, 7, 3, 8, 4, 9, 5, 10, 1, 6) // center triangle strip
        ).flatten().toIntArray()

        val lineIndices = listOf(
            listOf(0, 1, 0, 2, 0, 3, 0, 4, 0, 5), // линии от верхнего полюса (вертикальные)
            listOf(11, 6, 11, 7, 11, 8, 11, 9, 11, 10), // линии от нижнего полюса (вертикальные)
            listOf(1, 10, 1, 6), // линии центральной части вертикальные
            listOf(2, 6, 2, 7),
            listOf(3, 7, 3, 8),
            listOf(4, 8, 4, 9),
            listOf(5, 9, 5, 10),
            listOf(1, 2, 2, 3, 3, 4, 4, 5, 5, 1), // линии центральной части горизонтальные
            listOf(6, 7, 7, 8, 8, 9, 9, 10, 10, 6),
        ).flatten().toIntArray()

        linesElementsCount = lineIndices.size

        var j = 0

        val interleavedData =
            FloatArray(VERTICES_COUNT * (POSITION_COMPONENT_COUNT * NORMAL_COMPONENT_COUNT))

        for (i in 0 until VERTICES_COUNT) {
            interleavedData[j++] = vertices[i].x
            interleavedData[j++] = vertices[i].y
            interleavedData[j++] = vertices[i].z

            val n = vertices[i].asVector.unit

            interleavedData[j++] = n.x
            interleavedData[j++] = n.y
            interleavedData[j++] = n.z
        }

        return Data(interleavedData, triangleIndices, lineIndices)
    }

    companion object {

        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
}