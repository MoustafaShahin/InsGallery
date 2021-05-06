package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.animation.Interpolator
import com.luck.picture.lib.tools.ScreenUtils
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/3/26 15:37
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramViewPager : FrameLayout {
    private var startedTrackingX = 0
    private var startedTrackingY = 0
    private var scrollHorizontalPosition = 0f
    private var velocityTracker: VelocityTracker? = null
    private val Interpolator: Interpolator = AccelerateInterpolator()
    private var interceptY = 0
    private var interceptX = 0
    private val mItems: MutableList<Page> = ArrayList<Page>()
    private val mViews: MutableList<View> = ArrayList()
    private var mCurrentPosition = 0
    private var mSelectedPosition = 0
    private var mTabLayout: InstagramTabLayout? = null
    var click = false
    var startClickX = 0
    var startClickY = 0
    var time: Long = 0
    private var mAnimatorSet: AnimatorSet? = null
    private var mOnPageChangeListener: OnPageChangeListener? = null
    private var skipRange = 0
    private var scrollEnable = true
    private var isDisplayTabLayout = true

    constructor(context: Context) : super(context) {}
    constructor(context: Context, items: List<Page>?, config: PictureSelectionConfig) : super(context) {
        require(!(items == null || items.isEmpty())) { "items is isEmpty!" }
        mItems.addAll(items)
        installView(items)
        mItems[0].init(0, this)
        mViews[0].tag = true
        mItems[0].refreshData(context)
        mTabLayout = InstagramTabLayout(context, items, config)
        addView(mTabLayout)
    }

    fun installView(items: List<Page>) {
        for (item in items) {
            if (item != null) {
                val view: View = item.getView(getContext())
                if (view != null) {
                    addView(view)
                    mViews.add(view)
                } else {
                    throw IllegalStateException("getView(Context) is null!")
                }
            }
        }
    }

    fun addPage(page: Page?) {
        if (page != null) {
            mItems.add(page)
        }
    }

    fun onResume() {
        for (item in mItems) {
            item.onResume()
        }
    }

    fun onPause() {
        for (item in mItems) {
            item.onPause()
        }
    }

    fun onDestroy() {
        for (item in mItems) {
            item.onDestroy()
        }
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        var childHeight = height
        if (mTabLayout.getVisibility() === View.VISIBLE) {
            measureChild(mTabLayout, widthMeasureSpec, heightMeasureSpec)
            childHeight -= mTabLayout.getMeasuredHeight()
        }
        for (view in mViews) {
            view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY))
        }
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        var viewTop = 0
        var viewLeft: Int
        for (i in mViews.indices) {
            viewLeft = i * getMeasuredWidth()
            val view = mViews[i]
            view.layout(viewLeft, viewTop, viewLeft + view.measuredWidth, viewTop + view.measuredHeight)
        }
        if (mTabLayout.getVisibility() === View.VISIBLE) {
            viewLeft = 0
            viewTop = getMeasuredHeight() - mTabLayout.getMeasuredHeight()
            mTabLayout.layout(viewLeft, viewTop, viewLeft + mTabLayout.getMeasuredWidth(), viewTop + mTabLayout.getMeasuredHeight())
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!scrollEnable) {
            return false
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            interceptX = ev.getX()
            interceptY = ev.getY()
            return false
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (mViews.size < 2) {
                return false
            }
            val rect: Rect = mItems[mCurrentPosition].disallowInterceptTouchRect()
            if (rect != null && rect.contains(ev.getX() as Int, ev.getY() as Int)) {
                return false
            }
            val dx = (ev.getX() as Int - interceptX).toFloat()
            val dy = (ev.getY() as Int - interceptY).toFloat()
            if (ev.getPointerCount() < 2 && Math.abs(dx) > ScreenUtils.dip2px(getContext(), 3) && Math.abs(dy) < ScreenUtils.dip2px(getContext(), 5)) {
                startedTrackingX = ev.getX()
                startedTrackingY = ev.getY()
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startedTrackingX = event.getX()
            startedTrackingY = event.getY()
            if (velocityTracker != null) {
                velocityTracker.clear()
            }
            click = true
            startClickX = event.getX()
            startClickY = event.getY()
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker.addMovement(event)
            val dx: Float = (event.getX() - startedTrackingX) as Int.toFloat()
            val dy = (event.getY() as Int - startedTrackingY).toFloat()
            if (scrollEnable) {
                moveByX(dx * 1.1f)
            }
            startedTrackingX = event.getX()
            startedTrackingY = event.getY()
            if (click && (Math.abs(event.getX() - startClickX) > ScreenUtils.dip2px(getContext(), 3) || Math.abs(event.getY() - startClickY) > ScreenUtils.dip2px(getContext(), 3))) {
                click = false
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker.computeCurrentVelocity(1000)
            val velX: Float = velocityTracker.getXVelocity()
            if (velocityTracker != null) {
                velocityTracker.recycle()
                velocityTracker = null
            }
            if (scrollEnable) {
                val triggerValue: Int = getMeasuredWidth() / 2
                var position = (Math.abs(scrollHorizontalPosition) / getMeasuredWidth()) as Int
                if (Math.abs(scrollHorizontalPosition) % getMeasuredWidth() >= triggerValue) {
                    position++
                }
                val destination = getDestination(position)
                if (Math.abs(velX) >= 500) {
                    if (velX <= 0) {
                        startChildAnimation(getDestination(mCurrentPosition).toFloat(), 150)
                    } else {
                        startChildAnimation(getDestination(mCurrentPosition - 1).toFloat(), 150)
                    }
                } else {
                    startChildAnimation(destination.toFloat(), 200)
                }
            }
            if (click) {
                val rect = Rect()
                mTabLayout.getHitRect(rect)
                if (rect.contains(event.getX() as Int, event.getY() as Int)) {
                    val elapsedRealtime = SystemClock.elapsedRealtime()
                    if (elapsedRealtime - time > 300) {
                        time = elapsedRealtime
                        click = false
                        if (mTabLayout!!.getTabSize() > 1) {
                            val tabWidth: Int = getMeasuredWidth() / mTabLayout!!.getTabSize()
                            selectPagePosition((event.getX() / tabWidth) as Int)
                        }
                    }
                }
            }
        }
        return true
    }

    fun selectPagePosition(position: Int) {
        var duration: Long = 150
        val span = Math.abs(mCurrentPosition - position)
        if (span > 1) {
            duration += ((span - 1) * 80).toLong()
        }
        startChildAnimation(getDestination(position).toFloat(), duration)
    }

    private fun getDestination(position: Int): Int {
        var position = position
        if (position < 0) {
            position = 0
        }
        return -(position * getMeasuredWidth())
    }

    private fun startChildAnimation(destination: Float, duration: Long) {
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel()
        }
        mAnimatorSet = AnimatorSet()
        mAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(this, "scrollHorizontalPosition", scrollHorizontalPosition, destination))
        mAnimatorSet.setInterpolator(Interpolator)
        mAnimatorSet.setDuration(duration)
        mAnimatorSet.start()
    }

    fun moveByX(dx: Float) {
        setScrollHorizontalPosition(scrollHorizontalPosition + dx)
    }

    fun setScrollHorizontalPosition(value: Float) {
        if (mViews.size < 2) {
            return
        }
        val oldHorizontalPosition = scrollHorizontalPosition
        scrollHorizontalPosition = if (value < -(getMeasuredWidth() * (mViews.size - 1))) {
            -(getMeasuredWidth() * (mViews.size - 1)).toFloat()
        } else if (value > 0) {
            0f
        } else {
            value
        }
        if (oldHorizontalPosition == scrollHorizontalPosition) {
            return
        }
        val isTranslationX = skipRange <= 0 || scrollHorizontalPosition >= -(getMeasuredWidth() * (mViews.size - (1 + skipRange)))
        if (isTranslationX) {
            for (view in mViews) {
                view.translationX = scrollHorizontalPosition
            }
        } else {
            if (mViews[0].translationX != -(getMeasuredWidth() * (mViews.size - (1 + skipRange))).toFloat()) {
                for (view in mViews) {
                    view.translationX = -(getMeasuredWidth() * (mViews.size - (1 + skipRange))).toFloat()
                }
            }
        }
        var position = (Math.abs(scrollHorizontalPosition) / getMeasuredWidth()) as Int
        val offset: Float = Math.abs(scrollHorizontalPosition) % getMeasuredWidth()
        mTabLayout.setIndicatorPosition(position, offset / getMeasuredWidth())
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(position, offset / getMeasuredWidth(), offset.toInt())
        }
        if (offset == 0f) {
            mSelectedPosition = position
            mTabLayout!!.selectTab(position)
            if (mOnPageChangeListener != null) {
                mOnPageChangeListener.onPageSelected(position)
            }
            mItems[position].refreshData(getContext())
        }
        if (offset > 0) {
            position++
        }
        mCurrentPosition = position
        val currentView = mViews[position]
        val tag = currentView.tag
        var isInti = false
        if (tag is Boolean) {
            isInti = tag
        }
        if (!isInti) {
            mItems[position].init(position, this)
            currentView.tag = true
        }
    }

    fun setSkipRange(skipRange: Int) {
        if (skipRange < 1 || skipRange >= mItems.size) {
            return
        }
        this.skipRange = skipRange
    }

    fun setOnPageChangeListener(onPageChangeListener: OnPageChangeListener?) {
        mOnPageChangeListener = onPageChangeListener
    }

    fun setScrollEnable(scrollEnable: Boolean) {
        this.scrollEnable = scrollEnable
    }

    fun displayTabLayout(isDisplay: Boolean) {
        if (isDisplayTabLayout == isDisplay) {
            return
        }
        isDisplayTabLayout = isDisplay
        if (isDisplay) {
            InstagramUtils.setViewVisibility(mTabLayout, View.VISIBLE)
        } else {
            InstagramUtils.setViewVisibility(mTabLayout, View.GONE)
        }
    }

    fun getSelectedPosition(): Int {
        return mSelectedPosition
    }
}