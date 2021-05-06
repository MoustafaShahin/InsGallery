package com.luck.picture.lib.observable

import com.luck.picture.lib.entity.LocalMedia
import java.util.*

/**
 * @author：luck
 * @date：2017-1-12 21:30
 * @describe：解决预览时传值过大问题
 */
class ImagesObservable {
    private var data: MutableList<LocalMedia>? = null

    /**
     * 存储图片用于预览时用
     *
     * @param data
     */
    fun savePreviewMediaData(data: MutableList<LocalMedia>?) {
        this.data = data
    }

    /**
     * 读取预览的图片
     */
    fun readPreviewMediaData(): List<LocalMedia> {
        return if (data == null) ArrayList<LocalMedia>() else data!!
    }

    /**
     * 清空预览的图片
     */
    fun clearPreviewMediaData() {
        if (data != null) {
            data!!.clear()
        }
    }

    companion object {
        private var sObserver: ImagesObservable? = null
        fun getInstance(): ImagesObservable? {
            if (sObserver == null) {
                synchronized(ImagesObservable::class.java) {
                    if (sObserver == null) {
                        sObserver = ImagesObservable()
                    }
                }
            }
            return sObserver
        }
    }
}