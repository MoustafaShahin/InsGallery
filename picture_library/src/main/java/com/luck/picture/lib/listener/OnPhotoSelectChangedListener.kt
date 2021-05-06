package com.luck.picture.lib.listener

/**
 * @author：luck
 * @date：2020-03-26 10:34
 * @describe：OnPhotoSelectChangedListener
 */
interface OnPhotoSelectChangedListener<T> {
    /**
     * Photo callback
     */
    fun onTakePhoto()

    /**
     * Selected LocalMedia callback
     *
     * @param data
     */
    fun onChange(data: List<T>?)

    /**
     * Image preview callback
     *
     * @param data
     * @param position
     */
    fun onPictureClick(data: T, position: Int)
}