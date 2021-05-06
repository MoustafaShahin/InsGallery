package com.luck.picture.lib.listener

/**
 * @author：luck
 * @date：2020-04-16 12:42
 * @describe：OnQueryMediaResultListener
 */
interface OnQueryDataResultListener<T> {
    /**
     * Query to complete The callback listener
     *
     * @param data        The data source
     * @param currentPage The page number
     * @param isHasMore   Is there more
     */
    fun onComplete(data: List<T>?, currentPage: Int, isHasMore: Boolean)
}