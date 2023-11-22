package dev.barabu.base.math

import dev.barabu.base.geometry.Plane
import dev.barabu.base.geometry.Point
import dev.barabu.base.geometry.Ray
import dev.barabu.base.geometry.Sphere
import dev.barabu.base.geometry.Vector
import kotlin.math.max
import kotlin.math.min

fun vectorBetween(from: Point, to: Point): Vector =
    Vector(to.x - from.x, to.y - from.y, to.z - from.z)

/**
 * Идея такая:
 * Имеем три точки: point, начало луча (ray.point), конец луча (ray.farPoint).
 * Точки образуют треугольник и нам нужно найти его высоту в точке point.
 */
fun distanceBetween(ray: Ray, point: Point): Float {
    val nearToPoint = vectorBetween(ray.point, point)
    val farToPoint = vectorBetween(ray.farPoint, point)

    val areaOfParallelogram = nearToPoint.crossProduct(farToPoint).length()
    val lengthOfBase = ray.vector.length()

    // Высота треугольника к любому основанию равна "площадь параллелограмма поделить на длину
    // этого основания"
    return areaOfParallelogram / lengthOfBase
}

fun divideByW(vector: FloatArray) {
    if (vector.size == 4 && vector[3] != 0f) {
        vector[0] /= vector[3]
        vector[1] /= vector[3]
        vector[2] /= vector[3]
    }
}

/**
 * Пересечение сферы лучом
 */
fun intersect(ray: Ray, sphere: Sphere): Boolean =
    distanceBetween(ray, sphere.center) < sphere.radius

/**
 * Принцип нахождения точки пересечения луча и плоскости рассмотрен в разделе "Algebraic form".
 * @see <a href="https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection">тут</a><br>
 *
 * Как мы помним уравнение прямой в нотации декартовых координат выглядит так: Y= a*X + b
 * В векторной нотации уравнение прямой выглядит так: P = P' + U*s, где P' - это точка
 * этой прямой, а U - вектор направления этой прямой (тогда любую точку прямой можно найти как сумму
 * P' и скалированного вектора U. s - это скаляр). А уравнение плоскости в векторной нотации
 * можно представить так: (P - Po) dot (N) = 0, где Po - это любая точка на плоскости, а n - это
 * normal вектор. То есть dot произведение двух перпендикулярных векторов равно 0.
 *
 * Идея такая - пусть луч пересекается с плоскостью в некой точке P. Тогда справедливо условие:
 * (P - Po) dot (N) = 0
 *
 * Уравнение луча (прямой) в векторной нотации P = P' + U*s, где точка P' есть основание луча.
 * Подставляем P в уравнение плоскости: (P' + U*s - Po) dot (N) = 0
 * (U*s)dot(N) + (P' - Po)dot(N) = 0
 * (U*s)dot(N) = -(P' - Po)dot(N)
 * s * ((U)dot(N)) = -(P' - Po)dot(N)
 * s = -((Po - P')dot(N) / ((U)dot(N)))
 * Если (U)dot(N) != 0, то луч не параллелен плоскости и мы можем вычислять s, а потом и P.
 *
 * NOTE: Не забываем про знак (-) при вычислении скаляра !!!
 */
fun intersectionPoint(ray: Ray, plane: Plane): Point {

    /** s = (-1) * ((P' - Po)dot(N) / ((U)dot(N))) */
    val factor = -(
            vectorBetween(
                from = plane.point, // Po
                to = ray.point      // P'
            ).dotProduct(plane.normal) / ray.vector.dotProduct(plane.normal))

    return ray.point.translate(ray.vector.scale(factor))
}

fun clamp(value: Float, min: Float, max: Float): Float = min(max, max(min, value))