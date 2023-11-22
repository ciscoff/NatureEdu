package dev.barabu.base

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object TextResourceReader {

    fun readTexFromResource(context: Context, resourceId: Int): String {
        val body = StringBuilder()

        kotlin.runCatching {
            // Байтовый поток
            val inputStream = context.resources.openRawResource(resourceId)

            // Символьный потом
            val inputReader = InputStreamReader(inputStream)

            // Читаем строки
            val bufferedReader = BufferedReader(inputReader)

            while (bufferedReader.readLine()?.also { nextLine ->
                    body.append("$nextLine\n")
                } != null) {
            }

        }.onFailure { t ->
            Logging.e(t)
            throw java.lang.RuntimeException("Fuck off")
        }

        return body.toString()
    }
}