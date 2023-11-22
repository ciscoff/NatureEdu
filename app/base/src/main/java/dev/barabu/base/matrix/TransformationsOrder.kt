package dev.barabu.base.matrix

import android.opengl.Matrix
import dev.barabu.base.Logging
import dev.barabu.base.extentions.printM
import dev.barabu.base.geometry.Point

/**
 * Порядок хранения векторов в матрице.
 *
 * Если вектора хранятся столбцами, то при умножении матрицы на вектор МАТРИЦА стоит СЛЕВА, а
 * вектор справа в виде столбца.
 * Если вектора хранятся строками, то при умножении матрицы на вектор МАТРИЦА стоит СПРАВА, а
 * вектор слева в виде строки.
 * Конечный результат в обоих случаях одинаков.
 *
 * OpenGL хранит вектора столбцами (column major order), а значит при умножении матрица слева,
 * вектор справа. И вообще, если требуется выполнить последовательность трансформаций, то их
 * матрицы записываются справа налево в порядке очередности соответствующей трансформации: правая
 * применяется первой, левая последней.
 *
 * Стоит помнить, что матрица - это набор единичных векторов отдельной системы координат и её origin
 * внутри глобальной системы координат. Т.е. левая матрица помещает свой правый операнд (вектор или
 * матрицу) в себя, то есть в свою систему координат, а значит модифицирует значения правого
 * операнда (вектора или матрицы)
 * Левая матрица своей трансформацией "усиливает" все предыдущие трансформации, которые
 * произошли справа от нее. Визуально процесс можно представить как каскад функций, где результат
 * всплывает из глубины: F3( F2( F1(V) ) ) == M3( M2( M1(V) ) )
 *
 * Если мы имеем две отдельные матрицы scale и translate, то их произведение
 *  multiplyMM (..., leftHandM = scale, rightHandM = translate, ...)
 *
 * даст точно такой же результат, как последовательный вызов
 *   Matrix.setIdentityM(M)
 *   Matrix.scaleM(M, ...)
 *   Matrix.translateM(M, ...)
 *
 * То есть, если у нас последовательность трансформаций
 *   Matrix.TR1()
 *   Matrix.TR2()
 *   Matrix.TR3()
 * то последняя (TR3) будет "ближе" к вертексу, который трансформируем и ее действие будет как бы
 * первым в череде трансформаций
 *
 *  TR1 * TR2 * TR3 * V
 *  TR1 * TR2 * (TR3 * V)
 *  TR1 * (TR2 * (TR3 * V))
 *
 * Так удобнее представлять порядок применения трансформаций к вертексу, а то такое свойство матриц
 * как ассоциативность, где A(BC) == (AC)B может только запутать.
 */
class TransformationsOrder(private val center: Point = Point(1.1f, 2.1f, 3.1f)) {

    private val eduTMatrix = FloatArray(16)
    private val eduSMatrix = FloatArray(16)
    private val eduFinalMatrix = FloatArray(16)

    /**
     * Здесь для каждой трансформации готовим отдельную матрицу, а потом эти матрицы
     * перемножаем для получения конечного результата.
     */
    fun explicitMM() {
        Matrix.setIdentityM(eduTMatrix, 0)
        Matrix.setIdentityM(eduSMatrix, 0)
        Matrix.setIdentityM(eduFinalMatrix, 0)

        Matrix.scaleM(eduSMatrix, 0, 2f, 2f, 2f)
        Matrix.translateM(eduTMatrix, 0, center.x, center.y, center.z)
        Logging.d(eduSMatrix.printM("explicitMM: scale matrix"))
        Logging.d(eduTMatrix.printM("explicitMM: translate matrix "))

        Matrix.multiplyMM(
            eduFinalMatrix, 0, eduSMatrix, 0, eduTMatrix, 0
        )
        Logging.d(eduFinalMatrix.printM("explicitMM: MUL scale * translate"))

        Matrix.multiplyMM(
            eduFinalMatrix, 0, eduTMatrix, 0, eduSMatrix, 0
        )
        Logging.d(eduFinalMatrix.printM("explicitMM: MUL translate * scale"))
    }

    /**
     * Здесь используем одну матрицу для всех трансформаций.
     */
    fun implicitMM() {
        Matrix.setIdentityM(eduFinalMatrix, 0)
        Matrix.scaleM(eduFinalMatrix, 0, 2f, 2f, 2f)
        Matrix.translateM(eduFinalMatrix, 0, center.x, center.y, center.z)
        Logging.d(eduFinalMatrix.printM("implicitMM: scale first, translate last"))

        Matrix.setIdentityM(eduFinalMatrix, 0)
        Matrix.translateM(eduFinalMatrix, 0, center.x, center.y, center.z)
        Matrix.scaleM(eduFinalMatrix, 0, 2f, 2f, 2f)
        Logging.d(eduFinalMatrix.printM("implicitMM: translate first, scale last"))
    }
}