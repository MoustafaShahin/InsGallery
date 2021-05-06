package com.luck.picture.lib.widget

import android.content.*
import android.util.*
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * @author：luck
 * @date：2016-12-31 22:12
 * @describe：PreviewViewPager
 */
class PreviewViewPager : ViewPager {
    private var helper: MyViewPageHelper? = null

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        helper = MyViewPageHelper(this)
    }

    override fun setCurrentItem(item: Int) {
        setCurrentItem(item, true)
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        val scroller: MScroller = helper!!.getScroller()
        if (Math.abs(currentItem - item) > 1) {
            scroller!!.setNoDuration(true)
            super.setCurrentItem(item, smoothScroll)
            scroller!!.setNoDuration(false)
        } else {
            scroller!!.setNoDuration(false)
            super.setCurrentItem(item, smoothScroll)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.dispatchTouchEvent(ev)
        } catch (ignored: IllegalArgumentException) {
        } catch (ignored: ArrayIndexOutOfBoundsException) {
        }
        return false
    }
}