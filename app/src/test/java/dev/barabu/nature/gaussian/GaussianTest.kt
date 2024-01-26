package dev.barabu.nature.gaussian

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * m - медиана, вокруг которой формируется рассеивание значений
 * d - deviation, показатель рассеивания значений. Чем меньше величина, тем ближе график
 *     стягивается в медиане.
 *
 * ref: https://en.wikipedia.org/wiki/Normal_distribution
 * ref: https://en.wikipedia.org/wiki/Normal_distribution#/media/File:Normal_Distribution_PDF.svg
 *
 * Правило трёх сигм для выбора значения 'd':
 *   Все значения нормально распределённой величины лежат в интервале 6d.
 *   То есть если у нас d=0.6, то нам нужно делать выборку в диапазоне [0, 3.6]

 */
fun gaussian1D(x: Float, d: Float, m: Float = 0f): Float {
    val p = -0.5 * ((x - m) / d) * ((x - m) / d)
    return ((1f / (d * sqrt(2 * PI))) * E.pow(p)).toFloat()
}

fun gaussian2D(st: PointF, d: Float): Float {
    val p = -0.5 * (st.x * st.x + st.y * st.y) / (d * d)
    return (1.0 / (2 * PI * d * d) * E.pow(p)).toFloat()
}

class PointF(val x: Float, val y: Float)

class BlurKernel(private val radius: Int, private val deviation: Float = 0.6f) {
    private val kernelSize = radius * 2 + 1

    fun weight1D(): FloatArray {
        val weight = FloatArray(kernelSize)
        val step = (deviation * 6) / kernelSize

        for (i in 0 until kernelSize) {
            weight[i] = dev.barabu.base.utils.gaussian1D(i * step, deviation)
        }

        return weight
    }

    fun weight2D(): FloatArray {
        val weight = FloatArray(kernelSize * kernelSize)

        val step = (deviation * 6) / kernelSize

        for (i in 0 until kernelSize) {
            val ii = i.toFloat() - radius

            for (j in 0 until kernelSize) {
                val jj = j.toFloat() - radius

                weight[i * kernelSize + j] = gaussian2D(
                    PointF(ii * step, jj * step),
                    deviation
                )
            }
        }

        return weight
    }
}

val Float.asString: String
    get() = "%.8f".format(this)

const val KERNEL_SIZE = 5
val weight = FloatArray(KERNEL_SIZE * KERNEL_SIZE)


fun weight1D() {

    val d = 0.6f
    val step = (d * 6) / weight.size

    for (i in weight.indices) {
        weight[i] = gaussian1D(i * step, d)
    }
}

fun weight2D() {

    val d = 0.6f
    val step = (d * 6) / KERNEL_SIZE

    for (i in 0 until KERNEL_SIZE) {
        val ii = i.toFloat() - (KERNEL_SIZE / 2)

        for (j in 0 until KERNEL_SIZE) {
            val jj = j.toFloat() - (KERNEL_SIZE / 2)

            weight[i * KERNEL_SIZE + j] = gaussian2D(PointF(ii * step, jj * step), d)
        }
    }
}


fun main() {

    weight2D()
    for (i in 0 until KERNEL_SIZE) {
        for (j in 0 until KERNEL_SIZE) {
            print("${weight[i * KERNEL_SIZE + j].asString} ")
        }
        println()
    }

    println(" *** ")

    val radius = 2
    val kernelSize = 2 * radius + 1

    val kernel = BlurKernel(radius).weight2D()
    for (i in 0 until kernelSize) {
        for (j in 0 until kernelSize) {
            print("${kernel[i * kernelSize + j].asString} ")
        }
        println()
    }
}
