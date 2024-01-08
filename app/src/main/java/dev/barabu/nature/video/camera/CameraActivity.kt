package dev.barabu.nature.video.camera

import android.annotation.SuppressLint
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.nature.video.camera.domain.CameraWrapper
import dev.barabu.nature.video.camera.domain.CameraWrapper.Companion.getCameraId

/**
 * refs:
 * https://habr.com/ru/articles/468083/
 * https://habr.com/ru/articles/480878/
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: CameraSurfaceView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backFacingCam = fetchCamera(LENS_FACING_BACK)

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

    private fun fetchCamera(lens: Int): CameraWrapper {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(manager, lens)
        return CameraWrapper(cameraId, manager.getCameraCharacteristics(cameraId))
    }
}