package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

/**
 * ================================================
 * Created by JessYan on 2020/4/11 17:06
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
interface Page {
    fun getView(context: Context?): View?
    fun refreshData(context: Context?)
    fun init(position: Int, parent: ViewGroup?)
    fun onResume() {}
    fun onPause() {}
    fun onDestroy() {}
    fun getTitle(context: Context?): String
    fun disallowInterceptTouchRect(): Rect? {
        return null
    }
}