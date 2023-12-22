package dev.barabu.base

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y
import android.opengl.GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLUtils
import androidx.annotation.DrawableRes

/**
 * В OpenGL текстуры могут быть различного размера, но ОЧЕНЬ ЖЕЛАТЕЛЬНО чтобы они были степенью
 * двойки, то есть 128, 256, 512 и тд. Максимальный размер зависит от реализации, но как правило
 * это 2048x2048.
 *
 * Теперь по поводу биндинга и texture target. Терминология пипец какая странная, но разберемся
 * на примере трех функций:
 *
 * Sampling (или Texture Sample) - это операция получения цвета по координатам текстуры.
 * -------
 *
 * glGenTextures(1, texDescriptor, 0) - создаем одну текстуру и получаем ее дескриптор (он же
 *   unsigned int, он же texture name). В этот момент в нативном пространстве создан пустой
 *   объект и у нас есть его дескриптор.
 *
 * glBindTexture(GL_TEXTURE_2D, texDescriptor[0]) - по документации выполняем привязку текстуры
 *   к некой target, которая есть GL_TEXTURE_2D. Ни хера не понятно что такое target. Этих
 *   таргетов много: GL_TEXTURE_1D, GL_TEXTURE_2D, GL_TEXTURE_3D,... На самом деле target - это как
 *   бы API для работы с текстурой определенного формата. Например, у нас обычная 2D-битмапа,
 *   сериализованная в байтовый буфер. Чтобы этот буфер трактовался в OpenGL как двумерный массив мы
 *   привязываем текстуру к таргету GL_TEXTURE_2D. И теперь все операции с нашей текстурой будут
 *   работать с байтовым буфером как с двумерным массивом. И далее самое интересное - таргет
 *   становится как бы псевдонимом нашей текстуры и все операции на таргете GL_TEXTURE_2D будут
 *   выполняться на нашей текстуре до тех пор, пока мы ее не отвяжем от таргета. Получается что в
 *   OpenGL по одному таргету каждого типа. И в каждый момент времени только одна текстура может
 *   "владеть" таргетом определенного типа. Таргет оккупирован до тех пор, пока текстура либо явно
 *   не освободит его, либо другую текстуру привяжут к этому же таргету.
 *
 * glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR) - здесь мы применяем фильтр
 *   к текстуре, но указываем таргет, а не дескриптор. Это потому что текстура привязана к таргету
 *   и достаточно указать только его, чтобы работать с текстурой. То есть теперь везде указываем
 *   только таргет, а по факту выполняем операции с привязанной к нему текстурой.
 *
 * Фактически мы проделали следующее - в нативном пространстве создали (glGenTextures) объект
 * текстуры и получили её дескриптор. Потом указали тип текстуры как 2D (glBindTexture). Далее
 * настроили кое-какие параметры - указали типы фильтров для текстуры (glTexParameteri). И вот после
 * этого можно залить байты картинки в текстуру (texImage2D). На этом пока все и можно текстуру
 * отвязать от таргета. Это была первая часть балета - подготовительная.
 *
 * INFO: А можно иначе взглянуть - с помощью таргета мы нацелили OpenGL на нашу текстуру и все
 *  операции применяются к ней без явного указания дескриптора.
 *
 * Вторая часть начнётся в тот момент, когда текстура потребуется непосредственно для рисования.
 * "Для рисования текстурой" её нужно связать с texture unit'ом. В нашем распоряжении несколько
 * texture unit'ов для использования нескольких текстур, НО в каждый момент времени только ОДИН
 * texture unit является активным и только его текстура используется sampler'ом. Для смены текстуры
 * нужно активировать соответствующий texture unit. Эти операции выполняются в наследниках
 * ShaderProgram (см там)
 */
object TextureLoader {

    private const val CUBE_FACES = 6

    private const val TAG = "TextureLoader"

    /**
     * Создает текстуру (в нативном пространстве) и получает её хендлер. Загружает bitmap из
     * ресурсов и заливает в текстуру. Возвращает дескриптор.
     */
    fun loadTexture(
        context: Context,
        @DrawableRes resourceId: Int,
        listener: TexLoadListener? = null
    ): Int {
        Logging.d("$TAG.loadTexture")

        val texDescriptor = IntArray(1)
        glGenTextures(1, texDescriptor, 0)

        if (texDescriptor[0] == ERROR_CODE) return INVALID_DESCRIPTOR

        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        val bitmap = kotlin.runCatching {
            BitmapFactory.decodeResource(context.resources, resourceId, options)
        }.getOrNull()

        if (bitmap == null) {
            Logging.e("$TAG.loadTexture is failed")
            glDeleteTextures(1, texDescriptor, 0)
            return INVALID_DESCRIPTOR
        }

        // Привязываем текстуру к таргету GL_TEXTURE_2D.
        glBindTexture(GL_TEXTURE_2D, texDescriptor[0])

        listener?.onTexPreload(texDescriptor[0])

        // Заливаем картинку в текстуру
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        listener?.onTexLoaded(texDescriptor[0])

        // Освобождаем таргет
        glBindTexture(GL_TEXTURE_2D, 0)

        return texDescriptor[0]
    }

    /**
     * Мы внутри куба (isCubeInside == true) или снаружи ?
     */
    fun loadCubeMap(
        context: Context,
        @DrawableRes cubeResources: IntArray,
        isCubeInside: Boolean = true
    ): Int {
        Logging.d("$TAG.loadCubeMap")

        val texDescriptor = IntArray(1)
        glGenTextures(1, texDescriptor, 0)

        if (texDescriptor[0] == ERROR_CODE) return INVALID_DESCRIPTOR

        val options = BitmapFactory.Options().apply {
            inScaled = false
        }

        val cubeBitmaps = arrayOfNulls<Bitmap>(CUBE_FACES)

        repeat(CUBE_FACES) { i ->
            cubeBitmaps[i] =
                BitmapFactory.decodeResource(context.resources, cubeResources[i], options)
            if (cubeBitmaps[i] == null) {
                glDeleteTextures(1, texDescriptor, 0)
                return INVALID_DESCRIPTOR
            }
        }

        // Привязываем текстуру к таргету GL_TEXTURE_CUBE_MAP.
        glBindTexture(GL_TEXTURE_CUBE_MAP, texDescriptor[0])

        // Применяем фильтры к таргету (то есть к нашей текстуре, потому что она привязана к таргету)
        glTexParameteri(
            GL_TEXTURE_CUBE_MAP,
            GL_TEXTURE_MIN_FILTER,
            GL_LINEAR
        )
        glTexParameteri(
            GL_TEXTURE_CUBE_MAP,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR
        )

        // Загружаем картинки для каждой стороны куба
        //
        // NOTE: Front куба повернут к наблюдателю, то есть в сторону положительного Z. Однако
        //  мы ставим Front по отрицательному Z (GL_TEXTURE_CUBE_MAP_NEGATIVE_Z), а Back по
        //  положительному (GL_TEXTURE_CUBE_MAP_POSITIVE_Z). То есть это left-handed ориентация.
        //  Существует такое правило:
        //  - Если наблюдатель внутри куба, то используем left-handed раскладку сторон куба
        //  - Если наблюдатель снаружи, то right-handed раскладку сторон куба

        val (front, back) = if (isCubeInside) 4 to 5 else 5 to 4

        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, cubeBitmaps[0], 0)
        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, cubeBitmaps[1], 0)
        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, cubeBitmaps[2], 0)
        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, cubeBitmaps[3], 0)
        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, cubeBitmaps[front], 0) // front
        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, cubeBitmaps[back], 0) // back

        // Освобождаем таргет
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0)

        repeat(CUBE_FACES) { i -> cubeBitmaps[i]?.recycle() }
        return texDescriptor[0]
    }
}