package dev.barabu.nature.icosphere

inline fun <reified T> Sequence<T>.toArray(size: Int): Array<T> {
    val iterator = iterator()
    return Array(size) { iterator.next() }
}

/**
 * Массив из 12 вертексов
 */
private val icosahedron = Array(12) { i -> "V${"%02d".format(i)}" }

/**
 * Сфера на базе Icosahedron.
 * Это массив вертексов для 20 треугольников. Каждый треугольник - 3 вертекса, всего 60 вертексов.
 * Все треугольники идут последовательно, каждый вертекс участвует только в одном треугольнике,
 * поэтому индексы вертексов можно сразу использовать для рисования. Все треугольники clockwise.
 */
private val sphere: Array<String> by lazy {
    Array(60) { "" }.also { arr ->
        var index = 0

        //    00  00  00  00  00
        //    /\  /\  /\  /\  /\
        //   /  \/  \/  \/  \/  \
        //  01--02--03--04--05--01  <-- upDiamonds
        //   \  /\  /\  /\  /\  /
        //    \/  \/  \/  \/  \/
        //    06  07  08  09  10
        val upDiamonds = IntArray(5) { it + 1 } + 1

        repeat(upDiamonds.size - 1) { i ->
            arr[index++] = icosahedron[upDiamonds[i]]
            arr[index++] = icosahedron[upDiamonds[i + 1]]
            arr[index++] = icosahedron[0]

            arr[index++] = icosahedron[upDiamonds[i]]
            arr[index++] = icosahedron[upDiamonds[i] + 5]
            arr[index++] = icosahedron[upDiamonds[i + 1]]
        }

        //      02  03  04  05  01    <-- WOW (не прокатит lowDiamonds[i] - 4)
        //      /\  /\  /\  /\  /\
        //     /  \/  \/  \/  \/  \
        //    06--07--08--09--10--06  <-- lowDiamonds
        //     \  /\  /\  /\  /\  /
        //      \/  \/  \/  \/  \/
        //      11  11  11  11  11
        val lowDiamonds = IntArray(5) { it + 6 } + 6
        repeat(lowDiamonds.size - 1) { i ->
            arr[index++] = icosahedron[lowDiamonds[i]]
            arr[index++] = icosahedron[lowDiamonds[i + 1]]
            arr[index++] = icosahedron[if(i == 4) (lowDiamonds[i] - 9) else (lowDiamonds[i] - 4)]

            arr[index++] = icosahedron[lowDiamonds[i]]
            arr[index++] = icosahedron[11]
            arr[index++] = icosahedron[lowDiamonds[i + 1]]
        }
    }
}

/**
 * Вручную расставляем вершины внутреннего треугольника, но это может приводить к ошибке,
 * потому что мы не знаем между каких вершин внешнего треугольника лежит вершина внутреннего.
 * Поэтому сюда надо передавать заранее подготовленные треугольники. Например (V00, V01, V02)
 * и (v.00.01, v.01.02, v.02.00). То есть оба clockwise и первая вершина внутреннего
 * треугольника лежит между первой и второй вершинами внешнего треугольника.
 *
 *             V00
 *             / \
 *    v.00.01 *---* v.02.00
 *           / \ / \
 *      v01 *---*---* V02
 *           v.01.02
 */
private fun zipTriangles(outer: Array<String>, inner: Array<String>): Array<String> =
    arrayOf(outer[0], inner[0], inner[2]) +
            arrayOf(outer[1], inner[1], inner[0]) +
            arrayOf(outer[2], inner[2], inner[1]) +
            inner

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
private fun getInnerTriangle(triangle: Array<String>): Array<String> {
    return (triangle + triangle[0]).asSequence().zipWithNext().mapIndexed { i, p ->
        "v.${p.first.drop(1)}.${p.second.drop(1)}"
    }.toArray(3)
}

/**
 * Тестируем разбиение треугольника на 4 новых треугольника.
 */
private fun subdivideTriangle() {
    val outerTriangle = Array(3) { i -> "V${"%02d".format(i)}" }
    val innerTriangle = getInnerTriangle(outerTriangle)
    zipTriangles(outerTriangle, innerTriangle).forEachIndexed { i, s ->
        if (i % 3 == 0) print("\n$s ") else print("$s ")
    }
}

fun main() {

    /** Группируем вертексы по 3, то есть по треугольникам. */
    sphere.asSequence()
        .windowed(3, 3)
        .forEach { vertices ->
            val outerTriangle = vertices.toTypedArray()
            val innerTriangle = getInnerTriangle(outerTriangle)
            zipTriangles(outerTriangle, innerTriangle).forEachIndexed { i, s ->
                if (i % 3 == 0) print("\n$s ") else print("$s ")
            }
        }
}