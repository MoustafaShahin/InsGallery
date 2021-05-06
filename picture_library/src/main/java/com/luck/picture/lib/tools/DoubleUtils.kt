package com.luck.picture.lib.tools

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.lib.tool
 * email：893855882@qq.com
 * data：2017/5/25
 */
object DoubleUtils {
    /**
     * Prevent continuous click, jump two pages
     */
    private var lastClickTime: Long = 0
    private const val TIME: Long = 800
    fun isFastDoubleClick(): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastClickTime < TIME) {
            return true
        }
        lastClickTime = time
        return false
    }
}