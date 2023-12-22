package dev.barabu.base.domain

class Attribute(val descriptor: Int, val type: Type) {
    enum class Type {
        Position,
        Color,
        Normal,
        Tex
    }
}