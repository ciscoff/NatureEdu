package dev.barabu.base.extentions

import dev.barabu.base.Logging
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector
import dev.barabu.base.math.clamp
import kotlin.math.acos

/**
 * Возвращает угол в РАДИАНАХ от 0 до PI
 */
fun Vector.angleBetween(other: Vector): Float {
    val dot = this.dotProduct(other)
    val len = this.length()
    val lenOther = other.length()
    if (len == 0f || lenOther == 0f) {
        return 0f
    }
    val cos = dot / (len * lenOther)
    return acos(clamp(cos, -1f, 1f)).also {
        Logging.d("    >> cos=${cos.asString}, angle=${it.asString}")
    }
}

val Vector.asPoint: Point
    get() = Point(x, y, z)

/**
 * Строковое представление вектора
 */
val Vector.asString: String
    get() {
        val xS = "%.3f".format(this.x)
        val yS = "%.3f".format(this.y)
        val zS = "%.3f".format(this.z)
        return "[$xS, $yS, $zS]"
    }