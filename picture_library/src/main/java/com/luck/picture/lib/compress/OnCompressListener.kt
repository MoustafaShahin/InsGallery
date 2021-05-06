package com.luck.picture.lib.compress

import com.luck.picture.lib.entity.LocalMedia

interface OnCompressListener {
    /**
     * Fired when the compression is started, override to handle in your own code
     */
    fun onStart()

    /**
     * Fired when a compression returns successfully, override to handle in your own code
     */
    fun onSuccess(list: List<LocalMedia?>?)

    /**
     * Fired when a compression fails to complete, override to handle in your own code
     */
    fun onError(e: Throwable?)
}