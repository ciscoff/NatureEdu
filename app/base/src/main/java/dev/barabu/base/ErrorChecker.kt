package dev.barabu.base

import android.opengl.GLES20

fun checkGlError(op: String) {
    val error = GLES20.glGetError()

    if (error != GLES20.GL_NO_ERROR) {
        val message = "$op: glError 0x${Integer.toHexString(error)}"
        throw RuntimeException(message)
    }
}