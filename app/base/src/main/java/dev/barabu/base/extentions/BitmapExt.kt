package dev.barabu.base.extentions

import android.graphics.Bitmap
import android.graphics.Color
import dev.barabu.base.POSITION_COMPONENT_COUNT

/**
 * Конвертирует битмапу в массив вертексов. Каждый пиксель битмапы превращается в отдельный
 * вертекс. 2D координата каждого пикселя превращается в 3D координату, где X и Z живут в
 * диапазоне (-0.5, 0.5). А значение Y - в диапазоне (0, 1) и представляет собой нормализованный
 * компонент красного цвета пикселя битмапы.
 */

fun Bitmap.toVertexData(): FloatArray {
    val (w, h) = width to height

    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)
    recycle()

    val heightmapVertices = FloatArray(w * h * POSITION_COMPONENT_COUNT)
    var offset = 0

    // Квадрат 2x2 на плоскости XZ
    // Рельеф высотой от 0 до 1 по оси Y
    repeat(h) { row ->
        repeat(w) { col ->
            val posX = (col.toFloat() / (w - 1)) - 0.5f
            val posY = Color.red(pixels[(row * w) + col]) / 255f
            val posZ = (row.toFloat() / (h - 1)) - 0.5f
            heightmapVertices[offset++] = posX
            heightmapVertices[offset++] = posY
            heightmapVertices[offset++] = posZ
        }
    }
    return heightmapVertices
}


/**
 * Генерит "решетку" индексов и сериализует её в 1D массив.
 * Например, ниже показана битмапа размером 4x3 (WxH). Звездочками показаны пиксели, которые
 * образуют одну ячейку решетки. Их битмапные индексы будут записаны в массив indexData
 * как [0, 1, 4, 5, ....]
 *
 *        * * . .
 *        * * . .
 *        . . . .
 */
fun Bitmap.toElements(numElements: Int): IntArray {

    val indexData = IntArray(numElements)
    var offset = 0

    repeat(height - 1) { row ->
        repeat(width - 1) { col ->

            val topLeftIndexNum = (row * width + col)
            val topRightIndexNum = (row * width + col + 1)
            val bottomLeftIndexNum = ((row + 1) * width + col)
            val bottomRightIndexNum = ((row + 1) * width + col + 1)

            indexData[offset++] = topLeftIndexNum
            indexData[offset++] = bottomLeftIndexNum
            indexData[offset++] = topRightIndexNum
            indexData[offset++] = topRightIndexNum
            indexData[offset++] = bottomLeftIndexNum
            indexData[offset++] = bottomRightIndexNum
        }
    }
    return indexData
}