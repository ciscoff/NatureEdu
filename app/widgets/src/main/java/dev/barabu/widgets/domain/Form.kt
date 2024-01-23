package dev.barabu.widgets.domain

enum class Form {
    Collapsed,
    Expanded
}

@JvmInline
value class FormWrapper(val value: Form? = null)