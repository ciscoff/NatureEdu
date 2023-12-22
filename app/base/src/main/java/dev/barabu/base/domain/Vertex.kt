package dev.barabu.base.domain

import dev.barabu.base.geometry.Vector

data class Vertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val normal: Vector = Vector(),
    val tex: Tex = Tex(0f, 0f, 0f),
    val color: Vector? = null,
)

