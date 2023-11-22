package dev.barabu.base

import android.os.Process
import android.util.Log

object Logging {

    private const val INVOKE_SUSPEND = "invokeSuspend"
    private const val LOGGER = "Logging.kt"
    private const val LOG_TAG = "BASE_GL"
    private const val EMPTY_STRING = ""
    private const val STUB = "stub"

    fun d(message: String, tag: String = LOG_TAG) {
        val throwable = Throwable(STUB)
        val suffix = throwable.stackTrace.firstOrNull { element ->
            !element.toString().contains(STUB) && !element.toString().contains(LOGGER)
        }?.let(::parseStackEntry)

        Log.d("$tag:${Process.myPid()}$suffix", message)
    }

    fun e(throwable: Throwable, tag: String = LOG_TAG, message: String = EMPTY_STRING) {
        Log.e("$tag:${Process.myPid()}", message, throwable)
    }

    fun e(error: String, tag: String = LOG_TAG, message: String = EMPTY_STRING) {
        e(Throwable(error), tag, message)
    }

    /**
     * Получить строку вида 'MainActivity.onCreate:65'
     */
    private fun parseStackEntry(element: StackTraceElement): String {

        // Если логируем внутри билдера корутины launch/async
        parseIfSuspendedCallEntry(element)?.let {
            val (className, methodName) = it
            return " ${className.substringAfterLast(".")}.$methodName"
        }

        val className = element.className.substringAfterLast(".")
        val methodName = element.methodName
        val lineNum = element.lineNumber
        return " $className.$methodName:$lineNum"
    }

    private fun parseIfSuspendedCallEntry(element: StackTraceElement): Pair<String, String>? {
        if (element.className.contains(INVOKE_SUSPEND).not()) return null
        val chunks = element.className.split("""$""")
        return if (chunks.size > 2) {
            chunks[0] to chunks[1]
        } else {
            null
        }
    }
}