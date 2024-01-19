package dev.barabu.base.camera

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import dev.barabu.base.Logging


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
                CameraCharacteristics.LENS_FACING
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
        CameraCharacteristics.LENS_FACING -> {
            characteristics.get(key)
        }

        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> {
            characteristics.get(key)
        }

        else -> throw IllegalArgumentException("Key not recognized")
    }
}