package com.luck.picture.lib.immersive

import android.app.Activity
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.Window
import androidx.annotation.ColorInt

/**
 * @author：luck
 * @date：2019-11-25 20:58
 * @describe：NavBar工具类
 */
object NavBarUtils {
    /**
     * 动态设置 NavBar 色值
     *
     * @param activity
     * @param color
     */
    fun setNavBarColor(activity: Activity, @ColorInt color: Int) {
        setNavBarColor(activity.window, color)
    }

    fun setNavBarColor(window: Window, @ColorInt color: Int) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = color
        }
    }
}