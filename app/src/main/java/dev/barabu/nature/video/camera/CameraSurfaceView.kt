package dev.barabu.nature.video.camera

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import dev.barabu.base.Logging
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.video.camera.domain.CameraWrapper
import dev.barabu.nature.video.camera.gl.CameraRenderer

@SuppressLint("ViewConstructor")
class CameraSurfaceView(context: Context, cameraWrapper: CameraWrapper) : GLSurfaceView(context) {

    private var cameraRenderer: CameraRenderer
    private var isRendererSet = false

    init {
        cameraRenderer = CameraRenderer(this, cameraWrapper)

        if (context.isActualGlEsSupporting) {
            setEGLContextClientVersion(3)

            setEGLConfigChooser(8, 8, 8, 8, 16, 0)

            setRenderer(cameraRenderer)
            renderMode = RENDERMODE_WHEN_DIRTY
            isRendererSet = true
        } else {
            Logging.d("This device does not support OpenGL ES 3.0.")
        }
    }
}