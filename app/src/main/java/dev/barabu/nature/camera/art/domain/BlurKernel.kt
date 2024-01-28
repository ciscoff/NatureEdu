package dev.barabu.nature.camera.art.domain

import android.graphics.PointF
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.Logging
import dev.barabu.base.extentions.asString
import dev.barabu.base.utils.gaussian1D
import dev.barabu.base.utils.gaussian2D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Генератор kernel буфера.
 *
 * Kernel - это квадратная решетка из ячеек. Сторона решетки состоит из НЕЧЕТНОГО
 * количества ячеек.
 *
 * [radius] - число ячеек от центральной до крайней не считая центральную ячейку.
 * Например, для квадрата 3x3 radius равен 1. То есть kernel-матрица имеет размер '2*r + 1',
 * где '+1' - это центральная ячейка.
 * [deviation] - дисперсия, мера разброса величин от медианы. Чем меньше величина, тем ближе
 * значения функции ложатся к медиане. При deviation=1 получаем Стандартное нормальное распределение.
 *
 * В пределах 3-х единиц лежат 99.7% всех отклонений. Поэтому при расчете шага делим 3.
 */
class BlurKernel(private val radius: Int, private val deviation: Float = 1f) {

    private val array1D: FloatArray by lazy {
        val kernelSize = radius + 1
        FloatArray(kernelSize).apply {
            val step = 3f / kernelSize
            for (i in 0 until kernelSize) {
                this[i] = gaussian1D(i * step, deviation)
            }
        }
    }

    /**
     * Параметром step надо играться в зависимости от размера kernel
     */
    private val array2D: FloatArray by lazy {
        val kernelSize = radius * 2 + 1
        FloatArray(kernelSize * kernelSize).apply {
            val step = (deviation * 6) / kernelSize

            for (i in 0 until kernelSize) {
                val ii = i.toFloat() - radius

                for (j in 0 until kernelSize) {
                    val jj = j.toFloat() - radius

                    this[i * kernelSize + j] = gaussian2D(
                        PointF(ii * step, jj * step),
                        deviation
                    )
                }
            }
        }
    }

    fun gaussian1D(): FloatBuffer = toFloatBuffer(array1D)

    fun gaussian2D(): FloatBuffer = toFloatBuffer(array2D)

    private fun toFloatBuffer(array: FloatArray): FloatBuffer =
        ByteBuffer
            .allocateDirect(array.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(array)
                position(0)
            }

    fun toString1D(): String {
        val sb = StringBuffer()
        val kernelSize = radius + 1

        for (i in 0 until kernelSize) {
            sb.append("${array1D[i].asString} ")
        }

        sb.append("\n")

        return sb.toString()
    }

    fun toString2D(): String {
        val sb = StringBuffer()
        val kernelSize = radius * 2 + 1

        for (i in 0 until kernelSize) {
            for (j in 0 until kernelSize) {
                sb.append("${array2D[i * kernelSize + j].asString} ")
            }
            sb.append("\n")
        }

        return sb.toString()
    }
}