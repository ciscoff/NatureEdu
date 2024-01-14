package dev.barabu.nature.video.camera.gl

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
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
import android.view.Surface
import dev.barabu.base.ERROR_CODE
import dev.barabu.base.INVALID_DESCRIPTOR
import dev.barabu.base.Logging
import dev.barabu.nature.video.camera.domain.CameraWrapper
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(
    private val glSurfaceView: GLSurfaceView,
    private val cameraWrapper: CameraWrapper,
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private var previewTexDescriptor: Int = INVALID_DESCRIPTOR
    private var isSurfaceUpdated = false
    private val context = glSurfaceView.context

    private val stMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    /**
     * Counter-clockwise угол поворота девайса от своего натурального положения.
     */
    private val displayRotation: Int
        get() = DISPLAY_ROTATIONS[(context as Activity).windowManager.defaultDisplay.rotation]!!

    /** https://source.android.com/docs/core/graphics/arch-st?hl=en */
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var program: CameraProgram

    /**
     * INFO: не понятно какая именно Surface created, потому что мы сами создаем Surface,
     *  нацеливаем на OES текстуру и передаем в камеру. Скорее всего метод сигнализирует о создании
     *  потока для Renderer.
     */
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        Logging.d("$TAG.onSurfaceCreated")
        GLES20.glClearColor(0f, 1.0f, 0f, 1.0f)
        program = CameraProgram(context)
        setupOffScreenGlTexture()
        cameraWrapper.openCamera(
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager,
            surfaceTexture
        )
    }

    /**
     * Метод вызывается при повороте экрана.
     */
    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        Logging.d("$TAG.onSurfaceChanged")
        GLES20.glViewport(0, 0, width, height)

        updateModelMatrix(width.toFloat(), height.toFloat())
        updateSurfaceBufferSize(width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        //  stMatrix в Portrait и Landscape ориентации девайса одинаковы.
        //  То есть матрица учитывает только ориентацию сенсора, но НЕ учитывает поворот девайса.
        //
        //   0  -1   0   1
        //  -1   0   0   1
        //   0   0   1   0
        //   0   0   0   1
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
            bindStMatrixUniform(stMatrix)
            bindMvpMatrixUniform(modelMatrix)
            draw()
        }
    }

    private fun setupOffScreenGlTexture() {
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
     * references:
     *   https://github.com/tomoima525/cameraLayout
     *   https://developer.android.com/reference/android/graphics/SurfaceTexture#setDefaultBufferSize(int,%20int)
     *     "The new default buffer size will take effect the next time the image producer
     *     requests a buffer to fill. ...... For OpenGL ES, the EGLSurface should be destroyed
     *     (via eglDestroySurface), made not-current (via eglMakeCurrent), and then recreated
     *     (via eglCreateWindowSurface) to ensure that the new default size has taken effect."
     * NOTE: То есть в нашем случае setDefaultBufferSize срабатывает только один раз - первый!
     *
     *  INFO: Такой swap размеров позволяет корректно работать в любой ориентации. Проблема с
     *   ориентацией обнаружилась при старте активити в положении Landscape и была исправлена
     *   добавлением ветки Configuration.ORIENTATION_LANDSCAPE.
     *
     *  Не очень понятно зачем это делать, но получается, что всякий раз устанавливается
     *  размер "вертикального" буфера: ширина меньше высоты. В интернетах есть примеры с методом
     *  [dev.barabu.nature.video.camera.domain.CameraWrapper.isDimensionSwapped].
     *  Он фактически отдает true при портретной ориентации телефона и false при горизонтальной.
     *  Можно конечно использовать эти результаты для swap, но я просто проверяю
     *  context.resources.configuration.orientation
     *
     *  ref:
     *  https://github.com/tomoima525/cameraLayout?tab=readme-ov-file#step-1-check-the-dimension-rotation
     */
    private fun updateSurfaceBufferSize(width: Int, height: Int) {

        // NOTE: Это работает на телефоне и планшете (не фига не понял почему)
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                surfaceTexture.setDefaultBufferSize(width, width)
            }

            Configuration.ORIENTATION_LANDSCAPE -> {
                surfaceTexture.setDefaultBufferSize(height,height)
            }

            else -> {
                surfaceTexture.setDefaultBufferSize(width, height)
            }
        }

        // NOTE: Это работает на телефоне, НО не работает на планшете
        /*when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                surfaceTexture.setDefaultBufferSize(width, height)
            }

            Configuration.ORIENTATION_LANDSCAPE -> {
                surfaceTexture.setDefaultBufferSize(height, width)
            }

            else -> {
                surfaceTexture.setDefaultBufferSize(width, height)
            }
        }*/
    }

    /**
     * Будем настраивать трансформацию аналогично TextureView.
     * https://developer.android.com/reference/android/view/TextureView#setTransform(android.graphics.Matrix)
     *
     * INFO: Важен порядок операций:
     *  - Сначала scale
     *  - Потом rotate
     *
     * Сначала позиционируем вертекс с учетом положения дисплея (поворот), а потом скалируем:
     *
     *   |scaleM| * |rotateM| * |vertex|
     *
     * Если сделать наоборот, то скалированный вертекс улетит не туда после поворота и поплывёт
     * логика в методе [scaleModel]
     *
     * INFO: По поводу порядка применения матриц см [dev.barabu.base.matrix.TransformationsOrder]
     */
    private fun updateModelMatrix(width: Float, height: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        scaleModel(width, height)
        rotateModel()
    }

    private fun rotateModel() {
        Matrix.rotateM(modelMatrix, 0, displayRotation.toFloat(), 0f, 0f, 1f)
    }

    /**
     * INFO: 1
     *  ПИЛЯТЬ ! В горизонтальном положении ВООБЩЕ НЕ НАДО скалировать. Камера
     *  отдает кадр в своей натуральной ориентации и в размере Surface, который
     *  равен размеру экрана (у нас полноэкранное окно)
     * INFO: 2
     *  Предыдущее info справедливо, если в настройках девайса разрешен автоповорот,
     *  и активити пересоздается при смене ориентации. То есть получаем новую Surface
     *  и в горизонтальной ориентации ничего делать не надо, потому что кадр из камеры
     *  полностью готов к показу на весь экран.
     * INFO: 3
     *  Смотри комменты к методу [updateModelMatrix]
     */
    private fun scaleModel(width: Float, height: Float) {
        if (height > width) {
            // В вертикальном положении за 1 берем высоту (Y) и скалируем вертексы по ширине (X)
            val ratio = height / width
            Matrix.scaleM(modelMatrix, 0, ratio, 1f, 1f)
        } else {
            // В горизонтальном положении за 1 берем ширину (X) и скалируем вертексы по высоте (Y)
            val ratio = width / height
            Matrix.scaleM(modelMatrix, 0, 1f, ratio, 1f)
        }
    }

    /**
     * Алгоритм такой:
     * - Имеем ориентацию Portrait на экране W/H 720/1280
     * - Оставляем X-координаты вертексов равными -1 и 1
     * - Скалируем Y-координаты вертексов на величину H/W (1280/720=1.78)
     * То есть захватываем из мира прямоугольник [-1.0, -1.78] - [1.0, 1,78]. Ортогональная
     * проекция упакует прямоугольник в стандартный квадрат [-1, -1] - [1, 1], то сплющит
     * картинку по вертикали.
     *
     * INFO: Вариант с ортогональной проекцией не прокатил. Нужно явно скалировать вертексы.
     */
    private fun updateOrthographicProjectionMatrix(width: Float, height: Float) {
        val ratio = if (width > height) width / height else height / width

        if (width > height) {
            orthoM(projMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f)
        } else {
            orthoM(projMatrix, 0, -1f, 1f, -ratio, ratio, -1f, 1f)
        }
    }

    companion object {
        private const val TAG = "CameraRenderer"

        private val DISPLAY_ROTATIONS = mapOf(
            Surface.ROTATION_0 to 0,
            Surface.ROTATION_90 to 90,
            Surface.ROTATION_180 to 180,
            Surface.ROTATION_270 to 270
        )
    }
}