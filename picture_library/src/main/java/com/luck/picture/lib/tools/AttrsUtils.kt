package com.luck.picture.lib.tools

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue

/**
 * @author：luck
 * @data：2018/3/28 下午1:00
 * @描述: 动态获取attrs
 */
object AttrsUtils {
    /**
     * get attrs color
     *
     * @param context
     * @param attr
     * @return
     */
    fun getTypeValueColor(context: Context, attr: Int): Int {
        try {
            val typedValue = TypedValue()
            val attribute = intArrayOf(attr)
            val array = context.obtainStyledAttributes(typedValue.resourceId, attribute)
            val color = array.getColor(0, 0)
            array.recycle()
            return color
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * attrs status color or black
     *
     * @param context
     * @param attr
     * @return
     */
    fun getTypeValueBoolean(context: Context, attr: Int): Boolean {
        try {
            val typedValue = TypedValue()
            val attribute = intArrayOf(attr)
            val array = context.obtainStyledAttributes(typedValue.resourceId, attribute)
            val statusFont = array.getBoolean(0, false)
            array.recycle()
            return statusFont
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * attrs drawable
     *
     * @param context
     * @param attr
     * @return
     */
    fun getTypeValueDrawable(context: Context, attr: Int): Drawable? {
        try {
            val typedValue = TypedValue()
            val attribute = intArrayOf(attr)
            val array = context.obtainStyledAttributes(typedValue.resourceId, attribute)
            val drawable = array.getDrawable(0)
            array.recycle()
            return drawable
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}