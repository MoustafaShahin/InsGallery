package com.luck.picture.lib.instagram.adapter

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import com.luck.picture.lib.instagram.CombinedDrawable

/**
 * ================================================
 * Created by JessYan on 2020/6/10 10:25
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class MediaItemView(context: Context) : FrameLayout(context) {
    private val mImageView: ImageView
    private val mIconView: ImageView
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(heightMeasureSpec) - ScreenUtils.dip2px(getContext(), 75)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
        mIconView.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop: Int = (getMeasuredHeight() - mImageView.measuredHeight) / 2
        var viewLeft: Int = (getMeasuredWidth() - mImageView.measuredWidth) / 2
        mImageView.layout(viewLeft, viewTop, viewLeft + mImageView.measuredWidth, viewTop + mImageView.measuredHeight)
        viewTop = viewTop + mImageView.measuredHeight - ScreenUtils.dip2px(getContext(), 12) - mIconView.measuredHeight
        viewLeft = ScreenUtils.dip2px(getContext(), 12)
        mIconView.layout(viewLeft, viewTop, viewLeft + mIconView.measuredWidth, viewTop + mIconView.measuredHeight)
    }

    fun setImage(bitmap: Bitmap?) {
        mImageView.setImageBitmap(bitmap)
    }

    init {
        mImageView = ImageView(context)
        mImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(mImageView)
        mIconView = ImageView(context)
        val iconDrawable: CombinedDrawable = InstagramUtils.createCircleDrawableWithIcon(context, ScreenUtils.dip2px(context, 30), R.drawable.discover_filter)
        InstagramUtils.setCombinedDrawableColor(iconDrawable, Color.BLACK, true)
        mIconView.setImageDrawable(iconDrawable)
        addView(mIconView)
    }
}