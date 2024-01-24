package dev.barabu.nature.camera.art

import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dev.barabu.base.Logging
import dev.barabu.base.camera.getCameraId
import dev.barabu.base.extentions.isActualGlEsSupporting
import dev.barabu.nature.camera.art.domain.Camera
import dev.barabu.nature.camera.art.domain.CameraWrapper
import dev.barabu.nature.camera.art.gl.ArtRenderer
import dev.barabu.widgets.MenuViewModel
import dev.barabu.widgets.R
import dev.barabu.widgets.domain.Effect
import dev.barabu.widgets.domain.Lens
import kotlinx.coroutines.flow.MutableStateFlow

class ArtActivity : AppCompatActivity() {

    private lateinit var cameraState: MutableStateFlow<CameraWrapper>
    private lateinit var glSurfaceView: ArtGLSurfaceView
    private lateinit var renderer: ArtRenderer

    private var isRendererSet = false

    private val cameraThread: HandlerThread by lazy {
        HandlerThread(packageName).apply { start() }
    }
    private val cameraHandler: Handler by lazy {
        Handler(cameraThread.looper)
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this as AppCompatActivity)[MenuViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraState = MutableStateFlow(CameraWrapper(null))

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
        }

        setContentView(glSurfaceView)

        val menu = createSideMenu()
        (glSurfaceView.parent as FrameLayout).addView(menu)

        viewModel.menuState.observe(this) { menuState ->
            handleCameraSwap(menuState.lens.value)
            handleEffect(menuState.effect.value)
        }
    }

    override fun onStart() {
        Logging.d("$TAG.onStart")
        super.onStart()
        viewModel.onActivityStart()
    }

    override fun onResume() {
        Logging.d("$TAG.onResume")
        super.onResume()
        if (isRendererSet) {
            glSurfaceView.onResume()
        }
    }

    /**
     * Управляем thread'ом glSurfaceView
     */
    override fun onPause() {
        Logging.d("$TAG.onPause")
        super.onPause()
        if (isRendererSet) {
            glSurfaceView.onPause()
        }
    }

    override fun onStop() {
        Logging.d("$TAG.onStop")
        super.onStop()
        cameraState.tryEmit(CameraWrapper(null))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
    }

    private fun handleEffect(effect: Effect?) {
        if (effect == null) return
        glSurfaceView.queueEvent {
            renderer.handleEffect(effect)
        }
    }

    private fun handleCameraSwap(lens: Lens?) {
        if (lens == null) return
        when (lens) {
            Lens.Back -> swapCamera(LENS_FACING_BACK)
            Lens.Front -> swapCamera(LENS_FACING_FRONT)
        }
    }

    private fun swapCamera(lens: Int) {
        Logging.d("$TAG.swapCamera")
        cameraState.tryEmit(CameraWrapper(fetchCamera(lens)))
    }

    private fun fetchCamera(lens: Int): Camera {
        Logging.d("$TAG.fetchCamera")
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(manager, lens)
        return Camera(cameraId, manager.getCameraCharacteristics(cameraId), cameraHandler, this)
    }

    private fun createSideMenu(): LinearLayout {
        Logging.d("$TAG.createSideMenu")

        val view = LayoutInflater.from(this)
            .inflate(R.layout.w_layout_menu, null, false) as LinearLayout

        view.apply {
            layoutParams = FrameLayout.LayoutParams(
                resources.getDimensionPixelOffset(R.dimen.w_menu_width),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(
                    0,
                    0,
                    resources.getDimensionPixelOffset(R.dimen.w_menu_margin_end),
                    resources.getDimensionPixelOffset(R.dimen.w_menu_margin_bottom)
                )
            }
            return view
        }
    }

    companion object {
        private const val TAG = "ArtActivity"
    }
}