package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/4/21 15:13
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramRecordIndicator(context: Context, config: PictureSelectionConfig?) : FrameLayout(context) {
    private var mRecordedTime = 0
    private val mTimeView: TextView
    private val mIndicatorView: IndicatorView
    private var mIndicationrAnimation: ObjectAnimator? = null
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = ScreenUtils.dip2px(getContext(), 20)
        mTimeView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        mIndicatorView.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 8), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 8), MeasureSpec.EXACTLY))
        setMeasuredDimension(mTimeView.getMeasuredWidth() + mIndicatorView.measuredWidth * 2, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop: Int = (getMeasuredHeight() - mIndicatorView.measuredHeight) / 2
        var viewLeft = 0
        mIndicatorView.layout(viewLeft, viewTop, viewLeft + mIndicatorView.measuredWidth, viewTop + mIndicatorView.measuredHeight)
        viewTop = (getMeasuredHeight() - mTimeView.getMeasuredHeight()) / 2
        viewLeft = mIndicatorView.measuredWidth * 2
        mTimeView.layout(viewLeft, viewTop, viewLeft + mTimeView.getMeasuredWidth(), viewTop + mTimeView.getMeasuredHeight())
    }

    fun setRecordedTime(recordedTime: Int) {
        if (recordedTime < 0) {
            return
        }
        mRecordedTime = recordedTime
        if (recordedTime == 0) {
            mTimeView.setText("0:00")
        } else {
            val minutes = recordedTime / 60
            val second = recordedTime % 60
            mTimeView.setText(String.format("%d:%02d", minutes, second))
        }
    }

    fun playIndicatorAnimation() {
        if (mIndicationrAnimation == null) {
            mIndicationrAnimation = ObjectAnimator.ofFloat(mIndicatorView, "alpha", 1.0f, 0f).setDuration(500)
            mIndicationrAnimation.setRepeatMode(ValueAnimator.REVERSE)
            mIndicationrAnimation.setRepeatCount(ValueAnimator.INFINITE)
        }
        mIndicationrAnimation.start()
    }

    fun stopIndicatorAnimation() {
        if (mIndicationrAnimation != null && mIndicationrAnimation.isRunning()) {
            mIndicationrAnimation.end()
        }
    }

    fun release() {
        if (mIndicationrAnimation != null && mIndicationrAnimation.isRunning()) {
            mIndicationrAnimation.cancel()
        }
        mIndicationrAnimation = null
    }

    private inner class IndicatorView(context: Context?, radius: Int) : View(context) {
        private val mPaint: Paint
        private var mRadius: Int
        fun setRadius(radius: Int) {
            mRadius = radius
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(measuredWidth / 2f, measuredHeight / 2f, mRadius.toFloat(), mPaint)
        }

        init {
            mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mPaint.color = -0x5fecf7
            mPaint.style = Paint.Style.FILL
            mRadius = radius
        }
    }

    init {
        mTimeView = TextView(context)
        mTimeView.setTextSize(14f)
        mTimeView.setGravity(Gravity.CENTER_VERTICAL)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK || config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mTimeView.setTextColor(Color.WHITE)
        } else {
            mTimeView.setTextColor(Color.BLACK)
        }
        addView(mTimeView)
        mIndicatorView = IndicatorView(context, ScreenUtils.dip2px(context, 4))
        addView(mIndicatorView)
    }
}