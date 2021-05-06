package com.luck.picture.lib.widget.longimage

/**
 * Compatibility factory to instantiate decoders with empty public constructors.
 * @param <T> The base type of the decoder this factory will produce.
</T> */
class CompatDecoderFactory<T>(private val clazz: Class<out T?>) : DecoderFactory<T> {
    @Throws(IllegalAccessException::class, InstantiationException::class)
    override fun make(): T {
        return clazz.newInstance()
    }
}