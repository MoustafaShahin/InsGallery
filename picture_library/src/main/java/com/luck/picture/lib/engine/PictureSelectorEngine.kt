package com.luck.picture.lib.engine

import com.luck.picture.lib.entity.LocalMedia
/**
 * @author：luck
 * @date：2020/4/22 11:36 AM
 * @describe：PictureSelectorEngine
 */
interface PictureSelectorEngine {
    /**
     * Create ImageLoad Engine
     *
     * @return
     */
    fun createEngine(): ImageEngine?

    /**
     * Create Result Listener
     *
     * @return
     */
    fun getResultCallbackListener(): OnResultCallbackListener<LocalMedia?>?
}