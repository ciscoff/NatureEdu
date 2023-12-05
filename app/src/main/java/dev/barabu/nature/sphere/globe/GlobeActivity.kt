package dev.barabu.nature.sphere.globe

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.sphere.globe.gl.GlobeRenderer

class GlobeActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView

    private lateinit var renderer: GlobeRenderer

    private var isRendererSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderer = GlobeRenderer(this)

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