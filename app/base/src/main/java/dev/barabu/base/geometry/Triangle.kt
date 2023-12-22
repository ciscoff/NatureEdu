package dev.barabu.base.geometry

import dev.barabu.base.domain.Vertex

class Triangle(val v0: Vertex, val v1: Vertex, val v2: Vertex) {
    constructor(vertices: Array<Vertex>) : this(vertices[0], vertices[1], vertices[2])

    val asArray: Array<Vertex>
        get() = arrayOf(v0, v1, v2)

    val asFloatArray: FloatArray by lazy {
        floatArrayOf(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z)
    }
}