package dev.barabu.nature.sphere.planet

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.R
import dev.barabu.nature.sphere.ico.gl.IcoSphereRenderer
import dev.barabu.nature.sphere.planet.gl.PlanetRenderer

class PlanetActivity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLSurfaceView

    private lateinit var renderer: PlanetRenderer

    private var isRendererSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderer = PlanetRenderer(this)

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