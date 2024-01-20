package dev.barabu.nature.camera.art

import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.base.Logging
import dev.barabu.base.camera.getCameraId
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.camera.art.domain.Camera
import dev.barabu.nature.camera.art.domain.CameraWrapper
import dev.barabu.nature.camera.art.gl.ArtRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Collections

class ArtActivity : AppCompatActivity() {

    private lateinit var cameraState: MutableStateFlow<CameraWrapper>
    private lateinit var glSurfaceView: ArtGLSurfaceView
    private lateinit var renderer: ArtRenderer

    private val cameras = arrayListOf(LENS_FACING_BACK, LENS_FACING_FRONT)
    private var isRendererSet = false

    private val cameraThread: HandlerThread by lazy {
        HandlerThread(packageName).apply { start() }
    }
    private val cameraHandler: Handler by lazy {
        Handler(cameraThread.looper)
    }

    private val touchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_MOVE -> true
            MotionEvent.ACTION_UP -> {
                renderer.stopCapture(cameraState.value)
                Collections.rotate(cameras, 1)
                Handler(Looper.getMainLooper()).postDelayed({
                    cameraState.tryEmit(CameraWrapper(fetchCamera(cameras[0])))
                }, 100)
                v.performClick()
            }

            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraState = MutableStateFlow(CameraWrapper(fetchCamera(cameras[0])))

        glSurfaceView = ArtGLSurfaceView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            if (isActualGlEsSupporting) {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                renderer = ArtRenderer(this, cameraState)
                setRenderer(renderer)
                isRendererSet = true
            } else {
                Logging.d("This device does not support OpenGL ES 3.0.")
            }
            setOnTouchListener(touchListener)
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

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        // note: надо как-то очищать ресурсы камер
        renderer.stopCapture(cameraState.value)
    }

    private fun fetchCamera(lens: Int): Camera {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(manager, lens)
        return Camera(cameraId, manager.getCameraCharacteristics(cameraId), cameraHandler, this)
    }
}