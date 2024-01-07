package dev.barabu.nature.video.camera.domain

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
import android.hardware.camera2.CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Looper
import android.view.Surface
import dev.barabu.base.Logging
import dev.barabu.nature.video.camera.CameraActivity

/**
 * Сенсор камеры выравнивается по текущему положению "длинной" части экрана. Если текущая
 * ширина экрана больше высоты - то сенсор считает что у нас альбомная ориентация.
 */
class CameraWrapper(
    private val cameraId: String,
    private val characteristics: CameraCharacteristics,
) {

    private lateinit var surfaceTexture: SurfaceTexture

    private var cameraDevice: CameraDevice? = null

    private val physicalCameraIds: List<String>
        get() = characteristics.physicalCameraIds.toList()

    val sensorOrientation: Int
        get() = characteristics.get(SENSOR_ORIENTATION) ?: CameraActivity.SENSOR_ORIENTATION_UNKNOWN

    val isFront: Boolean
        get() = characteristics.get(LENS_FACING) == CameraMetadata.LENS_FACING_FRONT

    private val minFocusDist: Float
        get() = characteristics.get(LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

    private val cameraStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cd: CameraDevice) {
            cameraDevice = cd
            createCameraPreviewSession(cd, surfaceTexture)
        }

        override fun onDisconnected(p0: CameraDevice) {
        }

        override fun onError(p0: CameraDevice, p1: Int) {
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(manager: CameraManager, surfaceTexture: SurfaceTexture) {
        this.surfaceTexture = surfaceTexture

        /*printAvailableFrameSizes(characteristics)*/

        manager.openCamera(
            cameraId,
            cameraStateCallback,
            android.os.Handler(Looper.getMainLooper())
        )
    }

    private fun createCameraPreviewSession(
        camera: CameraDevice,
        surfaceTexture: SurfaceTexture
    ) {
        val surface = Surface(surfaceTexture)
        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(surface)

        val sessionStateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                setup3AControls(builder)
                session.setRepeatingRequest(builder.build(), null, null)
            }

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                TODO("Not yet implemented")
            }
        }

        camera.createCaptureSession(
            listOf(surface), sessionStateCallback,
            android.os.Handler(Looper.getMainLooper())
        )
        surface.release()
    }

    /**
     * Настроить builder использовать auto-focus, auto-exposure и auto-white-balance
     */
    private fun setup3AControls(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)

        if (minFocusDist > 0f) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (characteristics.get(CONTROL_AF_AVAILABLE_MODES)
                    ?.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) == true
            ) {
                builder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }

            if (characteristics.get(CONTROL_AWB_AVAILABLE_MODES)
                    ?.contains(CaptureRequest.CONTROL_AWB_MODE_AUTO) == true
            ) {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            }
        }
    }

    companion object {

        private fun printAvailableFrameSizes(characteristics: CameraCharacteristics) {
            val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP)
            for (size in map!!.getOutputSizes(SurfaceTexture::class.java)) {
                Logging.d("imageDimension ${size.width}/${size.height}")
            }
        }

        /**
         * ref: https://www.youtube.com/watch?v=IPJIjlxRrLI
         */
        fun getCameraId(
            manager: CameraManager,
            lens: Int
        ): String {
            var deviceId = emptyList<String>()

            try {
                val camIdList = manager.cameraIdList
                deviceId = camIdList.filter { id ->
                    lens == cameraCharacteristics(
                        manager,
                        id,
                        LENS_FACING
                    )
                }
            } catch (e: CameraAccessException) {
                Logging.e("${e.message}")
            }
            return deviceId[0]
        }

        private fun <T> cameraCharacteristics(
            manager: CameraManager,
            cameraId: String,
            key: CameraCharacteristics.Key<T>
        ): T? {
            val characteristics = manager.getCameraCharacteristics(cameraId)

            return when (key) {
                LENS_FACING -> {
                    characteristics.get(key)
                }

                SCALER_STREAM_CONFIGURATION_MAP -> {
                    characteristics.get(key)
                }

                else -> throw IllegalArgumentException("Key not recognized")
            }
        }
    }
}