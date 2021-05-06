package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils
import java.lang.ref.WeakReference

/**
 * ================================================
 * Created by JessYan on 2020/4/22 14:23
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramRecordProgressBar(context: Context?, config: PictureSelectionConfig?) : View(context) {
    private val mPaint: Paint
    private var mTimer: RecordCountDownTimer? = null
    private var mMaxTime: Long = 0
    private var progress = 0f
    private var defaultIndicator: GradientDrawable? = null
    private var mValueAnimator: ValueAnimator? = null
    private var mUpdateListener: AnimatorUpdateListener? = UpdateListener(this)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = ScreenUtils.dip2px(context, 4)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, progress, measuredHeight.toFloat(), mPaint)
        defaultIndicator.setBounds(progress.toInt(), 0, (progress + ScreenUtils.dip2px(context, 8)) as Int, measuredHeight)
        defaultIndicator.draw(canvas)
    }

    fun setMaxTime(maxTime: Long) {
        mMaxTime = maxTime
        mTimer = RecordCountDownTimer(mMaxTime, 10, this)
    }

    fun startRecord() {
        if (mTimer != null) {
            mTimer.start()
        }
    }

    fun stopRecord() {
        if (mTimer != null) {
            mTimer.cancel()
        }
    }

    fun startRecordAnimation() {
        if (mValueAnimator == null) {
            mValueAnimator = ValueAnimator.ofInt(0, 255)
            mValueAnimator.addUpdateListener(mUpdateListener)
            mValueAnimator.setDuration(500)
            mValueAnimator.setRepeatMode(ValueAnimator.REVERSE)
            mValueAnimator.setRepeatCount(ValueAnimator.INFINITE)
        }
        mValueAnimator.start()
    }

    fun stopRecordAnimation() {
        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.end()
        }
    }

    private fun updateProgress(millisUntilFinished: Long) {
        progress = (mMaxTime - millisUntilFinished) * 1.0f / mMaxTime * measuredWidth
        //        invalidate();
    }

    fun release() {
        if (mValueAnimator != null) {
            mValueAnimator.removeUpdateListener(mUpdateListener)
            if (mValueAnimator.isRunning()) {
                mValueAnimator.cancel()
            }
        }
        if (mTimer != null) {
            mTimer.cancel()
        }
        mUpdateListener = null
        mTimer = null
        mValueAnimator = null
    }

    private class RecordCountDownTimer internal constructor(millisInFuture: Long, countDownInterval: Long, recordProgressBar: InstagramRecordProgressBar) : CountDownTimer(millisInFuture, countDownInterval) {
        private val mRecordProgressBar: WeakReference<InstagramRecordProgressBar>
        override fun onTick(millisUntilFinished: Long) {
            val recordProgressBar = mRecordProgressBar.get()
            recordProgressBar?.updateProgress(millisUntilFinished)
        }

        override fun onFinish() {}

        init {
            mRecordProgressBar = WeakReference(recordProgressBar)
        }
    }

    private class UpdateListener internal constructor(recordProgressBar: InstagramRecordProgressBar) : AnimatorUpdateListener {
        private val mRecordProgressBar: WeakReference<InstagramRecordProgressBar>
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val recordProgressBar = mRecordProgressBar.get()
            if (recordProgressBar != null) {
                recordProgressBar.defaultIndicator.setAlpha(animation.getAnimatedValue() as Int)
                recordProgressBar.invalidate()
            }
        }

        init {
            mRecordProgressBar = WeakReference(recordProgressBar)
        }
    }

    init {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK || config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mPaint.color = Color.WHITE
            defaultIndicator = GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, intArrayOf(Color.WHITE, Color.BLACK))
        } else {
            mPaint.color = Color.BLACK
            defaultIndicator = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.WHITE, Color.BLACK))
        }
    }
}