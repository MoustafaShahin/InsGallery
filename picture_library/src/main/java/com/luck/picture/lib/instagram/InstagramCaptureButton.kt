package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/**
 * ================================================
 * Created by JessYan on 2020/4/17 11:41
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramCaptureButton(context: Context?) : View(context) {
    private val mPaint: Paint
    private var outsideColor = NORMAL_COLOR
    private val insideColor = Color.WHITE
    private var center_x = 0f
    private var center_y = 0f
    private var buttonRadius = 0f
    private var buttonOutsideRadius = 0f
    private var buttonInsideRadius = 0f
    private var isPress = false
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        center_x = measuredWidth / 2f
        center_y = measuredHeight / 2f
        buttonRadius = measuredWidth / 2f
        buttonOutsideRadius = buttonRadius
        buttonInsideRadius = buttonRadius * 0.65f
    }

    fun pressButton(isPress: Boolean) {
        if (this.isPress == isPress) {
            return
        }
        this.isPress = isPress
        outsideColor = if (isPress) {
            PRESS_COLOR
        } else {
            NORMAL_COLOR
        }
        invalidate()
    }

    fun isPress(): Boolean {
        return isPress
    }

    override fun onDraw(canvas: Canvas) {
        mPaint.color = outsideColor
        canvas.drawCircle(center_x, center_y, buttonOutsideRadius, mPaint)
        mPaint.color = insideColor
        canvas.drawCircle(center_x, center_y, buttonInsideRadius, mPaint)
    }

    companion object {
        private const val PRESS_COLOR = -0x555556
        private const val NORMAL_COLOR = -0x232324
    }

    init {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    }
}