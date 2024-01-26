package dev.barabu.base.utils

import android.graphics.PointF
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