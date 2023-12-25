package dev.barabu.base.geometry

import kotlin.math.sqrt

class Vector(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun div(divider: Float): Vector = Vector(x / divider, y / divider, z / divider)

    operator fun minus(other: Vector): Vector = Vector(x - other.x, y - other.y, z - other.z)

    operator fun plus(other: Vector): Vector = Vector(x + other.x, y + other.y, z + other.z)

    val r: Float get() = x

    val g: Float get() = y

    val b: Float get() = z

    val a: Float get() = 1f

    val unit: Vector
        get() = this / length()

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun scale(factor: Float): Vector = Vector(x * factor, y * factor, z * factor)

    /**
     * Длина вектора crossProduct равна площади параллелограмма, который образуют
     * векторы this и other.
     *
     * А площадь параллелограмма - это две площади треугольника, который образуют
     * векторы this и other.
     *
     * То есть можно вычислить площадь треугольника разделив crossProduct/2f
     *
     * INFO: Операция некоммутативна, то есть a × b = − b × a
     */
    fun crossProduct(other: Vector): Vector =
        Vector(
            ((y * other.z) - (z * other.y)).takeIf { it != -0.0f } ?: 0.0f,
            ((z * other.x) - (x * other.z)).takeIf { it != -0.0f } ?: 0.0f,
            ((x * other.y) - (y * other.x)).takeIf { it != -0.0f } ?: 0.0f,
        )

    fun dotProduct(other: Vector): Float = x * other.x + y * other.y + z * other.z
}