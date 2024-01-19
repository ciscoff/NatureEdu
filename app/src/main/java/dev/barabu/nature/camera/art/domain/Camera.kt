package dev.barabu.nature.camera.art.domain

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.barabu.base.Logging
import dev.barabu.base.extentions.camera2.isAutoExposureSupported
import dev.barabu.base.extentions.camera2.isAutoWhiteBalanceSupported
import dev.barabu.base.extentions.camera2.isContinuousAutoFocusSupported
import dev.barabu.base.extentions.camera2.minFocusDist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * ref:
 * https://github.com/android/camera-samples/blob/main/Camera2Video/app/src/main/java/com/example/android/camera2/video/fragments/PreviewFragment.kt#L255
 */
class Camera(
    private val cameraId: String,
    private val characteristics: CameraCharacteristics,
    private val cameraHandler: Handler,
    private val lifecycleOwner: LifecycleOwner
) {

    private class HandlerExecutor(private val handler: Handler) : Executor {
        override fun execute(command: Runnable) {
            if (!handler.post(command)) {
                throw RejectedExecutionException("${handler::class.simpleName} is shutting down");
            }
        }
    }

    private val sensorOrientation: Int?
        get() = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequest: CaptureRequest

    /**
     * Surface используется дважды:
     * - создание CameraCaptureSession
     * - создание CaptureRequest
     */
    fun initializeCamera(manager: CameraManager, surfaceTexture: SurfaceTexture) =
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val surface = Surface(surfaceTexture)
            cameraDevice = openCamera(manager, cameraId, cameraHandler)
            captureSession = createCaptureSession(cameraDevice, surface, cameraHandler)
            captureRequest = createCaptureRequest(cameraDevice, surface)
            captureSession.setRepeatingRequest(captureRequest, null, cameraHandler)
            surface.release()
        }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {

            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Logging.e(RuntimeException("Camera $cameraId error: ($error) $msg"))
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        target: Surface,
        handler: Handler,
    ): CameraCaptureSession = suspendCoroutine { cont ->

        val sessionStateCallback = object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) {
                cont.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Logging.e(exc)
                cont.resumeWithException(exc)
            }

            /** Called after all captures have completed */
            override fun onClosed(session: CameraCaptureSession) {
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val outputConfig = OutputConfiguration(target)
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(outputConfig), HandlerExecutor(handler), sessionStateCallback
            )
            device.createCaptureSession(sessionConfig)
        } else {
            device.createCaptureSession(listOf(target), sessionStateCallback, handler)
        }
    }

    private fun createCaptureRequest(cameraDevice: CameraDevice, target: Surface): CaptureRequest =
        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(target)
            setup3AControls()
        }.build()

    /**
     * ref: https://github.com/googlearchive/android-Camera2Raw
     * file: Camera2RawFragment.java
     *
     * Через builder включить auto-focus, auto-exposure и auto-white-balance
     */
    private fun CaptureRequest.Builder.setup3AControls() {
        // Enable auto-magical 3A run by camera device
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)

        // Если minFocusDist равно 0, то у камеры фокус фиксирован и не меняется
        val hasAutoFocus = characteristics.minFocusDist() > 0f

        if (hasAutoFocus) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (characteristics.isContinuousAutoFocusSupported()) {
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            } else {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (characteristics.isAutoExposureSupported(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        } else {
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }

        // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
        if (characteristics.isAutoWhiteBalanceSupported()) {
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
    }
}