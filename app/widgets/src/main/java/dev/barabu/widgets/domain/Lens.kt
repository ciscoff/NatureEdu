package dev.barabu.widgets.domain

enum class Lens {
    Back, Front
}

@JvmInline
value class LensWrapper(val value: Lens? = null)