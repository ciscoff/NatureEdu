package dev.barabu.nature.camera.preview

import android.annotation.SuppressLint
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dev.barabu.base.Logging
import dev.barabu.base.camera.getCameraId
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.camera.Camera
import dev.barabu.nature.camera.CameraWrapper
import dev.barabu.nature.camera.preview.gl.PreviewRenderer
import dev.barabu.widgets.menu.domain.Lens
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * refs:
 * https://habr.com/ru/articles/468083/
 * https://habr.com/ru/articles/480878/
 */
class PreviewActivity : AppCompatActivity() {

    private lateinit var cameraState: MutableStateFlow<CameraWrapper>
    private lateinit var glSurfaceView: PreviewGlSurfaceView
    private lateinit var renderer: PreviewRenderer

    private var isRendererSet = false

    private val cameraThread: HandlerThread by lazy {
        HandlerThread(packageName).apply { start() }
    }
    private val cameraHandler: Handler by lazy {
        Handler(cameraThread.looper)
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this as AppCompatActivity)[PreviewViewModel::class.java]
    }

    private val touchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                true
            }

            MotionEvent.ACTION_MOVE -> {
                true
            }

            MotionEvent.ACTION_UP -> {
                viewModel.onTouch()
                v.performClick()
            }

            else -> false
        }
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraState = MutableStateFlow(CameraWrapper(null))

        glSurfaceView = PreviewGlSurfaceView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            if (isActualGlEsSupporting) {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                renderer = PreviewRenderer(this, cameraState)
                setRenderer(renderer)
                isRendererSet = true
            } else {
                Logging.d("This device does not support OpenGL ES 3.0.")
            }

            setOnTouchListener(touchListener)
        }
        setContentView(glSurfaceView)

        viewModel.lensState.observe(this) { lensState ->
            handleCameraSwap(lensState.lens)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onActivityStart()
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

    override fun onStop() {
        super.onStop()
        cameraState.tryEmit(CameraWrapper(null))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
    }

    private fun fetchCamera(lens: Int): Camera {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(manager, lens)
        return Camera(cameraId, manager.getCameraCharacteristics(cameraId), cameraHandler, this)
    }

    private fun handleCameraSwap(lens: Lens?) {
        if (lens == null) return
        when (lens) {
            Lens.Back -> swapCamera(LENS_FACING_BACK)
            Lens.Front -> swapCamera(CameraMetadata.LENS_FACING_FRONT)
        }
    }

    private fun swapCamera(lens: Int) {
        cameraState.tryEmit(CameraWrapper(fetchCamera(lens)))
    }
}