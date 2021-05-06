package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils
import java.text.DecimalFormat

class RangeSeekBarView : View {
    private var TextPositionY = 0
    private var paddingTop = 0
    private var mActivePointerId = INVALID_POINTER_ID
    private var mMinShootTime = 3000L
    private var absoluteMinValuePrim = 0.0
    private var absoluteMaxValuePrim = 0.0
    private var normalizedMinValue = 0.0 //点坐标占总长度的比例值，范围从0-1
    private var normalizedMaxValue = 1.0 //点坐标占总长度的比例值，范围从0-1
    private var normalizedMinValueTime = 0.0
    private var normalizedMaxValueTime = 1.0 // normalized：规格化的--点坐标占总长度的比例值，范围从0-1
    private var mScaledTouchSlop = 0

    //    private Bitmap thumbImageLeft;
    //    private Bitmap thumbImageRight;
    //    private Bitmap thumbPressedImage;
    private var paint: Paint? = null
    private var rectPaint: Paint? = null
    private val mVideoTrimTimePaintL = Paint()
    private val mVideoTrimTimePaintR = Paint()
    private val mShadow = Paint()
    private var thumbWidth = 0
    private var thumbHalfWidth = 0f
    private val padding = 0f
    private var mStartPosition: Long = 0
    private var mEndPosition: Long = 0
    private val thumbPaddingTop = 0f
    private var isTouchDown = false
    private var mDownMotionX = 0f
    private var mIsDragging = false
    private var pressedThumb: Thumb? = null
    private var isMin = false
    private var min_width = 1.0 //最小裁剪距离
    private var notifyWhileDragging = false
    private var mRangeSeekBarChangeListener: OnRangeSeekBarChangeListener? = null
    private val whiteColorRes = Color.WHITE
    private var mThumbsCount = 0

    enum class Thumb {
        MIN, MAX
    }

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}
    constructor(context: Context?, absoluteMinValuePrim: Long, absoluteMaxValuePrim: Long, thumbsCount: Int) : super(context) {
        this.absoluteMinValuePrim = absoluteMinValuePrim.toDouble()
        this.absoluteMaxValuePrim = absoluteMaxValuePrim.toDouble()
        mThumbsCount = thumbsCount
        isFocusable = true
        isFocusableInTouchMode = true
        init()
    }

    private fun init() {
        TextPositionY = ScreenUtils.dip2px(context, 7)
        paddingTop = ScreenUtils.dip2px(context, 10)
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop()
        val scaledMinimumFlingVelocity: Int = ViewConfiguration.get(context).getScaledMinimumFlingVelocity()
        val scaledMaximumFlingVelocity: Int = ViewConfiguration.get(context).getScaledMaximumFlingVelocity()
        //        thumbImageLeft = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_thumb_handle);

//        int width = thumbImageLeft.getWidth();
//        int height = thumbImageLeft.getHeight();
        val newWidth: Int = ScreenUtils.dip2px(context, 20)
        //        int newHeight = ScreenUtils.dip2px(getContext(), 55);
//        float scaleWidth = newWidth * 1.0f / width;
//        float scaleHeight = newHeight * 1.0f / height;
//        Matrix matrix = new Matrix();
//        matrix.postScale(scaleWidth, scaleHeight);
//        thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, width, height, matrix, true);
//        thumbImageRight = thumbImageLeft;
//        thumbPressedImage = thumbImageLeft;
        thumbWidth = newWidth
        thumbHalfWidth = (thumbWidth / 2).toFloat()
        val shadowColor = -0x41000000
        mShadow.isAntiAlias = true
        mShadow.color = shadowColor
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rectPaint!!.style = Paint.Style.FILL
        rectPaint!!.color = whiteColorRes
        mVideoTrimTimePaintL.strokeWidth = 3f
        mVideoTrimTimePaintL.setARGB(255, 51, 51, 51)
        mVideoTrimTimePaintL.textSize = 28f
        mVideoTrimTimePaintL.isAntiAlias = true
        mVideoTrimTimePaintL.color = whiteColorRes
        mVideoTrimTimePaintL.textAlign = Paint.Align.LEFT
        mVideoTrimTimePaintR.strokeWidth = 3f
        mVideoTrimTimePaintR.setARGB(255, 51, 51, 51)
        mVideoTrimTimePaintR.textSize = 28f
        mVideoTrimTimePaintR.isAntiAlias = true
        mVideoTrimTimePaintR.color = whiteColorRes
        mVideoTrimTimePaintR.textAlign = Paint.Align.RIGHT
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 300
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec)
        }
        if (mThumbsCount < 8) {
            val frameWidth: Int = (ScreenUtils.getScreenWidth(context) - ScreenUtils.dip2px(context, 40)) / 8
            val maxWidth = (frameWidth * mThumbsCount + thumbWidth).toFloat()
            if (width > maxWidth) {
                width = maxWidth.toInt()
            }
        }
        var height = 120
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bg_middle_left = 0f
        val bg_middle_right = (width - paddingRight).toFloat()
        val rangeL = normalizedToScreen(normalizedMinValue)
        val rangeR = normalizedToScreen(normalizedMaxValue)
        val leftRect = Rect((bg_middle_left + thumbHalfWidth).toInt(), height, (rangeL + thumbHalfWidth).toInt(), 0)
        val rightRect = Rect((rangeR - thumbHalfWidth).toInt(), height, (bg_middle_right - thumbHalfWidth).toInt(), 0)
        canvas.drawRect(leftRect, mShadow)
        canvas.drawRect(rightRect, mShadow)
        canvas.drawRect(0f, 0f, thumbHalfWidth, height.toFloat(), mShadow)
        if (mThumbsCount >= 8) {
            canvas.drawRect(width - thumbHalfWidth, 0f, width.toFloat(), height.toFloat(), mShadow)
        }
        canvas.drawRect(rangeL + thumbHalfWidth, 0f, rangeR - thumbHalfWidth, thumbPaddingTop + ScreenUtils.dip2px(context, 2), rectPaint!!)
        //    canvas.drawRect(rangeL, getHeight() - ScreenUtils.dip2px(getContext(), 2), rangeR, getHeight(), rectPaint);
        drawThumb(normalizedToScreen(normalizedMinValue), false, canvas, true)
        drawThumb(normalizedToScreen(normalizedMaxValue), false, canvas, false)
        //    drawVideoTrimTimeText(canvas);
    }

    private fun drawThumb(screenCoord: Float, pressed: Boolean, canvas: Canvas, isLeft: Boolean) {
        val left = if (isLeft) thumbHalfWidth + screenCoord else screenCoord - thumbHalfWidth
        canvas.drawRect(left, 0f, left + ScreenUtils.dip2px(context, 2), height.toFloat(), rectPaint!!)
        canvas.drawCircle(left, (height / 2).toFloat(), thumbHalfWidth, rectPaint!!)
        //    canvas.drawBitmap(pressed ? thumbPressedImage : (isLeft ? thumbImageLeft : thumbImageRight), screenCoord - (isLeft ? 0 : thumbWidth), 0,
//        paint);
    }

    private fun drawVideoTrimTimeText(canvas: Canvas) {
        val leftThumbsTime = convertSecondsToTime(mStartPosition)
        val rightThumbsTime = convertSecondsToTime(mEndPosition)
        canvas.drawText(leftThumbsTime, normalizedToScreen(normalizedMinValue), TextPositionY.toFloat(), mVideoTrimTimePaintL)
        canvas.drawText(rightThumbsTime, normalizedToScreen(normalizedMaxValue), TextPositionY.toFloat(), mVideoTrimTimePaintR)
    }

    fun getNormalizedMinValue(): Double {
        return normalizedMinValue
    }

    fun getNormalizedMaxValue(): Double {
        return normalizedMaxValue
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTouchDown) {
            return super.onTouchEvent(event)
        }
        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event)
        }
        if (!isEnabled) {
            return false
        }
        if (absoluteMaxValuePrim <= mMinShootTime) {
            return super.onTouchEvent(event)
        }
        val pointerIndex: Int // 记录点击点的index
        val action: Int = event.getAction()
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1)
                pointerIndex = event.findPointerIndex(mActivePointerId)
                mDownMotionX = event.getX(pointerIndex)
                // 判断touch到的是最大值thumb还是最小值thumb
                pressedThumb = evalPressedThumb(mDownMotionX)
                if (pressedThumb == null) {
                    return super.onTouchEvent(event)
                }
                isPressed = true // 设置该控件被按下了
                onStartTrackingTouch() // 置mIsDragging为true，开始追踪touch事件
                trackTouchEvent(event)
                attemptClaimDrag()
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener!!.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_DOWN, isMin,
                            pressedThumb)
                }
            }
            MotionEvent.ACTION_MOVE -> if (pressedThumb != null) {
                if (mIsDragging) {
                    trackTouchEvent(event)
                } else {
                    // Scroll to follow the motion event
                    pointerIndex = event.findPointerIndex(mActivePointerId)
                    val x: Float = event.getX(pointerIndex) // 手指在控件上点的X坐标
                    // 手指没有点在最大最小值上，并且在控件上有滑动事件
                    if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                        isPressed = true
                        Log.e(TAG, "没有拖住最大最小值") // 一直不会执行？
                        invalidate()
                        onStartTrackingTouch()
                        trackTouchEvent(event)
                        attemptClaimDrag()
                    }
                }
                if (notifyWhileDragging && mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener!!.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_MOVE,
                            isMin, pressedThumb)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsDragging) {
                    trackTouchEvent(event)
                    onStopTrackingTouch()
                    isPressed = false
                } else {
                    onStartTrackingTouch()
                    trackTouchEvent(event)
                    onStopTrackingTouch()
                }
                invalidate()
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener!!.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_UP, isMin,
                            pressedThumb)
                }
                pressedThumb = null // 手指抬起，则置被touch到的thumb为空
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index: Int = event.getPointerCount() - 1
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index)
                mActivePointerId = event.getPointerId(index)
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mIsDragging) {
                    onStopTrackingTouch()
                    isPressed = false
                }
                invalidate() // see above explanation
            }
            else -> {
            }
        }
        return true
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex: Int = ev.getAction() and ACTION_POINTER_INDEX_MASK shr ACTION_POINTER_INDEX_SHIFT
        val pointerId: Int = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mDownMotionX = ev.getX(newPointerIndex)
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    private fun trackTouchEvent(event: MotionEvent) {
        if (event.getPointerCount() > 1) {
            return
        }
        Log.e(TAG, "trackTouchEvent: " + event.getAction() + " x: " + event.getX())
        val pointerIndex: Int = event.findPointerIndex(mActivePointerId) // 得到按下点的index
        var x = 0f
        x = try {
            event.getX(pointerIndex)
        } catch (e: Exception) {
            return
        }
        if (Thumb.MIN == pressedThumb) {
            // screenToNormalized(x)-->得到规格化的0-1的值
            setNormalizedMinValue(screenToNormalized(x, 0))
        } else if (Thumb.MAX == pressedThumb) {
            setNormalizedMaxValue(screenToNormalized(x, 1))
        }
    }

    private fun screenToNormalized(screenCoord: Float, position: Int): Double {
        val width = width
        return if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            0.0
        } else {
            isMin = false
            var current_width = screenCoord.toDouble()
            val rangeL = normalizedToScreen(normalizedMinValue)
            val rangeR = normalizedToScreen(normalizedMaxValue)
            val min = mMinShootTime / (absoluteMaxValuePrim - absoluteMinValuePrim) * (width - thumbWidth * 2)
            min_width = if (absoluteMaxValuePrim > 5 * 60 * 1000) { //大于5分钟的精确小数四位
                val df = DecimalFormat("0.0000")
                df.format(min).toDouble()
            } else {
                Math.round(min + 0.5).toDouble()
            }
            if (position == 0) {
                if (isInThumbRangeLeft(screenCoord, normalizedMinValue, 0.5)) {
                    return normalizedMinValue
                }
                val rightPosition: Float = if (getWidth() - rangeR >= 0) getWidth() - rangeR else 0
                val left_length = getValueLength() - (rightPosition + min_width)
                if (current_width > rangeL) {
                    current_width = rangeL + (current_width - rangeL)
                } else if (current_width <= rangeL) {
                    current_width = rangeL - (rangeL - current_width)
                }
                if (current_width > left_length) {
                    isMin = true
                    current_width = left_length
                }
                if (current_width < thumbWidth * 2 / 3) {
                    current_width = 0.0
                }
                val resultTime = (current_width - padding) / (width - 2 * thumbWidth)
                normalizedMinValueTime = Math.min(1.0, Math.max(0.0, resultTime))
                val result = (current_width - padding) / (width - 2 * padding)
                Math.min(1.0, Math.max(0.0, result)) // 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            } else {
                if (isInThumbRange(screenCoord, normalizedMaxValue, 0.5)) {
                    return normalizedMaxValue
                }
                val right_length = getValueLength() - (rangeL + min_width)
                if (current_width > rangeR) {
                    current_width = rangeR + (current_width - rangeR)
                } else if (current_width <= rangeR) {
                    current_width = rangeR - (rangeR - current_width)
                }
                var paddingRight = getWidth() - current_width
                if (paddingRight > right_length) {
                    isMin = true
                    current_width = getWidth() - right_length
                    paddingRight = right_length
                }
                if (paddingRight < thumbWidth * 2 / 3) {
                    current_width = getWidth().toDouble()
                    paddingRight = 0.0
                }
                var resultTime = (paddingRight - padding) / (width - 2 * thumbWidth)
                resultTime = 1 - resultTime
                normalizedMaxValueTime = Math.min(1.0, Math.max(0.0, resultTime))
                val result = (current_width - padding) / (width - 2 * padding)
                Math.min(1.0, Math.max(0.0, result)) // 保证该该值为0-1之间，但是什么时候这个判断有用呢？
            }
        }
    }

    private fun getValueLength(): Int {
        return width - 2 * thumbWidth
    }

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private fun evalPressedThumb(touchX: Float): Thumb? {
        var result: Thumb? = null
        val minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 2.0) // 触摸点是否在最小值图片范围内
        val maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 2.0)
        if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = if (touchX / width > 0.5f) Thumb.MIN else Thumb.MAX
        } else if (minThumbPressed) {
            result = Thumb.MIN
        } else if (maxThumbPressed) {
            result = Thumb.MAX
        }
        return result
    }

    private fun isInThumbRange(touchX: Float, normalizedThumbValue: Double, scale: Double): Boolean {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth * scale
    }

    private fun isInThumbRangeLeft(touchX: Float, normalizedThumbValue: Double, scale: Double): Boolean {
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=最小点图片的宽度的一般
        // 即判断触摸点是否在以最小值图片中心为原点，宽度一半为半径的圆内。
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue) - thumbWidth) <= thumbHalfWidth * scale
    }

    /**
     * 试图告诉父view不要拦截子控件的drag
     */
    private fun attemptClaimDrag() {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
    }

    fun onStartTrackingTouch() {
        mIsDragging = true
    }

    fun onStopTrackingTouch() {
        mIsDragging = false
    }

    fun setMinShootTime(min_cut_time: Long) {
        mMinShootTime = min_cut_time
    }

    private fun normalizedToScreen(normalizedCoord: Double): Float {
        return (paddingLeft + normalizedCoord * (width - paddingLeft - paddingRight)).toFloat()
    }

    private fun valueToNormalized(value: Long): Double {
        return if (0.0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            0.0
        } else (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim)
    }

    fun setStartEndTime(start: Long, end: Long) {
        mStartPosition = start / 1000
        mEndPosition = end / 1000
    }

    fun setSelectedMinValue(value: Long) {
        if (0.0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            setNormalizedMinValue(0.0)
        } else {
            setNormalizedMinValue(valueToNormalized(value))
        }
    }

    fun setSelectedMaxValue(value: Long) {
        if (0.0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            setNormalizedMaxValue(1.0)
        } else {
            setNormalizedMaxValue(valueToNormalized(value))
        }
    }

    fun setNormalizedMinValue(value: Double) {
        normalizedMinValue = Math.max(0.0, Math.min(1.0, Math.min(value, normalizedMaxValue)))
        invalidate() // 重新绘制此view
    }

    fun setNormalizedMaxValue(value: Double) {
        normalizedMaxValue = Math.max(0.0, Math.min(1.0, Math.max(value, normalizedMinValue)))
        invalidate() // 重新绘制此view
    }

    fun getSelectedMinValue(): Long {
        return normalizedToValue(normalizedMinValueTime)
    }

    fun getSelectedMaxValue(): Long {
        return normalizedToValue(normalizedMaxValueTime)
    }

    fun getStartLine(): Float {
        return normalizedToScreen(normalizedMinValue) + thumbHalfWidth
    }

    fun getEndLine(): Float {
        return normalizedToScreen(normalizedMaxValue) - thumbHalfWidth
    }

    private fun normalizedToValue(normalized: Double): Long {
        return (absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim)).toLong()
    }

    /**
     * 供外部activity调用，控制是都在拖动的时候打印log信息，默认是false不打印
     */
    fun isNotifyWhileDragging(): Boolean {
        return notifyWhileDragging
    }

    fun setNotifyWhileDragging(flag: Boolean) {
        notifyWhileDragging = flag
    }

    fun setTouchDown(touchDown: Boolean) {
        isTouchDown = touchDown
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("SUPER", super.onSaveInstanceState())
        bundle.putDouble("MIN", normalizedMinValue)
        bundle.putDouble("MAX", normalizedMaxValue)
        bundle.putDouble("MIN_TIME", normalizedMinValueTime)
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime)
        return bundle
    }

    override fun onRestoreInstanceState(parcel: Parcelable) {
        val bundle: Bundle = parcel as Bundle
        super.onRestoreInstanceState(bundle.getParcelable<Parcelable>("SUPER"))
        normalizedMinValue = bundle.getDouble("MIN")
        normalizedMaxValue = bundle.getDouble("MAX")
        normalizedMinValueTime = bundle.getDouble("MIN_TIME")
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME")
    }

    interface OnRangeSeekBarChangeListener {
        fun onRangeSeekBarValuesChanged(bar: RangeSeekBarView?, minValue: Long, maxValue: Long, action: Int, isMin: Boolean, pressedThumb: Thumb?)
    }

    fun setOnRangeSeekBarChangeListener(listener: OnRangeSeekBarChangeListener?) {
        mRangeSeekBarChangeListener = listener
    }

    companion object {
        private val TAG = RangeSeekBarView::class.java.simpleName
        const val INVALID_POINTER_ID = 255
        const val ACTION_POINTER_INDEX_MASK = 0x0000ff00
        const val ACTION_POINTER_INDEX_SHIFT = 8
        fun convertSecondsToTime(seconds: Long): String {
            var timeStr: String? = null
            var hour = 0
            var minute = 0
            var second = 0
            if (seconds <= 0) {
                return "00:00"
            } else {
                minute = seconds.toInt() / 60
                if (minute < 60) {
                    second = seconds.toInt() % 60
                    timeStr = unitFormat(minute) + ":" + unitFormat(second)
                } else {
                    hour = minute / 60
                    if (hour > 99) {
                        return "99:59:59"
                    }
                    minute = minute % 60
                    second = (seconds - hour * 3600 - minute * 60) as Int
                    timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second)
                }
            }
            return timeStr
        }

        private fun unitFormat(i: Int): String {
            var retStr: String? = null
            retStr = if (i >= 0 && i < 10) {
                "0" + Integer.toString(i)
            } else {
                "" + i
            }
            return retStr
        }
    }
}