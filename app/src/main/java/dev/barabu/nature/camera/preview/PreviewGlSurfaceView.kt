package dev.barabu.nature.camera.preview

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.camera.preview.domain.CameraWrapper
import dev.barabu.nature.camera.preview.gl.PreviewRenderer

@SuppressLint("ViewConstructor")
class PreviewGlSurfaceView(context: Context, cameraWrapper: CameraWrapper) : GLSurfaceView(context) {

    private var renderer: PreviewRenderer
    private var isRendererSet = false

    init {
        renderer = PreviewRenderer(this, cameraWrapper)

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