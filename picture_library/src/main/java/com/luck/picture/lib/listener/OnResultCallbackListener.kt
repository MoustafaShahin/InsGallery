package com.luck.picture.lib.listener

/**
 * @author：luck
 * @date：2020-01-14 17:08
 * @describe：onResult Callback Listener
 */
interface OnResultCallbackListener<T> {
    /**
     * return LocalMedia result
     *
     * @param result
     */
    fun onResult(result: List<T>?)

    /**
     * Cancel
     */
    fun onCancel()
}