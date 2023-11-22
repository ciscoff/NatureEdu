package dev.barabu.base.matrix

import kotlin.math.PI
import kotlin.math.tan

object ProjectionHelper {

    /**
     * Эта матрица полностью аналогична системной Matrix.perspectiveM
     */
    fun perspectiveM(m: FloatArray, yFovInDegrees: Float, aspect: Float, near: Float, far: Float) {

        val yFovInInRadians = yFovInDegrees * (PI.toFloat() / 180f)
        val angle = 1f / tan(yFovInInRadians / 2f)

        m[0] = angle / aspect
        m[1] = 0f
        m[2] = 0f
        m[3] = 0f

        m[4] = 0f
        m[5] = angle
        m[6] = 0f
        m[7] = 0f

        m[8] = 0f
        m[9] = 0f
        m[10] = -((far + near) / (far - near))
        m[11] = -1f

        m[12] = 0f
        m[13] = 0f
        m[14] = -((2f * far * near) / (far - near))
        m[15] = 0f
    }
}