package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.luck.picture.lib.instagram.InsGallery

/**
 * ================================================
 * Created by JessYan on 2020/6/11 18:16
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class ProcessAlertView(context: Context, config: PictureSelectionConfig) : FrameLayout(context) {
    private val mTitleView: TextView
    private val mSubtitleView: TextView
    private val mAgreeView: TextView
    private val mCancelView: TextView
    private val mPaint: Paint
    private var mOnAlertListener: onAlertListener? = null
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec) - ScreenUtils.dip2px(getContext(), 60)
        val height: Int = ScreenUtils.dip2px(getContext(), 210)
        mTitleView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        mSubtitleView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        mAgreeView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 50), MeasureSpec.EXACTLY))
        mCancelView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 50), MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop: Int = ScreenUtils.dip2px(getContext(), 30)
        val viewLeft = 0
        mTitleView.layout(viewLeft, viewTop, viewLeft + mTitleView.getMeasuredWidth(), viewTop + mTitleView.getMeasuredHeight())
        viewTop = ScreenUtils.dip2px(getContext(), 10) + mTitleView.getBottom()
        mSubtitleView.layout(viewLeft, viewTop, viewLeft + mSubtitleView.getMeasuredWidth(), viewTop + mSubtitleView.getMeasuredHeight())
        viewTop = getMeasuredHeight() - mCancelView.getMeasuredHeight() - mAgreeView.getMeasuredHeight()
        mAgreeView.layout(viewLeft, viewTop, viewLeft + mAgreeView.getMeasuredWidth(), viewTop + mAgreeView.getMeasuredHeight())
        viewTop = getMeasuredHeight() - mCancelView.getMeasuredHeight()
        mCancelView.layout(viewLeft, viewTop, viewLeft + mCancelView.getMeasuredWidth(), viewTop + mCancelView.getMeasuredHeight())
    }

    protected override fun onDraw(canvas: Canvas) {
        canvas.drawLine(0f, (getMeasuredHeight() - mCancelView.getMeasuredHeight() - mAgreeView.getMeasuredHeight()).toFloat(), getMeasuredWidth().toFloat(), (getMeasuredHeight() - mCancelView.getMeasuredHeight() - mAgreeView.getMeasuredHeight()).toFloat(), mPaint)
        canvas.drawLine(0f, (getMeasuredHeight() - mCancelView.getMeasuredHeight()).toFloat(), getMeasuredWidth().toFloat(), (getMeasuredHeight() - mCancelView.getMeasuredHeight()).toFloat(), mPaint)
    }

    fun setOnAlertListener(onAlertListener: onAlertListener?) {
        mOnAlertListener = onAlertListener
    }

    interface onAlertListener {
        fun onAgree()
        fun onCancel()
    }

    init {
        setWillNotDraw(false)
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
            mPaint.color = Color.parseColor("#dddddd")
        } else {
            mPaint.color = Color.parseColor("#3f3f3f")
        }
        val background: Drawable
        background = if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            InstagramUtils.createRoundRectDrawable(ScreenUtils.dip2px(context, 4), Color.parseColor("#2c2c2c"))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            InstagramUtils.createRoundRectDrawable(ScreenUtils.dip2px(context, 4), Color.parseColor("#18222D"))
        } else {
            InstagramUtils.createRoundRectDrawable(ScreenUtils.dip2px(context, 4), Color.WHITE)
        }
        setBackground(background)
        mTitleView = TextView(context)
        mTitleView.setGravity(Gravity.CENTER)
        mTitleView.setTextSize(18f)
        mTitleView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
            mTitleView.setTextColor(Color.BLACK)
        } else {
            mTitleView.setTextColor(Color.WHITE)
        }
        mTitleView.setText(R.string.discard_edits)
        addView(mTitleView)
        mSubtitleView = TextView(context)
        mSubtitleView.setPadding(ScreenUtils.dip2px(getContext(), 20), 0, ScreenUtils.dip2px(getContext(), 20), 0)
        mSubtitleView.setGravity(Gravity.CENTER)
        mSubtitleView.setTextSize(14f)
        mSubtitleView.setTextColor(Color.GRAY)
        mSubtitleView.setText(R.string.discard_edits_alert)
        addView(mSubtitleView)
        mAgreeView = TextView(context)
        mAgreeView.setGravity(Gravity.CENTER)
        mAgreeView.setTextSize(17f)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mAgreeView.setTextColor(Color.parseColor("#2FA6FF"))
        } else {
            mAgreeView.setTextColor(ContextCompat.getColor(context, R.color.picture_color_1766FF))
        }
        mAgreeView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        mAgreeView.setText(R.string.discard)
        addView(mAgreeView)
        mAgreeView.setOnClickListener(View.OnClickListener { v: View? ->
            if (mOnAlertListener != null) {
                mOnAlertListener!!.onAgree()
            }
        })
        mCancelView = TextView(context)
        mCancelView.setGravity(Gravity.CENTER)
        mCancelView.setTextSize(17f)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
            mCancelView.setTextColor(Color.BLACK)
        } else {
            mCancelView.setTextColor(Color.WHITE)
        }
        mCancelView.setText(R.string.picture_cancel)
        addView(mCancelView)
        mCancelView.setOnClickListener(View.OnClickListener { v: View? ->
            if (mOnAlertListener != null) {
                mOnAlertListener!!.onCancel()
            }
        })
    }
}