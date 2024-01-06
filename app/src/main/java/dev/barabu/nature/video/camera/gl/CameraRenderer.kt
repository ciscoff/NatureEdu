package dev.barabu.nature.video.camera.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
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
import android.opengl.Matrix.orthoM
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.nature.video.camera.domain.CameraWrapper
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(
    private val glSurfaceView: GLSurfaceView,
    private val cameraWrapper: CameraWrapper,
    private val context: Context,
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private var previewTexDescriptor: Int = INVALID_DESCRIPTOR
    private var isSurfaceUpdated = false

    private val stMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)

    /**
     * Рисовать "голый" кадр с камеры или учесть ориентацию и прочую хуйню
     */
    private val isDrawingRawPreview: Boolean = false

    /** https://source.android.com/docs/core/graphics/arch-st?hl=en */
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var program: CameraProgram

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0f, 1.0f, 0f, 1.0f)
        program = CameraProgram(context)
        setupOffScreenTexture()
        cameraWrapper.openCamera(
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager,
            surfaceTexture
        )
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        updateOrthographicProjectionMatrix(width.toFloat(), height.toFloat())

        // NOTE: Благодаря этому пропали последние незначительные искажения,
        //  которые оставались после работы ортогональной проекции.
        surfaceTexture.setDefaultBufferSize(width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        //  stMatrix при портретной ориентации девайса
        //
        //   0  -1  0  1
        //  -1   0  0  1
        //   0   0  1  0
        //   0   0  0  1
        synchronized(this) {
            if (isSurfaceUpdated) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(stMatrix)
                isSurfaceUpdated = false

                // NOTE: SurfaceTexture.getTransformMatrix вернет матрицу, которая позволит
                //  правильно забирать ST из текстуры GL_TEXTURE_EXTERNAL_OES. Кадр, приходящий
                //  в текстуру от сенсора камеры, требует дополнительной обработки. И матрица
                //  избавляет от ручных расчетов, которые есть в https://habr.com/ru/articles/480878

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

        program.apply {
            useProgram()
            bindVideoTexUniform(previewTexDescriptor)

            if (isDrawingRawPreview) {
                Matrix.setIdentityM(stMatrix, 0)
                Matrix.setIdentityM(projMatrix, 0)
            }

            bindStMatrixUniform(stMatrix)
            bindMvpMatrixUniform(projMatrix)
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

    /**
     * Алгоритм такой:
     * - Имеем ориентацию Portrait на экране W/H 720/1280
     * - Оставляем X-координаты вертексов равными -1 и 1
     * - Скалируем Y-координаты вертексов на величину H/W (1280/720=1.78)
     * То есть захватываем из мира прямоугольник [-1.0, -1.78] - [1.0, 1,78]. Ортогональная
     * проекция упакует прямоугольник в стандартный квадрат [-1, -1] - [1, 1], то сплющит
     * картинку по вертикали.
     */
    private fun updateOrthographicProjectionMatrix(width: Float, height: Float) {
        val ratio = if (width > height) width / height else height / width
        Logging.d("WOWOW width=$width, height=$height, ratio=$ratio")

        if (width > height) {
            orthoM(projMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f)
        } else {
            orthoM(projMatrix, 0, -1f, 1f, -ratio, ratio, -1f, 1f)
        }
    }
}