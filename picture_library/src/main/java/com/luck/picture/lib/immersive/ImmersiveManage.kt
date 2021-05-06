package com.luck.picture.lib.immersive

import android.graphics.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.*
import androidx.appcompat.app.AppCompatActivity

/**
 * @author：luck
 * @data：2018/3/28 下午1:00
 * @描述: 沉浸式相关
 */
object ImmersiveManage {
    /**
     * 注意：使用最好将布局xml 跟布局加入    android:fitsSystemWindows="true" ，这样可以避免有些手机上布局顶边的问题
     *
     * @param baseActivity        这个会留出来状态栏和底栏的空白
     * @param statusBarColor      状态栏的颜色
     * @param navigationBarColor  导航栏的颜色
     * @param isDarkStatusBarIcon 状态栏图标颜色是否是深（黑）色  false状态栏图标颜色为白色
     */
    fun immersiveAboveAPI23(baseActivity: AppCompatActivity, statusBarColor: Int, navigationBarColor: Int, isDarkStatusBarIcon: Boolean) {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            immersiveAboveAPI23(baseActivity, false, false, statusBarColor, navigationBarColor, isDarkStatusBarIcon)
        }
    }

    /**
     * @param baseActivity
     * @param statusBarColor     状态栏的颜色
     * @param navigationBarColor 导航栏的颜色
     */
    fun immersiveAboveAPI23(baseActivity: AppCompatActivity, isMarginStatusBar: Boolean, isMarginNavigationBar: Boolean, statusBarColor: Int, navigationBarColor: Int, isDarkStatusBarIcon: Boolean) {
        try {
            val window = baseActivity.window
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
                //4.4版本及以上 5.0版本及以下
                window.setFlags(
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            } else if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                if (isMarginStatusBar && isMarginNavigationBar) {
                    //5.0版本及以上
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    LightStatusBarUtils.setLightStatusBar(baseActivity, isMarginStatusBar, isMarginNavigationBar, statusBarColor == Color.TRANSPARENT, isDarkStatusBarIcon)
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                } else if (!isMarginStatusBar && !isMarginNavigationBar) {
                    window.requestFeature(Window.FEATURE_NO_TITLE)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    LightStatusBarUtils.setLightStatusBar(baseActivity, isMarginStatusBar, isMarginNavigationBar, statusBarColor == Color.TRANSPARENT, isDarkStatusBarIcon)
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                } else if (!isMarginStatusBar && isMarginNavigationBar) {
                    window.requestFeature(Window.FEATURE_NO_TITLE)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    LightStatusBarUtils.setLightStatusBar(baseActivity, isMarginStatusBar, isMarginNavigationBar, statusBarColor == Color.TRANSPARENT, isDarkStatusBarIcon)
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                } else {
                    //留出来状态栏 不留出来导航栏 没找到办法。。
                    return
                }
                window.statusBarColor = statusBarColor
                window.navigationBarColor = navigationBarColor
            }
        } catch (e: Exception) {
        }
    }
}