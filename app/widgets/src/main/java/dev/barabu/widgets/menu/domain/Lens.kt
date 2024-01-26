package dev.barabu.widgets.menu.domain

enum class Lens {
    Back, Front
}

@JvmInline
value class LensWrapper(val value: Lens? = null)