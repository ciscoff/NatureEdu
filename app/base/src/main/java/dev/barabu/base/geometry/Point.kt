package dev.barabu.base.geometry

class Point(val x: Float, val y: Float, val z: Float) {
    fun translateY(distance: Float): Point = Point(x, y + distance, z)

    fun translate(vector: Vector): Point = Point(x + vector.x, y + vector.y, z + vector.z)
}