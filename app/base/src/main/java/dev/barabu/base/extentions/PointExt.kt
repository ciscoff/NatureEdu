package dev.barabu.base.extentions

import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Vector

val Point.asVector: Vector
    get() = Vector(x, y, z)

/**
 * Строковое представление точки
 */
val Point.asString: String
    get() {
        val xS = "%.3f".format(this.x)
        val yS = "%.3f".format(this.y)
        val zS = "%.3f".format(this.z)
        return "p:[ $xS, $yS, $zS]"
    }

fun vectorBetween(from: Point, to: Point): Vector =
    Vector(to.x - from.x, to.y - from.y, to.z - from.z)