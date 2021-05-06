package com.luck.picture.lib.listener

/**
 * @author：luck
 * @date：2020-01-03 16:43
 * @describe：Image load complete callback
 */
interface OnImageCompleteCallback {
    /**
     * Start loading
     */
    fun onShowLoading()

    /**
     * Stop loading
     */
    fun onHideLoading()
}