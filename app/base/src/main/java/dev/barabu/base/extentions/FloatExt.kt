package dev.barabu.base.extentions

import kotlin.math.PI

val Float.asString: String
    get() = "%.8f".format(this)

val Float.radians: Float
    get() = this * (PI.toFloat() / 180f)

val Float.degrees: Float
    get() = this * (180f / PI.toFloat())
