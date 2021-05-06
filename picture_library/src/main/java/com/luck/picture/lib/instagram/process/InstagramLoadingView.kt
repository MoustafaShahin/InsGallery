package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/7/30 10:35
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramLoadingView(context: Context?) : View(context) {
    private val mPaint: Paint
    private val mBackRect: RectF
    private val mHeight: Int
    private val mWidth: Int
    private val mProgressWidth: Int
    private val mProgressRect: RectF
    private var mProgress = 0f
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(mWidth, mHeight)
    }

    fun updateProgress(progress: Double) {
        mProgress = Math.round(360 * progress).toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = ContextCompat.getColor(context, R.color.picture_color_a83)
        canvas.drawRoundRect(mBackRect, ScreenUtils.dip2px(context, 5).toFloat(), ScreenUtils.dip2px(context, 5).toFloat(), mPaint)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = ScreenUtils.dip2px(context, 2).toFloat()
        mPaint.color = -0xbbbbbc
        canvas.drawCircle(measuredWidth / 2f, measuredHeight / 2f, mProgressWidth / 2f, mPaint)
        mPaint.color = Color.WHITE
        canvas.drawArc(mProgressRect, -90f, mProgress, false, mPaint)
    }

    init {
        mWidth = ScreenUtils.dip2px(getContext(), 45)
        mHeight = ScreenUtils.dip2px(getContext(), 45)
        mProgressWidth = ScreenUtils.dip2px(getContext(), 25)
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBackRect = RectF(0, 0, mWidth, mHeight)
        val progressLeft = (mWidth - mProgressWidth) / 2f
        val progressTop = (mHeight - mProgressWidth) / 2f
        mProgressRect = RectF(progressLeft, progressTop, progressLeft + mProgressWidth, progressTop + mProgressWidth)
    }
}