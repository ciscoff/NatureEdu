package dev.barabu.base.utils

import java.io.BufferedReader
import java.io.InputStreamReader

fun getSystemProperty(propName: String): String? = runCatching {
    val process = Runtime.getRuntime().exec("getprop $propName")
    BufferedReader(InputStreamReader(process.inputStream), 1024).use {
        it.readLine()
    }
}.getOrNull()