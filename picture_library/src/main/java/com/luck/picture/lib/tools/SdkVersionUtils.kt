package com.luck.picture.lib.tools

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES

/**
 * @author：luck
 * @date：2019-07-17 15:12
 * @describe：Android Sdk版本判断
 */
object SdkVersionUtils {
    /**
     * 判断是否是Android Q版本
     *
     * @return
     */
    fun checkedAndroid_Q(): Boolean {
        return VERSION.SDK_INT >= VERSION_CODES.Q
    }
}