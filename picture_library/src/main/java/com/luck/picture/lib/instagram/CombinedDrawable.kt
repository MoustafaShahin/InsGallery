/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */
package com.luck.picture.lib.instagram

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

class CombinedDrawable : Drawable, Drawable.Callback {
    private var background: Drawable
    private var icon: Drawable?
    private var left = 0
    private var top = 0
    private var iconWidth = 0
    private var iconHeight = 0
    private var backWidth = 0
    private var backHeight = 0
    private var offsetX = 0
    private var offsetY = 0
    private var fullSize = false

    constructor(backgroundDrawable: Drawable, iconDrawable: Drawable?, leftOffset: Int, topOffset: Int) {
        background = backgroundDrawable
        icon = iconDrawable
        left = leftOffset
        top = topOffset
        if (iconDrawable != null) {
            iconDrawable.callback = this
        }
    }

    fun setIconSize(width: Int, height: Int) {
        iconWidth = width
        iconHeight = height
    }

    constructor(backgroundDrawable: Drawable, iconDrawable: Drawable?) {
        background = backgroundDrawable
        icon = iconDrawable
        if (iconDrawable != null) {
            iconDrawable.callback = this
        }
    }

    fun setCustomSize(width: Int, height: Int) {
        backWidth = width
        backHeight = height
    }

    fun setIconOffset(x: Int, y: Int) {
        offsetX = x
        offsetY = y
    }

    fun getIcon(): Drawable? {
        return icon
    }

    fun getBackground(): Drawable {
        return background
    }

    fun setFullsize(value: Boolean) {
        fullSize = value
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        icon!!.colorFilter = colorFilter
    }

    override fun isStateful(): Boolean {
        return icon!!.isStateful
    }

    override fun setState(stateSet: IntArray): Boolean {
        icon!!.state = stateSet
        return true
    }

    override fun getState(): IntArray {
        return icon!!.state
    }

    override fun onStateChange(state: IntArray): Boolean {
        return true
    }

    override fun jumpToCurrentState() {
        icon!!.jumpToCurrentState()
    }

    override fun getConstantState(): ConstantState? {
        return icon!!.constantState
    }

    override fun draw(canvas: Canvas) {
        background.bounds = bounds
        background.draw(canvas)
        if (icon != null) {
            val x: Int
            val y: Int
            if (fullSize) {
                icon!!.bounds = bounds
            } else {
                if (iconWidth != 0) {
                    x = bounds.centerX() - iconWidth / 2 + left + offsetX
                    y = bounds.centerY() - iconHeight / 2 + top + offsetY
                    icon!!.setBounds(x, y, x + iconWidth, y + iconHeight)
                } else {
                    x = bounds.centerX() - icon!!.intrinsicWidth / 2 + left
                    y = bounds.centerY() - icon!!.intrinsicHeight / 2 + top
                    icon!!.setBounds(x, y, x + icon!!.intrinsicWidth, y + icon!!.intrinsicHeight)
                }
            }
            icon!!.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        icon!!.alpha = alpha
        background.alpha = alpha
    }

    override fun getIntrinsicWidth(): Int {
        return if (backWidth != 0) backWidth else background.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return if (backHeight != 0) backHeight else background.intrinsicHeight
    }

    override fun getMinimumWidth(): Int {
        return if (backWidth != 0) backWidth else background.minimumWidth
    }

    override fun getMinimumHeight(): Int {
        return if (backHeight != 0) backHeight else background.minimumHeight
    }

    override fun getOpacity(): Int {
        return icon!!.opacity
    }

    override fun invalidateDrawable(who: Drawable) {
        invalidateSelf()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        scheduleSelf(what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        unscheduleSelf(what)
    }
}