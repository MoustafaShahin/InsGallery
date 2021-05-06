package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.instagram.InsGallery

/**
 * ================================================
 * Created by JessYan on 2020/5/29 11:51
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramTitleBar(context: Context, config: PictureSelectionConfig, mediaType: MediaType?) : FrameLayout(context) {
    private val mLeftView: ImageView
    private val mCenterView: ImageView
    private val mRightView: TextView
    private var mClickListener: OnTitleBarItemOnClickListener? = null
    fun setRightViewText(text: String?) {
        mRightView.setText(text)
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = ScreenUtils.dip2px(getContext(), 48)
        mLeftView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        if (mCenterView.visibility == View.VISIBLE) {
            mCenterView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        }
        mRightView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop: Int = (getMeasuredHeight() - mLeftView.measuredHeight) / 2
        var viewLeft = 0
        mLeftView.layout(viewLeft, viewTop, viewLeft + mLeftView.measuredWidth, viewTop + mLeftView.measuredHeight)
        if (mCenterView.visibility == View.VISIBLE) {
            viewTop = (getMeasuredHeight() - mCenterView.measuredHeight) / 2
            viewLeft = (getMeasuredWidth() - mCenterView.measuredWidth) / 2
            mCenterView.layout(viewLeft, viewTop, viewLeft + mCenterView.measuredWidth, viewTop + mCenterView.measuredHeight)
        }
        viewTop = (getMeasuredHeight() - mRightView.getMeasuredHeight()) / 2
        viewLeft = getMeasuredWidth() - mRightView.getMeasuredWidth()
        mRightView.layout(viewLeft, viewTop, viewLeft + mRightView.getMeasuredWidth(), viewTop + mRightView.getMeasuredHeight())
    }

    fun setClickListener(clickListener: OnTitleBarItemOnClickListener?) {
        mClickListener = clickListener
    }

    interface OnTitleBarItemOnClickListener {
        fun onLeftViewClick()
        fun onCenterViewClick(view: ImageView?)
        fun onRightViewClick()
    }

    init {
        mLeftView = ImageView(context)
        mLeftView.setImageResource(R.drawable.discover_return)
        mLeftView.setPadding(ScreenUtils.dip2px(context, 15), 0, ScreenUtils.dip2px(context, 15), 0)
        mLeftView.setOnClickListener { v: View? ->
            if (mClickListener != null) {
                mClickListener!!.onLeftViewClick()
            }
        }
        addView(mLeftView)
        mCenterView = ImageView(context)
        mCenterView.setPadding(ScreenUtils.dip2px(context, 10), 0, ScreenUtils.dip2px(context, 10), 0)
        mCenterView.setOnClickListener { v: View? ->
            if (mClickListener != null) {
                mClickListener!!.onCenterViewClick(mCenterView)
            }
        }
        addView(mCenterView)
        when (mediaType) {
            SINGLE_VIDEO -> mCenterView.setImageResource(R.drawable.discover_volume_off)
            else -> mCenterView.visibility = View.GONE
        }
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
            mLeftView.setColorFilter(PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.picture_color_black), PorterDuff.Mode.MULTIPLY))
            mCenterView.setColorFilter(PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.picture_color_black), PorterDuff.Mode.MULTIPLY))
        }
        mRightView = TextView(context)
        mRightView.setPadding(ScreenUtils.dip2px(context, 10), 0, ScreenUtils.dip2px(context, 10), 0)
        val textColor: Int
        textColor = if (config.style.pictureRightDefaultTextColor !== 0) {
            config.style.pictureRightDefaultTextColor
        } else {
            if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
                ContextCompat.getColor(context, R.color.picture_color_1766FF)
            } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
                Color.parseColor("#2FA6FF")
            } else {
                ContextCompat.getColor(context, R.color.picture_color_1766FF)
            }
        }
        mRightView.setTextColor(textColor)
        mRightView.setTextSize(14f)
        mRightView.setText(context.getString(R.string.next))
        mRightView.setGravity(Gravity.CENTER)
        mRightView.setOnClickListener(View.OnClickListener { v: View? ->
            if (mClickListener != null) {
                mClickListener!!.onRightViewClick()
            }
        })
        addView(mRightView)
    }
}