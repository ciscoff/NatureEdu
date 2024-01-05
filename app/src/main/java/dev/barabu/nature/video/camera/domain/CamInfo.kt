package dev.barabu.nature.video.camera.domain

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Looper
import android.view.Surface


/**
 * Сенсор камеры выравнивается по текущему положению "длинной" части экрана. Если текущая
 * ширина экрана больше высоты - то сенсор считает что у нас альбомная ориентация.
 */
class CamInfo(
    val cameraId: String,
    val isFront: Boolean,
    private val manager: CameraManager
) {
    private var cameraDevice: CameraDevice? = null

    private lateinit var surfaceTexture: SurfaceTexture

    private val cameraStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(p0: CameraDevice) {
            cameraDevice = p0
            createCameraPreviewSession(p0, surfaceTexture)
        }

        override fun onDisconnected(p0: CameraDevice) {
        }

        override fun onError(p0: CameraDevice, p1: Int) {
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surfaceTexture: SurfaceTexture) {
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
}