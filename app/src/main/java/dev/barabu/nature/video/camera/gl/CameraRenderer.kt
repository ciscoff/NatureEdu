package dev.barabu.nature.video.camera.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.nature.video.camera.domain.CamInfo
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(
    private val glSurfaceView: GLSurfaceView,
    private val camInfo: CamInfo,
    private val context: Context,
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private var previewTexDescriptor: Int = INVALID_DESCRIPTOR
    private var isSurfaceUpdated = false

    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    /** https://source.android.com/docs/core/graphics/arch-st?hl=en */
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var program: CameraProgram

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0f, 1.0f, 0f, 1.0f)
        program = CameraProgram(context)
        setupOffScreenTexture()
        camInfo.openCamera(surfaceTexture)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        //  stMatrix (https://stackoverflow.com/a/30635539)
        //   0  -1  0  1
        //  -1   0  0  1
        //   0   0  1  0
        //   0   0  0  1
        synchronized(this) {
            if (isSurfaceUpdated) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(stMatrix)
                isSurfaceUpdated = false

                // NOTE: Здесь нельзя ставить drawVideo(). Будет моргать.
            }
        }

        drawPreview()
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {
        synchronized(this) {
            isSurfaceUpdated = true
            glSurfaceView.requestRender()
        }
    }

    private fun drawPreview() {
        Matrix.setIdentityM(mvpMatrix, 0)

        program.apply {
            useProgram()
            bindVideoTexUniform(previewTexDescriptor)
            bindStMatrixUniform(stMatrix)
            bindMvpMatrixUniform(mvpMatrix)
            draw()
        }
    }

    private fun setupOffScreenTexture() {
        val descriptor = IntArray(1)
        GLES20.glGenTextures(1, descriptor, 0)

        if (descriptor[0] == ERROR_CODE) throw RuntimeException("glGenTextures failed")

        previewTexDescriptor = descriptor[0]
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, previewTexDescriptor)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(previewTexDescriptor)
        surfaceTexture.setOnFrameAvailableListener(this)
    }
}