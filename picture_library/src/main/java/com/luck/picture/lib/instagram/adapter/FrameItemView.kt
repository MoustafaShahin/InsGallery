package com.luck.picture.lib.instagram.adapter

import android.content.Context
import android.widget.ImageView
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/6/10 10:25
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class FrameItemView(context: Context) : FrameLayout(context) {
    private val mImageView: ImageView
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = (ScreenUtils.getScreenWidth(getContext()) - ScreenUtils.dip2px(getContext(), 40)) / 8
        val height: Int = ScreenUtils.dip2px(getContext(), 90)
        mImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val viewTop = 0
        val viewLeft = 0
        mImageView.layout(viewLeft, viewTop, viewLeft + mImageView.measuredWidth, viewTop + mImageView.measuredHeight)
    }

    fun setImage(bitmap: Bitmap?) {
        mImageView.setImageBitmap(bitmap)
    }

    fun setImageDrawable(drawable: Drawable?) {
        mImageView.setImageDrawable(drawable)
    }

    fun setImageResource(resId: Int) {
        mImageView.setImageResource(resId)
    }

    init {
        mImageView = ImageView(context)
        mImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(mImageView)
    }
}