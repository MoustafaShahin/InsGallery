package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/6/29 17:38
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class VideoRulerView(context: Context?, private val mDuration: Long) : View(context) {
    private val mPaint: Paint
    private val mPaddingLeft: Int
    private val mPaddingRight: Int
    private val mShortLineWidth: Int
    private val mLongLineWidth: Int
    private val mShortLineHeight: Int
    private val mLongLineHeight: Int
    private val mTextPaint: TextPaint
    private var mLineCount = 0
    private var mInterval = 0f
    private var mStep = 0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mLineCount = if (mDuration > 60000) 60 else (mDuration / 1000).toInt()
        if (mDuration < 15000) {
            mInterval = Math.round(mDuration / 1875f) * ((measuredWidth - mPaddingLeft - mPaddingRight) / 8f) / mLineCount
            mLineCount = Math.round((measuredWidth - mPaddingLeft) / mInterval)
        } else {
            mInterval = (measuredWidth - mPaddingLeft - mPaddingRight) * 1f / mLineCount
        }
        if (mDuration > 60000) {
            mLineCount = (mDuration / 1000).toInt()
            mInterval = Math.round(mDuration / 7500f) * ((measuredWidth - mPaddingLeft - mPaddingRight) / 8f + 1) / mLineCount
        }
        mStep = if (mDuration > 30000) 10 else 5
    }

    override fun onDraw(canvas: Canvas) {
        drawLongLineAndText(canvas, mPaddingLeft.toFloat(), 0)
        for (i in 0 until mLineCount) {
            val index = i + 1
            val left = mPaddingLeft + index * 1f * mInterval
            if (index % mStep == 0) {
                drawLongLineAndText(canvas, left, index)
            } else {
                drawShortLine(canvas, left)
            }
        }
    }

    fun getWidthByScrollX(scrollX: Int): Int {
        return scrollX + getRangWidth()
    }

    fun getRangWidth(): Int {
        return measuredWidth - mPaddingLeft - mPaddingRight
    }

    fun getInterval(): Float {
        return mInterval
    }

    private fun drawShortLine(canvas: Canvas, left: Float) {
        mPaint.strokeWidth = mShortLineWidth.toFloat()
        canvas.drawLine(left, (measuredHeight - mShortLineHeight).toFloat(), left, measuredHeight.toFloat(), mPaint)
    }

    private fun drawLongLineAndText(canvas: Canvas, left: Float, time: Int) {
        mPaint.strokeWidth = mLongLineWidth.toFloat()
        canvas.drawLine(left, (measuredHeight - mLongLineHeight).toFloat(), left, measuredHeight.toFloat(), mPaint)
        val text: String
        text = if (time < 60) {
            String.format(":%02d", time)
        } else {
            String.format("%d:%02d", time / 60, time % 60)
        }
        canvas.drawText(text, left - mTextPaint.measureText(text) / 2, (measuredHeight - mLongLineHeight - ScreenUtils.dip2px(context, 5)).toFloat(), mTextPaint)
    }

    init {
        mPaddingLeft = ScreenUtils.dip2px(context!!, 20)
        mPaddingRight = ScreenUtils.dip2px(context, 20)
        mLongLineWidth = ScreenUtils.dip2px(context, 1)
        mShortLineWidth = 1
        mLongLineHeight = ScreenUtils.dip2px(context, 90)
        mShortLineHeight = mLongLineHeight / 2
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = ContextCompat.getColor(context, R.color.picture_color_light_grey)
        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.setTextSize(ScreenUtils.sp2px(context, 12))
        mTextPaint.setColor(ContextCompat.getColor(context, R.color.picture_color_light_grey))
    }
}