package com.luck.picture.lib.widget

import android.content.Context
import android.view.animation.Interpolator
import android.widget.Scroller

/**
 * @author：luck
 * @date：2020-04-11 14:41
 * @describe：MScroller
 */
class MScroller @JvmOverloads constructor(context: Context?, interpolator: Interpolator? = sInterpolator) : Scroller(context, interpolator) {
    var noDuration = false
    fun setNoDuration(noDuration: Boolean) {
        this.noDuration = noDuration
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        if (noDuration) {
            //界面滑动不需要时间间隔
            super.startScroll(startX, startY, dx, dy, 0)
        } else {
            super.startScroll(startX, startY, dx, dy, duration)
        }
    }

    companion object {
        private val sInterpolator = Interpolator { t: Float ->
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }
}