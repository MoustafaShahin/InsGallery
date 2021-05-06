package com.luck.picture.lib.widget

import androidx.viewpager.widget.ViewPager

/**
 * @author：luck
 * @date：2020-04-11 14:43
 * @describe：MyViewPageHelper
 */
class MyViewPageHelper(var viewPager: ViewPager) {
    var scroller: MScroller? = null
    fun setCurrentItem(item: Int) {
        setCurrentItem(item, true)
    }

    fun getScroller(): MScroller? {
        return scroller
    }

    fun setCurrentItem(item: Int, smooth: Boolean) {
        val current = viewPager.currentItem
        //如果页面相隔大于1,就设置页面切换的动画的时间为0
        if (Math.abs(current - item) > 1) {
            scroller!!.setNoDuration(true)
            viewPager.setCurrentItem(item, smooth)
            scroller!!.setNoDuration(false)
        } else {
            scroller!!.setNoDuration(false)
            viewPager.setCurrentItem(item, smooth)
        }
    }

    private fun init() {
        scroller = MScroller(viewPager.context)
        val cl = ViewPager::class.java
        try {
            val field = cl.getDeclaredField("mScroller")
            field.isAccessible = true
            //利用反射设置mScroller域为自己定义的MScroller
            field[viewPager] = scroller
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        init()
    }
}