package com.luck.picture.lib.instagram

import android.animation.Animator
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.animation.Interpolator
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/3/26 15:37
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramGallery : FrameLayout {
    private var mPreviewView: ViewGroup? = null
    private var mGalleryView: ViewGroup? = null
    private var startedTrackingX = 0
    private var startedTrackingY = 0
    private var scrollPosition = 0f
    private var velocityTracker: VelocityTracker? = null
    private val interpolator: Interpolator = LinearInterpolator()
    private var galleryHeight = 0
    private var interceptY = 0
    private var scrollTop = false
    private var maskView: View? = null
    private var emptyView: TextView? = null
    private var previewBottomMargin = 0
    private var viewVisibility = View.VISIBLE
    private var mAnimatorSet: AnimatorSet? = null

    constructor(context: Context) : super(context) {}
    constructor(context: Context, previewView: ViewGroup?, galleryView: ViewGroup?) : super(context) {
        installView(previewView, galleryView)
    }

    fun installView(previewView: ViewGroup?, galleryView: ViewGroup?) {
        mPreviewView = previewView
        mGalleryView = galleryView
        if (previewView != null) {
            addView(previewView)
        }
        if (galleryView != null) {
            addView(galleryView)
        }
        installMaskView(getContext())
        installEmptyView(getContext())
    }

    private fun installMaskView(context: Context) {
        maskView = View(context)
        maskView!!.setBackgroundColor(-0x67000000)
        addView(maskView)
        maskView!!.visibility = View.GONE
        maskView!!.setOnClickListener { v: View? -> startChildAnimation(false, 200) }
    }

    private fun installEmptyView(context: Context) {
        emptyView = TextView(context)
        emptyView.setGravity(Gravity.CENTER)
        emptyView.setLineSpacing(ScreenUtils.dip2px(context, 3), 1.0f)
        emptyView.setTextSize(16f)
        emptyView.setTextColor(ContextCompat.getColor(context, R.color.picture_color_aab2bd))
        emptyView.setText(context.getString(R.string.picture_empty))
        addView(emptyView)
        emptyView.setVisibility(View.INVISIBLE)
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        if (mPreviewView != null && mPreviewView.getVisibility() == View.VISIBLE) {
            mPreviewView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
        }
        if (mGalleryView != null && mGalleryView.getVisibility() == View.VISIBLE) {
            galleryHeight = mGalleryView.getLayoutParams().height
            if (galleryHeight <= 0) {
                galleryHeight = getGalleryHeight(width, height)
            }
            mGalleryView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(galleryHeight, MeasureSpec.EXACTLY))
        }
        if (maskView!!.visibility == View.VISIBLE) {
            maskView!!.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width - previewBottomMargin, MeasureSpec.EXACTLY))
        }
        if (emptyView.getVisibility() == View.VISIBLE) {
            emptyView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        }
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop = 0
        var viewLeft = 0
        if (maskView!!.visibility == View.VISIBLE) {
            maskView!!.layout(viewLeft, viewTop, viewLeft + maskView!!.measuredWidth, viewTop + maskView!!.measuredHeight)
        }
        if (mPreviewView != null && mGalleryView != null && mPreviewView.getVisibility() == View.VISIBLE && mGalleryView.getVisibility() == View.VISIBLE) {
            mPreviewView.layout(viewLeft, viewTop, viewLeft + mPreviewView.getMeasuredWidth(), viewTop + mPreviewView.getMeasuredHeight())
            viewTop += mPreviewView.getMeasuredHeight()
            mGalleryView.layout(viewLeft, viewTop, viewLeft + mGalleryView.getMeasuredWidth(), viewTop + mGalleryView.getMeasuredHeight())
        }
        if (emptyView.getVisibility() == View.VISIBLE) {
            viewTop = (getMeasuredHeight() - emptyView.getMeasuredHeight()) / 2
            viewLeft = (getMeasuredWidth() - emptyView.getMeasuredWidth()) / 2
            emptyView.layout(viewLeft, viewTop, viewLeft + emptyView.getMeasuredWidth(), viewTop + emptyView.getMeasuredHeight())
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (viewVisibility != View.VISIBLE) {
            return super.onInterceptTouchEvent(ev)
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            val rect = Rect()
            mPreviewView.getHitRect(rect)
            rect[rect.left, rect.bottom - ScreenUtils.dip2px(getContext(), 15), rect.right] = rect.bottom + ScreenUtils.dip2px(getContext(), 15)
            return if (rect.contains(ev.getX() as Int, ev.getY() as Int)) {
                true
            } else {
                interceptY = ev.getY()
                false
            }
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            val rect = Rect()
            mGalleryView.getHitRect(rect)
            if (mGalleryView is GalleryView && rect.contains(ev.getX() as Int, ev.getY() as Int)) {
                val dy = (ev.getY() as Int - interceptY).toFloat()
                return if (dy > 5 && (mGalleryView as GalleryView?)!!.isScrollTop()) {
                    startedTrackingY = ev.getY()
                    true
                } else {
                    false
                }
            }
            return false
        } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            return false
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mPreviewView == null || mGalleryView == null || viewVisibility != View.VISIBLE) {
            return super.onTouchEvent(event)
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startedTrackingX = event.getX()
            startedTrackingY = event.getY()
            if (velocityTracker != null) {
                velocityTracker.clear()
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker.addMovement(event)
            val dx: Float = (event.getX() - startedTrackingX) as Int.toFloat()
            val dy = (event.getY() as Int - startedTrackingY).toFloat()
            if (scrollPosition >= 0) {
                moveByY(dy * 0.25f)
            } else {
                moveByY(dy)
            }
            measureGallerayHeight(dy)
            startedTrackingY = event.getY()
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker.computeCurrentVelocity(1000)
            val velY: Float = velocityTracker.getYVelocity()
            if (velocityTracker != null) {
                velocityTracker.recycle()
                velocityTracker = null
            }
            val triggerValue: Int = getMeasuredWidth() / 2
            if (Math.abs(velY) >= 3500) {
                if (velY <= 0) {
                    startChildAnimation(true, 150)
                } else {
                    startChildAnimation(false, 150)
                }
            } else {
                if (scrollPosition <= -triggerValue) {
                    startChildAnimation(true, 200)
                } else {
                    startChildAnimation(false, 200)
                }
            }
        }
        return true
    }

    private fun startChildAnimation(scrollTop: Boolean, duration: Long, callback: AnimationCallback? = null) {
        this.scrollTop = scrollTop
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel()
        }
        mAnimatorSet = AnimatorSet()
        if (scrollTop) {
            setGalleryHeight(getMeasuredHeight() - getPreviewFoldHeight())
            mAnimatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, "scrollPosition", scrollPosition, -(getMeasuredWidth() - getPreviewFoldHeight()).toFloat()))
        } else {
            mAnimatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, "scrollPosition", scrollPosition, 0f),
                    ObjectAnimator.ofInt(this, "galleryHeight", galleryHeight, getGalleryHeight(getMeasuredWidth(), getMeasuredHeight())))
        }
        mAnimatorSet.setDuration(duration)
        mAnimatorSet.setInterpolator(interpolator)
        if (callback != null) {
            mAnimatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    callback.onAnimationStart()
                }

                override fun onAnimationEnd(animation: Animator) {
                    callback.onAnimationEnd()
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        mAnimatorSet.start()
    }

    private fun measureGallerayHeight(dy: Float) {
        var height: Int = mGalleryView.getLayoutParams().height
        if (height <= 0) {
            height = getGalleryHeight(getMeasuredWidth(), getMeasuredHeight())
        }
        if (dy < 0) {
            height += Math.abs(dy).toInt()
            if (height > getMeasuredHeight() - getPreviewFoldHeight()) {
                height = getMeasuredHeight() - getPreviewFoldHeight()
            }
        } else if (dy > 0) {
            height -= Math.abs(dy).toInt()
            if (height < getGalleryHeight(getMeasuredWidth(), getMeasuredHeight())) {
                height = getGalleryHeight(getMeasuredWidth(), getMeasuredHeight())
            }
        }
        mGalleryView.getLayoutParams().height = height
        mGalleryView.requestLayout()
    }

    fun moveByY(dy: Float) {
        setScrollPosition(scrollPosition + dy)
    }

    fun setGalleryHeight(galleryHeight: Int) {
        mGalleryView.getLayoutParams().height = galleryHeight
        mGalleryView.requestLayout()
    }

    fun setScrollPosition(value: Float) {
        val oldScrollPosition = scrollPosition
        scrollPosition = if (value < -(getMeasuredWidth() - getPreviewFoldHeight())) {
            -(getMeasuredWidth() - getPreviewFoldHeight()).toFloat()
        } else {
            value
        }
        if (oldScrollPosition == scrollPosition) {
            return
        }
        mPreviewView.setTranslationY(scrollPosition)
        mGalleryView.setTranslationY(scrollPosition)
        maskView!!.translationY = scrollPosition
        if (scrollPosition < 0) {
            maskView!!.alpha = Math.abs(scrollPosition) / (getMeasuredWidth() - getPreviewFoldHeight())
            if (maskView!!.visibility != View.VISIBLE) {
                maskView!!.visibility = View.VISIBLE
            }
        } else if (scrollPosition == 0f) {
            if (maskView!!.visibility != View.GONE) {
                maskView!!.visibility = View.GONE
            }
        }
        scrollTop = if (scrollPosition <= -(getMeasuredWidth() - getPreviewFoldHeight())) {
            true
        } else {
            false
        }
    }

    fun expandPreview() {
        if (scrollTop) {
            startChildAnimation(false, 200)
        }
    }

    fun expandPreview(callback: AnimationCallback?) {
        if (scrollTop) {
            startChildAnimation(false, 200, callback)
        }
    }

    fun closePreview() {
        if (!scrollTop) {
            startChildAnimation(true, 200)
        }
    }

    fun isScrollTop(): Boolean {
        return scrollTop
    }

    fun setPreviewBottomMargin(previewBottomMargin: Int) {
        this.previewBottomMargin = previewBottomMargin
    }

    private fun getGalleryHeight(ParentWidth: Int, ParentHeight: Int): Int {
        return ParentHeight - ParentWidth
    }

    private fun getPreviewFoldHeight(): Int {
        return ScreenUtils.dip2px(getContext(), 60)
    }

    fun getPreviewView(): ViewGroup? {
        return mPreviewView
    }

    fun getGalleryView(): ViewGroup? {
        return mGalleryView
    }

    fun getEmptyView(): TextView? {
        return emptyView
    }

    fun setViewVisibility(visibility: Int) {
        viewVisibility = visibility
        InstagramUtils.setViewVisibility(mPreviewView, visibility)
        InstagramUtils.setViewVisibility(mGalleryView, visibility)
    }

    interface AnimationCallback {
        fun onAnimationStart()
        fun onAnimationEnd()
    }

    fun setInitGalleryHeight() {
        mGalleryView.getLayoutParams().height = -1
    }
}