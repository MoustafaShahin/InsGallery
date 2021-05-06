package com.luck.picture.lib.camera.listener

import java.io.File

/**
 * @author：luck
 * @date：2020-01-04 13:38
 * @describe：相机回调监听
 */
interface CameraListener {
    /**
     * 拍照成功返回
     *
     * @param file
     */
    fun onPictureSuccess(file: File)

    /**
     * 录像成功返回
     *
     * @param file
     */
    fun onRecordSuccess(file: File)

    /**
     * 使用相机出错
     *
     *
     */
    fun onError(videoCaptureError: Int, message: String?, cause: Throwable?)
}