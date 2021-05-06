package com.luck.picture.lib.instagram.adapter

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import com.luck.picture.lib.instagram.InsGallery

/**
 * ================================================
 * Created by JessYan on 2020/6/2 15:35
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class FilterItemView(context: Context, config: PictureSelectionConfig) : FrameLayout(context) {
    private val mTitleView: TextView
    private val mConfig: PictureSelectionConfig
    private val mImageView: ImageView
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = ScreenUtils.dip2px(getContext(), 100)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mTitleView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        mImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewLeft: Int = (getMeasuredWidth() - mTitleView.getMeasuredWidth()) / 2
        var viewTop: Int = (getMeasuredHeight() - mTitleView.getMeasuredHeight() - mImageView.measuredHeight - ScreenUtils.dip2px(getContext(), 5)) / 2
        mTitleView.layout(viewLeft, viewTop, viewLeft + mTitleView.getMeasuredWidth(), viewTop + mTitleView.getMeasuredHeight())
        viewLeft = 0
        viewTop = viewTop + ScreenUtils.dip2px(getContext(), 5) + mTitleView.getMeasuredHeight()
        mImageView.layout(viewLeft, viewTop, viewLeft + mImageView.measuredWidth, viewTop + mImageView.measuredHeight)
    }

    fun selection(isSelection: Boolean) {
        if (isSelection) {
            if (mConfig.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
                mTitleView.setTextColor(Color.parseColor("#262626"))
            } else {
                mTitleView.setTextColor(Color.parseColor("#fafafa"))
            }
            setTranslationY(-ScreenUtils.dip2px(getContext(), 10))
        } else {
            setTranslationY(0f)
            mTitleView.setTextColor(Color.parseColor("#999999"))
        }
    }

    fun refreshFilter(filterType: FilterType, bitmap: Bitmap?, position: Int, selectionPosition: Int) {
        if (position == selectionPosition) {
            selection(true)
        } else {
            selection(false)
        }
        mTitleView.setText(filterType.getName())
        mImageView.setImageBitmap(bitmap)
    }

    init {
        mConfig = config
        mTitleView = TextView(context)
        mTitleView.setTextColor(Color.parseColor("#999999"))
        mTitleView.setTextSize(12f)
        mTitleView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        mTitleView.setGravity(Gravity.CENTER)
        addView(mTitleView)
        mImageView = ImageView(context)
        mImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(mImageView)
    }
}