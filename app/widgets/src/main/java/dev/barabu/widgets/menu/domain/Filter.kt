package dev.barabu.widgets.menu.domain

enum class Filter {
    Colored, Grayscale, Invert, Blur/*, OldTv*/
}

@JvmInline
value class FilterWrapper(val value: Filter? = null)