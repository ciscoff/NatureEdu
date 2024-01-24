package dev.barabu.widgets.domain

enum class Effect {
    Colored, Grayscale, Blur/*, OldTv*/
}

@JvmInline
value class EffectWrapper(val value: Effect? = null)