package dev.barabu.base.utils

/**
 * ref:
 * https://gist.github.com/Muyangmin/e8ec1002c930d8df3df46b306d03315d
 */
val isMiUi: Boolean = !getSystemProperty("ro.miui.ui.version.name").isNullOrEmpty()