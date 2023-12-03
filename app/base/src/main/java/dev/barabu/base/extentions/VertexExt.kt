package dev.barabu.base.extentions

import dev.barabu.base.domain.Vertex
import dev.barabu.base.geometry.Vector

val Vertex.asVector: Vector
    get() = Vector(x, y, z)