package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Color
import android.view.View
import com.luck.picture.lib.permissions.PermissionChecker

/**
 * ================================================
 * Created by JessYan on 2020/4/24 17:43
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramCameraEmptyView(context: Context, config: PictureSelectionConfig?) : FrameLayout(context) {
    private val mTitleView: TextView
    private val mContentView: TextView
    private val mActionView: TextView
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mTitleView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.AT_MOST))
        mContentView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.AT_MOST))
        mActionView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.AT_MOST))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop: Int = (getMeasuredHeight() - mContentView.getMeasuredHeight()) / 2
        var viewLeft: Int = (getMeasuredWidth() - mContentView.getMeasuredWidth()) / 2
        mContentView.layout(viewLeft, viewTop, viewLeft + mContentView.getMeasuredWidth(), viewTop + mContentView.getMeasuredHeight())
        viewTop -= ScreenUtils.dip2px(getContext(), 15) + mTitleView.getMeasuredHeight()
        viewLeft = (getMeasuredWidth() - mTitleView.getMeasuredWidth()) / 2
        mTitleView.layout(viewLeft, viewTop, viewLeft + mTitleView.getMeasuredWidth(), viewTop + mTitleView.getMeasuredHeight())
        viewTop = mContentView.getBottom() + ScreenUtils.dip2px(getContext(), 15)
        viewLeft = (getMeasuredWidth() - mActionView.getMeasuredWidth()) / 2
        mActionView.layout(viewLeft, viewTop, viewLeft + mActionView.getMeasuredWidth(), viewTop + mActionView.getMeasuredHeight())
    }

    init {
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            setBackgroundColor(Color.parseColor("#1C1C1E"))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            setBackgroundColor(Color.parseColor("#213040"))
        } else {
            setBackgroundColor(ContextCompat.getColor(context, R.color.picture_color_262626))
        }
        mTitleView = TextView(context)
        mTitleView.setTextSize(20f)
        mTitleView.setTextColor(ContextCompat.getColor(context, R.color.picture_color_c7c7c7))
        mTitleView.setText(R.string.camera_access)
        addView(mTitleView)
        mContentView = TextView(context)
        mContentView.setTextSize(17f)
        mContentView.setTextColor(ContextCompat.getColor(context, R.color.picture_color_light_grey))
        mContentView.setText(R.string.camera_access_content)
        addView(mContentView)
        mActionView = TextView(context)
        mActionView.setTextSize(17f)
        mActionView.setTextColor(ContextCompat.getColor(context, R.color.picture_color_3c98ea))
        mActionView.setText(R.string.enable)
        mActionView.setOnClickListener(View.OnClickListener { v: View? -> PermissionChecker.launchAppDetailsSettings(getContext()) })
        addView(mActionView)
    }
}