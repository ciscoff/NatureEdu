package dev.barabu.nature.video.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import dev.barabu.nature.video.camera.domain.CamInfo


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

        val cams = fetchAvailableCameras()

        glSurfaceView = CameraSurfaceView(this, cams[0]).apply {
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

    private fun fetchAvailableCameras(): List<CamInfo> {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val list = mutableListOf<CamInfo>()

        var isFront = false

        for (cameraId in cameraManager.cameraIdList) {

            val cc = cameraManager.getCameraCharacteristics(cameraId)

            // Список выходных форматов камеры
            val configurationMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            when (cc.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    isFront = true
                }

                CameraCharacteristics.LENS_FACING_BACK -> {
                }
            }

            list.add(CamInfo(cameraId, isFront, cameraManager))
        }
        return list
    }
}