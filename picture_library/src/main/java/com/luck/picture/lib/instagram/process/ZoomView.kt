package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/7/9 11:26
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class ZoomView(context: Context?) : AppCompatImageView(context) {
    private val mPaint: Paint
    private var mBitmap: Bitmap? = null
    fun setBitmap(bitmap: Bitmap?) {
        mBitmap = bitmap
        invalidate()
    }

    protected override fun onDraw(canvas: Canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0f, 0f, mPaint)
        }
        canvas.drawRect(ScreenUtils.dip2px(getContext(), 1).toFloat(), ScreenUtils.dip2px(getContext(), 1).toFloat(), getMeasuredWidth() - ScreenUtils.dip2px(getContext(), 1), getMeasuredHeight() - ScreenUtils.dip2px(getContext(), 1), mPaint)
    }

    init {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.color = -0x222223
        mPaint.strokeWidth = ScreenUtils.dip2px(getContext(), 2).toFloat()
    }
}