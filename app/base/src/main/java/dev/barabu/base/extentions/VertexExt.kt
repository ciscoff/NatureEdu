package dev.barabu.base.extentions

import dev.barabu.base.domain.Vertex
import dev.barabu.base.geometry.Vector

val Vertex.asVector: Vector
    get() = Vector(x, y, z)

val Vertex.serialized: List<Float>
    get() = arrayListOf<Float>().apply {
        add(x)
        add(y)
        add(z)
        add(normal.x)
        add(normal.y)
        add(normal.z)
        add(tex.s)
        add(tex.t)

        if (color != null) {
            add(color.r)
            add(color.g)
            add(color.b)
        }
    }