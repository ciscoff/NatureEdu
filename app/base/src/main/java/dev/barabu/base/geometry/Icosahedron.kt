package dev.barabu.base.geometry

import dev.barabu.base.domain.Vertex
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * ref: https://www.songho.ca/opengl/gl_sphere.html
 */
class Icosahedron(val radius: Float) {

    /**
     * 12 вертексов: 2 вертекса на полюсах и по 5 вертексов на двух широтах.
     */
    val vertices: Array<Vertex> by lazy {

        Array(VERTICES_COUNT) { _ -> Vertex(0f, 0f, 0f) }.also { buffer ->
            var i1: Int
            var i2: Int

            // Обход верхней широты начнем с угла -126 deg
            var hAngle1 = (-PI / 2 - H_ANGLE / 2).toFloat()
            // Обход нижней широты начнем с угла -90 deg
            var hAngle2 = (-PI / 2).toFloat()

            // Первый вертекс - северный полюс
            buffer[0] = Vertex(0f, radius, 0f)

            // Еще по 5 вертексов на двух широтах
            (1..5).forEach { i ->
                // Первая широта
                i1 = i
                // Вторая широта
                i2 = (i + 5)

                val y = radius * sin(V_ANGLE)
                val xz = radius * cos(V_ANGLE)

                // Будем одновременно двигаться по верхней и нижней широте
                buffer[i1] = Vertex(xz * cos(hAngle1), y, xz * sin(hAngle1))
                buffer[i2] = Vertex(xz * cos(hAngle2), -y, xz * sin(hAngle2))

                hAngle1 += H_ANGLE
                hAngle2 += H_ANGLE
            }

            // Последний вертекс - южный полюс
            buffer[11] = Vertex(0f, -radius, 0f)
        }
    }

    companion object {
        // Всего вертексов в базовом Icosahedron'е
        const val VERTICES_COUNT = 12

        // Шаг долготы 72 градуса
        private const val H_ANGLE = PI.toFloat() / 180 * 72

        // Широта, на которой живут 5 вертексов
        private val V_ANGLE = atan(0.5f)
    }
}