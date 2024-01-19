package dev.barabu.nature.camera.art

import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.base.camera.getCameraId
import dev.barabu.nature.camera.art.domain.Camera

class ArtActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: ArtGLSurfaceView

    private val cameraThread: HandlerThread by lazy {
        HandlerThread(packageName).apply { start() }
    }
    private val cameraHandler: Handler by lazy {
        Handler(cameraThread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cameras = mapOf(
            LENS_FACING_BACK to fetchCamera(LENS_FACING_BACK),
            /*LENS_FACING_FRONT to fetchCamera(LENS_FACING_FRONT)*/
        )

        glSurfaceView = ArtGLSurfaceView(this, cameras).apply {
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
        // todo: надо как-то очищать ресурсы камер
    }

    private fun fetchCamera(lens: Int): Camera {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(manager, lens)
        return Camera(cameraId, manager.getCameraCharacteristics(cameraId), cameraHandler, this)
    }
}