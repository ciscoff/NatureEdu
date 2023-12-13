package dev.barabu.nature.sphere.ico.domain

import android.opengl.GLES20
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
import dev.barabu.base.geometry.Icosahedron
import dev.barabu.base.geometry.Vector
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexBuffer
import dev.barabu.nature.sphere.Mode

/**
 * В этой версии:
 *
 * Режим Smooth
 * ------------
 * - на основе Icosahedron'а создается базовая сфера [icoSphere] как набор 22 вертексов.
 * - треугольники формируются в массиве [triangleElements] индексами вертексов.
 * - subdivisions добавляет вертексы в хвост массива [icoSphere], а индексы перестраиваются и
 *   весь [triangleElements] перезаписывается. То есть в этом режиме вертексы одного треугольника
 *   разбросаны по массиву [icoSphere].
 *
 * Режим Flat
 * ----------
 * - на основе Icosahedron'а создается базовая сфера [icoSphere] из 60 вертексов (20 треугольников)
 * - subdivisions перестраивает содержимое [icoSphere]: вертексы образованных 4-х треугольников
 *   сериализуются последовательно и в итоге весь [icoSphere] перезаписывается.
 * - треугольники формируются в массиве [triangleElements] после каждого subdivisions просто
 *   как последовательный список индексов элементов [icoSphere].
 *
 * INFO: Отличие двух режимов в порядке формирования треугольников.
 *
 */

class IcoSphere(
    private val radius: Float,
    subdivisions: Int = 1,
    isFlat: Boolean = true
) : Model {

    private val vertexArray: IsoSphereVertexArray

    private val icosahedron = Icosahedron(radius)
    private val icoSphere = ArrayList<Vertex>()
    private val triangleElements = ArrayList<Int>()
    private val lineElements = ArrayList<Int>()

    private val vertexDataSmooth: FloatArray
        get() = icoSphere.asSequence()
            .map { vertex -> buildVertexDataSmooth(vertex) }
            .flatten().toList().toFloatArray()

    private val vertexDataFlat: FloatArray
        get() = triangleElements.asSequence()
            .windowed(3, 3)
            .map { triangleIndices -> triangleIndices.map { i -> icoSphere[i] } }
            .map { vertices -> buildVertexDataFlat(vertices) }
            .flatten().toList().toFloatArray()

    init {
        if (isFlat) {
            buildIcoSphereFlat()
            // сначала "закругляем"...
            subdivideSphereFlat(subdivisions)
            // ... потом перезаписываем индексы треугольников
            buildTriangleElementsFlat()
        } else {
            buildIcoSphereSmooth()
            buildTriangleElementsSmooth()
            subdivideSphereSmooth(subdivisions)
        }
        buildLineElements()

        vertexArray = IsoSphereVertexArray(
            VertexBuffer(if (isFlat) vertexDataFlat else vertexDataSmooth),
            ElementBuffer(triangleElements.toIntArray()),
            ElementBuffer(lineElements.toIntArray())
        )
    }

    private fun buildVertexDataSmooth(vertex: Vertex): List<Float> {
        val normal = vertex.asVector.unit
        return listOf(vertex.x, vertex.y, vertex.z, normal.x, normal.y, normal.z)
    }

    private fun buildVertexDataFlat(vertices: List<Vertex>): List<Float> {
        val normal = computeFaceNormal(vertices)
        return vertices.asSequence().map { vertex ->
            listOf(vertex.x, vertex.y, vertex.z, normal.x, normal.y, normal.z)
        }.flatten().toList()
    }

    private fun computeFaceNormal(vertices: List<Vertex>): Vector {
        val v21 = vertices[1].asVector - vertices[0].asVector
        val v31 = vertices[2].asVector - vertices[0].asVector
        val norm = v31.crossProduct(v21)
        return if (norm.length() > 0.000001f) norm.unit else Vector(0f, 0f, 0f)
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

    private fun drawPolygons() {
        vertexArray.bindPolygons()
        glDrawElements(GL_TRIANGLES, triangleElements.size, GL_UNSIGNED_INT, 0)
    }

    /**
     * Рисуем линии по сфере
     */
    private fun drawLines() {
        vertexArray.bindLines()
        glLineWidth(1.2f)
        glDrawElements(GLES20.GL_LINES, lineElements.size, GL_UNSIGNED_INT, 0)
    }

    /**
     * На первом этапе превращаем массив вертексов Icosahedron'а в массив вертексов базовой
     * сферы (22 вертекса). На втором этапе в [buildTriangleElementsSmooth] превратим массив вертексов
     * базовой сферы в массив индексов. Некоторые вертексы выполняют роль "соединителей" поэтому
     * их надо повторить (вертексы 00, 01, 06 и 11).
     *
     *   Вертексы сферы (этап 1)       Индексы вертексов сферы (этап 2)
     *
     *    00  00  00  00  00                00  01  02  03  04
     *    /\  /\  /\  /\  /\                /\  /\  /\  /\  /\
     *   /  \/  \/  \/  \/  \              /  \/  \/  \/  \/  \
     *  01--02--03--04--05--01            05--06--07--08--09--10
     *   \  /\  /\  /\  /\  /\    ==>>     \  /\  /\  /\  /\  /\
     *    \/  \/  \/  \/  \/  \             \/  \/  \/  \/  \/  \
     *    06--07--08--09--10--06            11--12--13--14--15--16
     *     \  /\  /\  /\  /\  /              \  /\  /\  /\  /\  /
     *      \/  \/  \/  \/  \/                \/  \/  \/  \/  \/
     *      11  11  11  11  11                17  18  19  20  21
     */
    private fun buildIcoSphereSmooth() {
        with(icoSphere) {
            repeat(5) { add(icosahedron.vertices[0]) }

            add(icosahedron.vertices[1])
            add(icosahedron.vertices[2])
            add(icosahedron.vertices[3])
            add(icosahedron.vertices[4])
            add(icosahedron.vertices[5])
            add(icosahedron.vertices[1])

            add(icosahedron.vertices[6])
            add(icosahedron.vertices[7])
            add(icosahedron.vertices[8])
            add(icosahedron.vertices[9])
            add(icosahedron.vertices[10])
            add(icosahedron.vertices[6])

            repeat(5) { add(icosahedron.vertices[11]) }
        }
    }

    /**
     * Собираем сферу из вертексов Icosahedron'а. Получаем массив из 60 вертексов для 20
     * треугольников. Если вертекс участвует в нескольких треугольниках, то он "размножается" по
     * количеству своих треугольников. Все треугольники идут последовательно поэтому индексы
     * вертексов можно сразу использовать для рисования. Все треугольники counter clockwise.
     */
    private fun buildIcoSphereFlat() {

        // Распределение вертексов Icosahedron'а в верхних ромбах
        //
        //    00  00  00  00  00
        //    /\  /\  /\  /\  /\
        //   /  \/  \/  \/  \/  \
        //  01--02--03--04--05--01  <-- upDiamonds (индексы вертексов в icosahedron.vertices)
        //   \  /\  /\  /\  /\  /
        //    \/  \/  \/  \/  \/
        //    06  07  08  09  10
        val upDiamonds = IntArray(5) { it + 1 } + 1

        with(icoSphere) {
            repeat(upDiamonds.size - 1) { i ->
                add(icosahedron.vertices[upDiamonds[i]])
                add(icosahedron.vertices[upDiamonds[i + 1]])
                add(icosahedron.vertices[0])

                add(icosahedron.vertices[upDiamonds[i]])
                add(icosahedron.vertices[upDiamonds[i] + 5])
                add(icosahedron.vertices[upDiamonds[i + 1]])
            }
        }

        // Распределение вертексов Icosahedron'а в нижних ромбах
        //
        //    02  03  04  05  01  <-- это upDiamonds без первого элемента
        //    /\  /\  /\  /\  /\
        //   /  \/  \/  \/  \/  \
        //  06--07--08--09--10--06  <-- lowDiamonds (индексы вертексов в icosahedron.vertices)
        //   \  /\  /\  /\  /\  /
        //    \/  \/  \/  \/  \/
        //    11  11  11  11  11
        val lowDiamonds = IntArray(5) { it + 6 } + 6
        with(icoSphere) {
            repeat(lowDiamonds.size - 1) { i ->
                add(icosahedron.vertices[lowDiamonds[i]])
                add(icosahedron.vertices[lowDiamonds[i + 1]])
                add(icosahedron.vertices[upDiamonds[i + 1]])

                add(icosahedron.vertices[lowDiamonds[i]])
                add(icosahedron.vertices[11])
                add(icosahedron.vertices[lowDiamonds[i + 1]])
            }
        }
    }

    /**
     * Это второй этап инициализации - формирование треугольников из ИНДЕКСОВ вертексов базовой
     * сферы. Делим все треугольники на два набора ромбиков и по каждому набору отрабатываем.
     * Все треугольники формируются как counter clockwise.
     */
    private fun buildTriangleElementsSmooth() {

        //    00  01  02  03  04
        //    /\  /\  /\  /\  /\
        //   /  \/  \/  \/  \/  \
        //  05--06--07--08--09--10  <-- upDiamonds
        //   \  /\  /\  /\  /\  /
        //    \/  \/  \/  \/  \/
        //    11  12  13  14  15
        val upDiamonds = IntArray(6) { it + 5 }
        repeat(5) { i ->
            triangleElements.add(upDiamonds[i])
            triangleElements.add(upDiamonds[i + 1])
            triangleElements.add(upDiamonds[i] - 5)

            triangleElements.add(upDiamonds[i])
            triangleElements.add(upDiamonds[i] + 6)
            triangleElements.add(upDiamonds[i + 1])
        }

        //    06  07  08  09  10
        //    /\  /\  /\  /\  /\
        //   /  \/  \/  \/  \/  \
        //  11--12--13--14--15--16  <-- lowDiamonds
        //   \  /\  /\  /\  /\  /
        //    \/  \/  \/  \/  \/
        //    17  18  19  20  21
        val lowDiamonds = IntArray(6) { it + 11 }
        repeat(5) { i ->
            triangleElements.add(lowDiamonds[i])
            triangleElements.add(lowDiamonds[i + 1])
            triangleElements.add(lowDiamonds[i] - 5)

            triangleElements.add(lowDiamonds[i])
            triangleElements.add(lowDiamonds[i] + 6)
            triangleElements.add(lowDiamonds[i + 1])
        }
    }

    private fun buildTriangleElementsFlat() {
        triangleElements.addAll((0..icoSphere.lastIndex))
    }

    /**
     * После того как сформированы треугольники можно переходить к созданию линий. Чтобы линии не
     * повторялись каждой из них присваивается уникальный ключ (high_index << 32 + low_index).
     * Если в словаре уже присутствует линия с таким ключом, то новая не добавляется.
     *
     * INFO: В режиме Flat оптимизация линий не работает, потому что каждый индекс участвует
     *  только в одном треугольнике.
     */
    private fun buildLineElements() {
        // Это индексы вертексов для рисования линий
        val lines = mutableMapOf<ULong, List<Int>>()

        triangleElements.asSequence()
            .windowed(3, 3)  // собираем треугольник из трех индексов
            .map { list -> list.sorted() }  // сортируем индексы по возрастанию
            // здесь сохраняем уникальные линии, чтобы не дублировать.
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

        lineElements.addAll(lines.values.flatten())
    }

    /**
     * INFO: В режиме Smooth мы накидываем новые, внутренние треугольники в хвост icoSphere,
     *  а потом "пересобираем" вообще все треугольники перестройкой индексов.
     *
     * Разделяем каждый треугольник на 4 треугольника. Параметр [level] содержит количество
     * рекурсивных проходов деления.
     */
    private fun subdivideSphereSmooth(level: Int) {
        // base case
        if (level <= 0) return

        triangleElements.asSequence()
            .windowed(3, 3)  // берем 3 индекса
            .map { outerTriangleIndices ->
                // формируем внешний треугольник
                val outerTriangle = outerTriangleIndices.map { i -> icoSphere[i] }
                // вычисляем внутренний треугольник
                val innerTriangle = getInnerTriangle(outerTriangle)
                // добавляем внутренний треугольник в список вертексов
                icoSphere.addAll(innerTriangle)
                // вычисляем 4 треугольника из внешнего и внутреннего.
                // на выходе получаем список индексов для вертексов 4-х треугольников.
                zipTriangles(
                    outerTriangleIndices,
                    ((icoSphere.size - 3)..icoSphere.lastIndex).toList()
                )
            }.flatten().toList().also { indices ->
                triangleElements.clear()
                triangleElements.addAll(indices)
            }

        subdivideSphereSmooth(level - 1)
    }

    private fun subdivideSphereFlat(level: Int) {
        // base case
        if (level <= 0) return

        icoSphere.asSequence()
            .windowed(3, 3) // берем треугольник
            .map { outerTriangle ->    // разделяем на 4 треугольника
                val innerTriangle = getInnerTriangle(outerTriangle)
                zipTriangles(outerTriangle, innerTriangle)
            }.flatten().toList().also { vertices ->
                icoSphere.clear()
                icoSphere.addAll(vertices)
            }

        return subdivideSphereFlat(level - 1)
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

    private fun getMiddleVertex(v1: Vertex, v2: Vertex, length: Float): Vertex {
        val tmp = v1.asVector + v2.asVector
        val scaleFactor = length / tmp.length()
        return Vertex(tmp.x * scaleFactor, tmp.y * scaleFactor, tmp.z * scaleFactor)
    }

    /**
     *  (V0, V1, V2) - outer tri
     *  (v0, v1, v2) - inner tri
     *
     *         V0
     *        / \
     *    v0 *---* v2
     *      / \ / \
     *  V1 *---*---* V2
     *        v1
     */
    private fun <T> zipTriangles(outer: List<T>, inner: List<T>): List<T> =
        listOf(
            outer[0], inner[0], inner[2],
            outer[1], inner[1], inner[0],
            outer[2], inner[2], inner[1],
            inner[0], inner[1], inner[2]
        )

    companion object {
        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
}