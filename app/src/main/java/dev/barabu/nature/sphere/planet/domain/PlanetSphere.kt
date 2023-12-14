package dev.barabu.nature.sphere.planet.domain

import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_UNSIGNED_INT
import android.opengl.GLES20.glDrawElements
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.NORMAL_COMPONENT_COUNT
import dev.barabu.base.POSITION_COMPONENT_COUNT
import dev.barabu.base.TEX_COMPONENT_COUNT
import dev.barabu.base.domain.Attribute
import dev.barabu.base.domain.Model
import dev.barabu.base.domain.Tex
import dev.barabu.base.domain.Vertex
import dev.barabu.base.extentions.asVector
import dev.barabu.base.extentions.serialized
import dev.barabu.base.geometry.Icosahedron
import dev.barabu.base.geometry.Vector
import dev.barabu.base.gl.ElementBuffer
import dev.barabu.base.gl.VertexBuffer

/**
 * Это [dev.barabu.nature.sphere.ico.domain.IcoSphere] в режиме Smooth.
 */
class PlanetSphere(
    private val radius: Float,
    subdivisions: Int = 1,
) : Model {

    private val vertexArray: PlanetVertexArray

    private val icosahedron = Icosahedron(radius)
    private val icoSphere = ArrayList<Vertex>()
    private val triangleElements = ArrayList<Int>()

    private val vertexData: FloatArray
        get() = icoSphere.asSequence()
            .map { vertex -> vertex.serialized }
            .flatten().toList().toFloatArray()

    init {
        buildIcoSphere()
        buildTriangleElements()
        subdivideSphere(subdivisions)

        vertexArray = PlanetVertexArray(
            VertexBuffer(vertexData),
            ElementBuffer(triangleElements.toIntArray())
        )
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

                Attribute.Type.Tex -> {
                    Triple(
                        TEX_COMPONENT_COUNT,
                        (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT) * BYTES_PER_FLOAT,
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
        glDrawElements(GL_TRIANGLES, triangleElements.size, GL_UNSIGNED_INT, 0)
    }

    /**
     * На первом этапе превращаем массив вертексов Icosahedron'а в массив вертексов базовой
     * сферы (22 вертекса). На втором этапе в [buildTriangleElements] превратим массив вертексов
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
    private fun buildIcoSphere() {
        with(icoSphere) {

            var normal = icosahedron.vertices[0].asVector.unit
            repeat(5) { i ->
                val tex = Tex((i * 2 + 1) * S_STEP, 0f)
                add(icosahedron.vertices[0].copy(normal = normal, tex = tex))
            }

            add(
                icosahedron.vertices[1].copy(
                    normal = icosahedron.vertices[1].asVector.unit,
                    tex = Tex(0f, T_STEP)
                )
            )
            add(
                icosahedron.vertices[2].copy(
                    normal = icosahedron.vertices[2].asVector.unit,
                    tex = Tex(2 * S_STEP, T_STEP)
                )
            )
            add(
                icosahedron.vertices[3].copy(
                    normal = icosahedron.vertices[3].asVector.unit,
                    tex = Tex(4 * S_STEP, T_STEP)
                )
            )
            add(
                icosahedron.vertices[4].copy(
                    normal = icosahedron.vertices[4].asVector.unit,
                    tex = Tex(6 * S_STEP, T_STEP)
                )
            )
            add(
                icosahedron.vertices[5].copy(
                    normal = icosahedron.vertices[5].asVector.unit,
                    tex = Tex(8 * S_STEP, T_STEP)
                )
            )
            add(
                icosahedron.vertices[1].copy(
                    normal = icosahedron.vertices[1].asVector.unit,
                    tex = Tex(10 * S_STEP, T_STEP)
                )
            )

            add(
                icosahedron.vertices[6].copy(
                    normal = icosahedron.vertices[6].asVector.unit,
                    tex = Tex(S_STEP, 2 * T_STEP)
                )
            )
            add(
                icosahedron.vertices[7].copy(
                    normal = icosahedron.vertices[7].asVector.unit,
                    tex = Tex(3 * S_STEP, 2 * T_STEP)
                )
            )
            add(
                icosahedron.vertices[8].copy(
                    normal = icosahedron.vertices[8].asVector.unit,
                    tex = Tex(5 * S_STEP, 2 * T_STEP)
                )
            )
            add(
                icosahedron.vertices[9].copy(
                    normal = icosahedron.vertices[9].asVector.unit,
                    tex = Tex(7 * S_STEP, 2 * T_STEP)
                )
            )
            add(
                icosahedron.vertices[10].copy(
                    normal = icosahedron.vertices[10].asVector.unit,
                    tex = Tex(9 * S_STEP, 2 * T_STEP)
                )
            )
            add(
                icosahedron.vertices[6].copy(
                    normal = icosahedron.vertices[6].asVector.unit,
                    tex = Tex(11 * S_STEP, 2 * T_STEP)
                )
            )

            normal = icosahedron.vertices[11].asVector.unit
            repeat(5) { i ->
                val tex = Tex((i * 2 + 2) * S_STEP, 3 * T_STEP)
                add(icosahedron.vertices[11].copy(normal = normal, tex = tex))
            }
        }
    }

    /**
     * Это второй этап инициализации - формирование треугольников из ИНДЕКСОВ вертексов базовой
     * сферы. Делим все треугольники на два набора ромбиков и по каждому набору отрабатываем.
     * Все треугольники формируются как counter clockwise.
     */
    private fun buildTriangleElements() {

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

    /**
     * INFO: В режиме Smooth мы накидываем новые, внутренние треугольники в хвост icoSphere,
     *  а потом "пересобираем" вообще все треугольники перестройкой индексов.
     *
     * Разделяем каждый треугольник на 4 треугольника. Параметр [level] содержит количество
     * рекурсивных проходов деления.
     */
    private fun subdivideSphere(level: Int) {
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

        subdivideSphere(level - 1)
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

        val pos = Vector(tmp.x * scaleFactor, tmp.y * scaleFactor, tmp.z * scaleFactor)
        val normal = pos.unit
        val tex = Tex((v1.tex.s + v2.tex.s) / 2, (v1.tex.t + v2.tex.t) / 2)
        return Vertex(pos.x, pos.y, pos.z, normal, tex)
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

        private const val S_STEP = 186 / 2048.0f;     // horizontal texture step
        private const val T_STEP = 322 / 1024.0f;     // vertical texture step

        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + TEX_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
}