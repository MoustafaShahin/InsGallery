package com.luck.picture.lib.instagram

/**
 * ================================================
 * Created by JessYan on 2020/4/20 15:45
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
interface InstagramCaptureListener {
    fun takePictures()
    fun recordStart()
    fun recordEnd(time: Long)
    fun recordShort(time: Long)
    fun recordError()
}