package dev.barabu.base.geometry

/**
 * Луч, представлен как точка основания и вектор направления.
 */
class Ray(val point: Point, val vector: Vector) {

    val farPoint: Point
        get() = point.translate(vector)
}