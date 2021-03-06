package com.luck.picture.lib.camera.view

import android.content.Context
import android.graphics.*
import android.view.View

/**
 * =====================================
 * 作    者: 陈嘉桐 445263848@qq.com
 * 版    本：1.0.4
 * 创建日期：2017/4/26
 * 描    述：拍照或录制完成后弹出的确认和返回按钮
 * =====================================
 */
class TypeButton : View {
    private var button_type = 0
    private var button_size = 0
    private var center_X = 0f
    private var center_Y = 0f
    private var button_radius = 0f
    private var mPaint: Paint? = null
    private var path: Path? = null
    private var strokeWidth = 0f
    private var index = 0f
    private var rectF: RectF? = null

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, type: Int, size: Int) : super(context) {
        button_type = type
        button_size = size
        button_radius = size / 2.0f
        center_X = size / 2.0f
        center_Y = size / 2.0f
        mPaint = Paint()
        path = Path()
        strokeWidth = size / 50f
        index = button_size / 12f
        rectF = RectF(center_X, center_Y - index, center_X + index * 2, center_Y + index)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(button_size, button_size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //如果类型为取消，则绘制内部为返回箭头
        if (button_type == TYPE_CANCEL) {
            mPaint!!.isAntiAlias = true
            mPaint!!.color = -0x11232324
            mPaint!!.style = Paint.Style.FILL
            canvas.drawCircle(center_X, center_Y, button_radius, mPaint!!)
            mPaint!!.color = Color.BLACK
            mPaint!!.style = Paint.Style.STROKE
            mPaint!!.strokeWidth = strokeWidth
            path!!.moveTo(center_X - index / 7, center_Y + index)
            path!!.lineTo(center_X + index, center_Y + index)
            path!!.arcTo(rectF, 90f, -180f)
            path!!.lineTo(center_X - index, center_Y - index)
            canvas.drawPath(path!!, mPaint!!)
            mPaint!!.style = Paint.Style.FILL
            path!!.reset()
            path!!.moveTo(center_X - index, (center_Y - index * 1.5).toFloat())
            path!!.lineTo(center_X - index, (center_Y - index / 2.3).toFloat())
            path!!.lineTo((center_X - index * 1.6).toFloat(), center_Y - index)
            path!!.close()
            canvas.drawPath(path!!, mPaint!!)
        }
        //如果类型为确认，则绘制绿色勾
        if (button_type == TYPE_CONFIRM) {
            mPaint!!.isAntiAlias = true
            mPaint!!.color = -0x1
            mPaint!!.style = Paint.Style.FILL
            canvas.drawCircle(center_X, center_Y, button_radius, mPaint!!)
            mPaint!!.isAntiAlias = true
            mPaint!!.style = Paint.Style.STROKE
            mPaint!!.color = -0xff3400
            mPaint!!.strokeWidth = strokeWidth
            path!!.moveTo(center_X - button_size / 6f, center_Y)
            path!!.lineTo(center_X - button_size / 21.2f, center_Y + button_size / 7.7f)
            path!!.lineTo(center_X + button_size / 4.0f, center_Y - button_size / 8.5f)
            path!!.lineTo(center_X - button_size / 21.2f, center_Y + button_size / 9.4f)
            path!!.close()
            canvas.drawPath(path!!, mPaint!!)
        }
    }

    companion object {
        const val TYPE_CANCEL = 0x001
        const val TYPE_CONFIRM = 0x002
    }
}