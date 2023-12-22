package dev.barabu.base

/**
 * Оповестить клиента и этапах загрузки текстуры. Оба метода вызываются в состоянии
 * glBindTexture(GL_TEXTURE_2D, texDescriptor), поэтому клиенту не нужно делать
 * binding к таргету и нельзя вызывать glBindTexture(GL_TEXTURE_2D, 0)
 */
interface TexLoadListener {

    /**
     * Вызывается ДО заливки битмапы в нативный буфер. Вызывающий может выполнить настройку
     * будущей текстуры, например Wrapping и Filtering.
     */
    fun onTexPreload(texId: Int)

    /**
     * Вызывается ПОСЛЕ заливки битмапы в нативный буфер. Вызывающий может выполнить дополнительную
     * настройку текстуры, например сгенерить Mipmaps.
     */
    fun onTexLoaded(texId: Int)
}