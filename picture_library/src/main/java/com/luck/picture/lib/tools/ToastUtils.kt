package com.luck.picture.lib.tools

import android.content.Context
import android.widget.Toast

/**
 * @author：luck
 * @data：2018/3/28 下午4:10
 * @描述: Toast工具类
 */
object ToastUtils {
    fun s(context: Context, s: String?) {
        if (!isShowToast()) {
            Toast.makeText(context.applicationContext, s, Toast.LENGTH_SHORT)
                    .show()
        }
    }

    /**
     * Prevent continuous click, jump two pages
     */
    private var lastToastTime: Long = 0
    private const val TIME: Long = 1500
    fun isShowToast(): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastToastTime < TIME) {
            return true
        }
        lastToastTime = time
        return false
    }
}