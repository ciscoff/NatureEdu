package dev.barabu.nature.sphere.ico.domain

import android.opengl.GLES20.GL_LINES
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_TRIANGLE_FAN
import android.opengl.GLES20.GL_TRIANGLE_STRIP
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import android.opengl.GLES20.glLineWidth
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.BYTES_PER_INT
import dev.barabu.base.Logging
import dev.barabu.base.NORMAL_COMPONENT_COUNT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.domain.Vertex
import dev.barabu.base.extentions.asVector
import dev.barabu.base.extentions.toArray
import dev.barabu.base.extentions.toIntArray
import dev.barabu.base.geometry.Icosahedron
import dev.barabu.base.geometry.Icosahedron.Companion.VERTICES_COUNT
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexBuffer


/**
 * Тестовые классы см. в dev(test) IcoBuilder.kt
 */
class IcoSphere(
    private val radius: Float,
    isFlat: Boolean = true
) : Model {

    private class Data(
        val vertices: FloatArray,
        val triangles: IntArray,
        val lines: IntArray
    )

    enum class Mode {
        Polygon,
        Line,
        Both
    }

    private val vertexArray: IsoSphereVertexArray

    private val icosahedron = Icosahedron(radius)

    /**
     * Собираем сферу из ромбиков Icosahedron'а на 12 вертексах.
     * Получаем массив вертексов для 20 треугольников. Каждый треугольник - 3 вертекса, всего 60
     * вертексов. Все треугольники идут последовательно, каждый вертекс участвует только в одном
     * треугольнике, поэтому индексы вертексов можно сразу использовать для рисования.
     * Все треугольники clockwise.
     */
    private val icoSphere: Array<Vertex> by lazy {
        Array(60) { Vertex(0f, 0f, 0f) }.also { vertices ->
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
        glDrawElements(GL_TRIANGLES, 60, GL_UNSIGNED_INT, 0)
    }

    private var lineElementsCount: Int = 120

    /**
     * Рисуем линии по сфере
     */
    private fun drawLines() {
        vertexArray.bindLines()
        glLineWidth(1.2f)
        glDrawElements(GL_LINES, lineElementsCount, GL_UNSIGNED_INT, 0)
    }

    override fun bindAttributes(attributes: List<Attribute>) {
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

    /*private fun buildVerticesFlat(): FloatArray {
        val vertices = buildIcosahedronVertices()

        val interleavedVertices =
            FloatArray(ICOSAHEDRON_TRIANGLES_COUNT * 3 * (POSITION_COMPONENT_COUNT * NORMAL_COMPONENT_COUNT))

        for (i in 0 until ICOSAHEDRON_VERTICES_COUNT) {

        }
        return interleavedVertices
    }*/

    private fun buildSphereSmooth(): Data {

        // Это вертексы и нормали
        val interleavedVertices = icoSphere.asSequence().map { vertex ->
            val normal = vertex.asVector.unit
            listOf(vertex.x, vertex.y, vertex.z, normal.x, normal.y, normal.z)
        }.flatten().toList().toFloatArray()

        // Это индексы вертексов для рисования треугольников
        val triangles: IntArray = (0..icoSphere.lastIndex).toIntArray()

        // Это индексы вертексов для рисования линий
        val lines = mutableMapOf<ULong, List<Int>>()

        triangles.asSequence()
            .windowed(3, 3)
            .map { li -> li.sorted() }
            .forEach { li ->
                var key: ULong = (li[1].toULong() shl 32) or li[0].toULong()
                if (lines[key] == null) {
                    lines[key] = listOf(li[0], li[1])
                }
                key = (li[2].toULong() shl 32) or li[0].toULong()
                if (lines[key] == null) {
                    lines[key] = listOf(li[0], li[2])
                }
                key = (li[2].toULong() shl 32) or li[1].toULong()
                if (lines[key] == null) {
                    lines[key] = listOf(li[1], li[2])
                }
            }

        return Data(interleavedVertices, triangles, lines.values.toList().flatten().toIntArray())
    }

    // If subdivision=0, do nothing.
    //         v1           //
    //        / \           //
    // newV1 *---* newV3    //
    //      / \ / \         //
    //    v2---*---v3       //
    //        newV2         //
//    private fun buildVerticesSmooth(val subdivision: Int = 1): Data {
//        val vertices = icosahedron.vertices
//
//    }

    /**
     * Вставляем треугольник в треугольник [triangle] и возвращаем его вертексы.
     * Разбиваем треугольник на 4 внутренних. Делаем обход вершин 0 -> 1 -> 2 -> 0
     *
     *             V00
     *             / \
     *    v.00.01 *---* v.02.00
     *           / \ / \
     *      v01 *---*---* V02
     *           v.01.02
     */
//    private fun getInnerTriangle(triangle: Array<Vertex>): Array<String> {
//        return (triangle + triangle[0]).asSequence().zipWithNext().mapIndexed { i, p ->
//
//        }.toArray(3)
//    }

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

        lineElementsCount = lineIndices.size

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

        // Байтовые смещения в буфере индексов
        private const val TOP_TRIANGLE_FAN_OFFSET = 0
        private const val BOTTOM_TRIANGLE_FAN_OFFSET = 7 * BYTES_PER_INT
        private const val CENTER_TRIANGLE_STRIP_OFFSET = 14 * BYTES_PER_INT
    }
}