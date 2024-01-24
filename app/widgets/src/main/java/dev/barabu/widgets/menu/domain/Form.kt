package dev.barabu.widgets.menu.domain

enum class Form {
    Collapsed,
    Expanded
}

@JvmInline
value class FormWrapper(val value: Form? = null)