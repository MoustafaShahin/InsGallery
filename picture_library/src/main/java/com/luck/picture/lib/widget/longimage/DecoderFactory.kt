package com.luck.picture.lib.widget.longimage

/**
 * Interface for decoder (and region decoder) factories.
 * @param <T> the class of decoder that will be produced.
</T> */
interface DecoderFactory<T> {
    /**
     * Produce a new instance of a decoder with type [T].
     * @return a new instance of your decoder.
     */
    @Throws(IllegalAccessException::class, InstantiationException::class)
    fun make(): T
}