package dev.barabu.nature.mountains

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.mountains.gl.MountainsRenderer

class MountainsActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView

    private lateinit var renderer: MountainsRenderer

    private var isRendererSet = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderer = MountainsRenderer(this)

        glSurfaceView = GLSurfaceView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            if (isActualGlEsSupporting) {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                isRendererSet = true
            } else {
                Logging.d("This device does not support OpenGL ES 3.0.")
            }
        }

        setContentView(glSurfaceView)
    }
}