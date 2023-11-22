package dev.barabu.base.math

import android.opengl.Matrix
import dev.barabu.base.geometry.Vector
import dev.barabu.base.extentions.angleBetween
import dev.barabu.base.extentions.asString
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * refs:
 * https://danceswithcode.net/engineeringnotes/quaternions/quaternions.html
 * https://www.youtube.com/watch?v=tBYXR_fAXSQ
 *
 * Вращение вертекса с помощью кватерниона:
 * - Превращаешь вертекс в кватернион H. [x, y, z] -> (0 + x*i + y*j + z*k)
 * - Ось вращения и угол превращаешь в другой кватернион Q
 * - Умножаешь Q*H*Q', где Q'- это инверсный кватернион
 * Из полученного кватерниона извлекаешь q1, q2, q3 - это и есть результат поворота.
 */
class Quaternion(val q0: Float, val q1: Float, val q2: Float, val q3: Float) {

    /**
     * Это для случая когда кватернион используется многократно - бесконечное вращение.
     */
    val rowMajorRotationM: FloatArray by lazy {
        FloatArray(16).also { array ->
            if (isIdentityQuaternion) {
                Matrix.setIdentityM(array, 0)
            } else {
                rowMajorOrderRotationM(array)
            }
        }
    }

    val columnMajorRotationM: FloatArray by lazy {
        FloatArray(16).also { array ->
            if (isIdentityQuaternion) {
                Matrix.setIdentityM(array, 0)
            } else {
                columnMajorOrderRotationM(array)
            }
        }
    }

    operator fun times(s: Quaternion): Quaternion = Quaternion(
        q0 * s.q0 - q1 * s.q1 - q2 * s.q2 - q3 * s.q3,
        q0 * s.q1 + q1 * s.q0 - q2 * s.q3 + q3 * s.q2,
        q0 * s.q2 + q1 * s.q3 + q2 * s.q0 - q3 * s.q1,
        q0 * s.q3 - q1 * s.q2 + q2 * s.q1 + q3 * s.q0
    )

    val inverse: Quaternion by lazy { Quaternion(q0, -q1, -q2, -q3) }

    private val isIdentityQuaternion: Boolean = q0 == 0f && q1 == 1f && q2 == 0f && q3 == 0f

    /**
     * NOTE: Магнитуда кватерниона вращения всегда 1. Проверяй !
     */
    fun len(): Float = sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)

    fun print(name: String? = null): String = "Q${name}:($q0, $q1, $q2, $q3)"

    private fun columnMajorOrderRotationM(dest: FloatArray) {

        dest[0] = 1f - 2 * q2 * q2 - 2 * q3 * q3
        dest[1] = 2 * q1 * q2 - 2 * q0 * q3
        dest[2] = 2 * q1 * q3 + 2 * q0 * q2
        dest[3] = 0f

        dest[4] = 2 * q1 * q2 + 2 * q0 * q3
        dest[5] = 1f - 2 * q1 * q1 - 2 * q3 * q3
        dest[6] = 2 * q2 * q3 - 2 * q0 * q1
        dest[7] = 0f

        dest[8] = 2 * q1 * q3 - 2 * q0 * q2
        dest[9] = 2 * q2 * q3 + 2 * q0 * q1
        dest[10] = 1f - 2 * q1 * q1 - 2 * q2 * q2
        dest[11] = 0f

        dest[12] = 0f
        dest[13] = 0f
        dest[14] = 0f
        dest[15] = 1f
    }

    /**
     * При использовании этой матрицы куб следует за пальцем и не нужно умножать угол
     * между векторами на (-1f)
     */
    private fun rowMajorOrderRotationM(dest: FloatArray) {

        dest[0] = 1f - 2 * q2 * q2 - 2 * q3 * q3
        dest[4] = 2 * q1 * q2 - 2 * q0 * q3
        dest[8] = 2 * q1 * q3 + 2 * q0 * q2
        dest[12] = 0f

        dest[1] = 2 * q1 * q2 + 2 * q0 * q3
        dest[5] = 1f - 2 * q1 * q1 - 2 * q3 * q3
        dest[9] = 2 * q2 * q3 - 2 * q0 * q1
        dest[13] = 0f

        dest[2] = 2 * q1 * q3 - 2 * q0 * q2
        dest[6] = 2 * q2 * q3 + 2 * q0 * q1
        dest[10] = 1f - 2 * q1 * q1 - 2 * q2 * q2
        dest[14] = 0f

        dest[3] = 0f
        dest[7] = 0f
        dest[11] = 0f
        dest[15] = 1f
    }

    companion object {

        private const val TAG = "Quaternion"

        /**
         * Два исходных вектора имеют общее основание и образуют плоскость. Нам нужен поворот
         * вокруг оси, перпендикулярной этой плоскости и проходящей через основание векторов.
         *
         * INFO: angleBetween возвращает угол, а crossProduct показывает ось вокруг которой
         *  осуществляем поворот. Угол всегда от 0 до PI (фактически от 0 до 180 градусов), а вот
         *  направление оси будет определять "по" или "против" часовой повернемся мы повернемся
         *  на этот угол.
         */

        fun fromVectors(v1: Vector, v2: Vector): Quaternion {

            // NOTE: нужен unit-вектор, чтобы магнитуда кватерниона была 1
            val axis = v1.crossProduct(v2).unit
            val angle = v1.angleBetween(v2)

            /**
             * NOTE: ВОТ ТУТ БЫЛ ПИЗДЕЦ !!!! ОШИБКА в повороте из-за Quaternion(0f, 1f, 0f, 0f)
             *
             * INFO: Случай "identity quaternion"
             *
             * Вот тут [https://danceswithcode.net/engineeringnotes/quaternions/quaternions.html]
             * есть раздел о конвертации кватерниона в axis-angle представление и рассмотрен случай
             * когда в кватернионе q0 равен 1. Так как q0 есть косинус угла, то сам угол равен 0.
             * Предлагается просто записывать axis-angle как (0f, 1f, 0f, 0f). То есть нулевой
             * угол, поворота нет, а магнитуда вектора оси равна 1. Вроде все ОК.
             *
             * Я решил последовать этому совету в своем коде. И хотя я выполняю обратную конвертацию
             * из axis-angle в кватернион, но для случая cos == 1f стал создавать кватернион
             * (0f, 1f, 0f, 0f) и словил проблему при повороте куба касанием - в какой-то момент куб
             * перепрыгивал на 180 градусов. Вместо front появлялся back. Явно был мгновенный
             * поворот либо вокруг Y, либо вокруг X. Логирование позволило определить, что поворот
             * на 180 происходит вокруг X. И оказалось что из-за Quaternion(0f, 1f, 0f, 0f).
             * Матрица поворота вычисляла поворот на PI вокруг оси X.
             *
             * В результате я решил оставить форму (0f, 1f, 0f, 0f) чтобы сохранить единичную
             * магнитуду, но матрицу поворота отдавать как Identity. И все завелось !!!
             */
            val cos = cos(angle / 2f)
            return if (cos == 1f) {
                Quaternion(0f, 1f, 0f, 0f)
            } else Quaternion(
                q0 = cos,
                q1 = axis.x * sin(angle / 2f),
                q2 = axis.y * sin(angle / 2f),
                q3 = axis.z * sin(angle / 2f)
            ).also { q -> checkLength(q) }
        }

        fun fromAxisAngle(radians: Float, axis: Vector): Quaternion {
            val cos = cos(radians / 2f)
            return if (cos == 1f) {
                Quaternion(0f, 1f, 0f, 0f)
            } else Quaternion(
                q0 = cos,
                q1 = axis.x * sin(radians / 2f),
                q2 = axis.y * sin(radians / 2f),
                q3 = axis.z * sin(radians / 2f)
            ).also { q -> checkLength(q) }
        }

        private fun checkLength(q: Quaternion): Boolean {
            val l = q.len()
            return if (l in 0.999..1.00)
                true
            else
                throw IllegalStateException("Magnitude should be 1. Current is ${l.asString}")
        }
    }
}