package dev.barabu.base.extentions

import dev.barabu.base.geometry.Ray

/**
 * Строковое представление луча
 */
val Ray.asString: String
    get() = "r:[${point.asString}, ${vector.asString}]"