package com.luck.picture.lib.listener

/**
 * @author：luck
 * @date：2020-01-15 14:38
 * @describe：Custom video playback callback
 */
interface OnVideoSelectedPlayCallback<T> {
    /**
     * Play the video
     *
     * @param data
     */
    fun startPlayVideo(data: T)
}