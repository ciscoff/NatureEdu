package dev.barabu.base.domain

import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_VERTEX_SHADER

enum class Shader(val type: Int, val desc: String) {
    Vertex(GL_VERTEX_SHADER, "vertex"),
    Fragment(GL_FRAGMENT_SHADER, "fragment")
}