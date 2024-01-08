package dev.barabu.nature.video.camera.domain

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Looper
import android.view.Surface
import dev.barabu.base.Logging
import dev.barabu.base.extentions.camera2.isAutoExposureSupported
import dev.barabu.base.extentions.camera2.isAutoWhiteBalanceSupported
import dev.barabu.base.extentions.camera2.isContinuousAutoFocusSupported
import dev.barabu.base.extentions.camera2.minFocusDist

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
     * ref: https://github.com/googlearchive/android-Camera2Raw
     * file: Camera2RawFragment.java
     *
     * Через builder включить auto-focus, auto-exposure и auto-white-balance
     */
    private fun setup3AControls(builder: CaptureRequest.Builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)

        // Если minFocusDist равно 0, то у камеры фокус фиксирован и не меняется
        val hasAutoFocus = characteristics.minFocusDist() > 0f

        if (hasAutoFocus) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (characteristics.isContinuousAutoFocusSupported()) {
                builder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (characteristics.isAutoExposureSupported(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }

        // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
        if (characteristics.isAutoWhiteBalanceSupported()) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
    }

    companion object {

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