package dev.barabu.nature.camera.art.domain

import android.graphics.PointF
import dev.barabu.base.BYTES_PER_FLOAT
import dev.barabu.base.extentions.asString
import dev.barabu.base.utils.gaussian1D
import dev.barabu.base.utils.gaussian2D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Kernel - это квадратная решетка из ячеек. Сторона решетки состоит из НЕЧЕТНОГО
 * количества ячеек.
 *
 * [radius] - число ячеек от центральной до крайней не считая центральную ячейку.
 * Например, для квадрата 3x3 radius равен 1. То есть kernel-матрица имеет размер '2*r + 1',
 * где '+1' - это центральная ячейка.
 */
class BlurKernel(private val radius: Int, private val deviation: Float = 0.6f) {

    private val kernelSize = radius * 2 + 1

    private val array1D: FloatArray by lazy {
        FloatArray(kernelSize).apply {
            val step = (deviation * 6) / kernelSize

            for (i in 0 until kernelSize) {
                this[i] = gaussian1D(i * step, deviation)
            }
        }
    }

    /**
     * Параметром step надо играться в зависимости от размера kernel
     */
    private val array2D: FloatArray by lazy {
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

    override fun toString(): String {
        val sb = StringBuffer()

        for (i in 0 until kernelSize) {
            for (j in 0 until kernelSize) {
                sb.append("${array2D[i * kernelSize + j].asString} ")
            }
            sb.append("\n")
        }

        return sb.toString()
    }
}