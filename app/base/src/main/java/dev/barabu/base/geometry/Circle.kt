package dev.barabu.base.geometry

class Circle(val center: Point, val radius: Float) {
    fun scale(factor: Float): Circle = Circle(center, radius * factor)
}