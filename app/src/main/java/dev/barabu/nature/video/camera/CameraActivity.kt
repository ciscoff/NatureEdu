package dev.barabu.nature.video.camera

import android.annotation.SuppressLint
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.base.Logging
import dev.barabu.nature.video.camera.domain.CameraWrapper
import dev.barabu.nature.video.camera.domain.CameraWrapper.Companion.getCameraId

/**
 * refs:
 * https://habr.com/ru/articles/468083/
 * https://habr.com/ru/articles/480878/
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: CameraSurfaceView
    private lateinit var backFacingCam: CameraWrapper

    private val cameraThread: HandlerThread by lazy {
        HandlerThread(packageName).apply { start() }
    }
    private val cameraHandler: Handler by lazy {
        Handler(cameraThread.looper)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        Logging.d("WOWOW onCreate")
        super.onCreate(savedInstanceState)

        backFacingCam = fetchCamera(LENS_FACING_BACK)

        glSurfaceView = CameraSurfaceView(this, backFacingCam).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        backFacingCam.release()
    }

    private fun fetchCamera(lens: Int): CameraWrapper {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(manager, lens)
        return CameraWrapper(cameraId, manager.getCameraCharacteristics(cameraId), cameraHandler)
    }
}