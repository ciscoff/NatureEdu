package dev.barabu.widgets.domain

sealed class Effect {

    object Colored : Effect()

    object Grey : Effect()

    object Blur : Effect()

    /*object OldTv : Effect()*/
}