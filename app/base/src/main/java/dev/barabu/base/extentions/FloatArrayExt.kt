package dev.barabu.base.extentions

/**
 * Строковое представление матрицы
 */
fun FloatArray.printM(title: String = ""): String {
    val stride = 4
    val rows = 4
    val cols = 4

    val sb = StringBuffer()

    sb.append("\n***********************\n*** $title\n")
    repeat(cols) { i ->
        repeat(rows) { j ->
            sb.append("%.3f ".format(this[i + j * stride]))
        }
        sb.append("\n")
    }
    return sb.toString() + "\n\n"
}

/**
 * Строковое представление вектора
 */
fun FloatArray.printV(title: String = ""): String {
    val cols = 4

    val sb = StringBuffer()
    sb.append("$title: [ ")
    repeat(cols) { i ->
        sb.append("%.3f ".format(this[i]))
    }
    return "$sb]\n\n"
}