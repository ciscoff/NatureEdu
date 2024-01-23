package dev.barabu.widgets.domain

sealed class Effect {

    data object Colored : Effect()

    data object Grey : Effect()

    data object Blur : Effect()

    /*object OldTv : Effect()*/
}

@JvmInline
value class EffectWrapper(val value: Effect? = null)