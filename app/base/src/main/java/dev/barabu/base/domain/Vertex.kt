package dev.barabu.base.domain

import dev.barabu.base.geometry.Vector

class Vertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val color: Vector? = null,
    val normal: Vector? = null,
    val tex: Tex? = null
)

