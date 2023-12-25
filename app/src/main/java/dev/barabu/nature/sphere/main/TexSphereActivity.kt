package dev.barabu.nature.sphere.main

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.ViewGroup
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.BaseActivity
import dev.barabu.nature.sphere.main.gl.TexSphereRenderer

/**
 * Текстурированная сфера
 */
class TexSphereActivity : BaseActivity() {
    private lateinit var glSurfaceView: GLSurfaceView

    private lateinit var renderer: TexSphereRenderer

    private var isRendererSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderer = TexSphereRenderer(this)

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

    override fun onResume() {
        super.onResume()
        if (isRendererSet) {
            glSurfaceView.onResume()
        }
    }

    /**
     * Управляем thread'ом glSurfaceView
     */
    override fun onPause() {
        super.onPause()
        if (isRendererSet) {
            glSurfaceView.onPause()
        }
    }
}