package dev.barabu.nature.camera.art

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.camera.art.domain.Camera
import dev.barabu.nature.camera.art.gl.ArtRenderer

@SuppressLint("ViewConstructor")
class ArtGLSurfaceView(
    context: Context,
    cameras: Map<Int, Camera>
) : GLSurfaceView(context) {

    private var renderer: ArtRenderer
    private var isRendererSet = false

    init {
        renderer = ArtRenderer(this, cameras)

        if (context.isActualGlEsSupporting) {
            setEGLContextClientVersion(3)

            setEGLConfigChooser(8, 8, 8, 8, 16, 0)

            setRenderer(renderer)
            renderMode = RENDERMODE_WHEN_DIRTY
            isRendererSet = true
        } else {
            Logging.d("This device does not support OpenGL ES 3.0.")
        }
    }
}