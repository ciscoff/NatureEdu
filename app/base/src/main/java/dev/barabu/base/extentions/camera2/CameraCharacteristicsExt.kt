package dev.barabu.base.extentions.camera2

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size

/**
 * Набор полезных расширений частично взятых из https://github.com/tomoima525/cameraLayout/tree/master
 */

fun CameraCharacteristics.isSupported(
    modes: CameraCharacteristics.Key<IntArray>,
    mode: Int
): Boolean {
    val ints = this.get(modes) ?: return false
    for (value in ints) {
        if (value == mode) {
            return true
        }
    }
    return false
}

fun CameraCharacteristics.isFacingFront(): Boolean =
    get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT

fun CameraCharacteristics.isFacingBack(): Boolean =
    get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK

fun CameraCharacteristics.isAutoExposureSupported(mode: Int): Boolean =
    isSupported(
        CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
        mode
    )

fun CameraCharacteristics.isContinuousAutoFocusSupported(): Boolean =
    isSupported(
        CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,
        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
    )

fun CameraCharacteristics.isAutoWhiteBalanceSupported(): Boolean =
    isSupported(
        CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES,
        CameraCharacteristics.CONTROL_AWB_MODE_AUTO
    )

fun CameraCharacteristics.sensorOrientation(): Int =
    get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

fun CameraCharacteristics.minFocusDist(): Float =
    get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

/**
 * Выбрать наибольшее разрешение из доступных
 */
fun CameraCharacteristics.getCaptureSize(comparator: Comparator<Size>): Size {
    val map: StreamConfigurationMap =
        get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return Size(0, 0)
    return map.getOutputSizes(ImageFormat.JPEG)
        .asList()
        .maxWith(comparator) ?: Size(0, 0)
}

fun CameraCharacteristics.printAvailableFrameSizes(): String {
    val result = ArrayList<String>()
    val map = get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    for (size in map!!.getOutputSizes(SurfaceTexture::class.java)) {
        result.add("imageDimension ${size.width}x${size.height}")
    }
    return result.joinToString("\n")
}