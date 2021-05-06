package com.luck.picture.lib.camera.listener

/**
 * @author：luck
 * @date：2020-01-04 13:56
 */
interface CaptureListener {
    fun takePictures()
    fun recordShort(time: Long)
    fun recordStart()
    fun recordEnd(time: Long)
    fun recordZoom(zoom: Float)
    fun recordError()
}