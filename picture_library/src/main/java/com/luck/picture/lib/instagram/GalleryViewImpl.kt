package com.luck.picture.lib.instagram

import android.content.*
import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.recyclerview.widget.RecyclerView

/**
 * ================================================
 * Created by JessYan on 2020/3/30 16:14
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class GalleryViewImpl(context: Context) : RecyclerView(context), GalleryView {
    private var startedTrackingY = 0
    private var velocityTracker: VelocityTracker? = null
    override fun isScrollTop(): Boolean {
        val child = getChildAt(0)
        if (child != null) {
            val holder = findContainingViewHolder(child)
            if (holder != null && holder.adapterPosition == 0) {
                val top = child.top
                return if (top >= 0) {
                    true
                } else {
                    false
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            startedTrackingY = event.y.toInt()
            if (velocityTracker != null) {
                velocityTracker!!.clear()
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker!!.addMovement(event)
            if (startedTrackingY == 0) {
                startedTrackingY = event.y.toInt()
            }
            val dy = (event.y.toInt() - startedTrackingY).toFloat()
            if (dy > 0 && isScrollTop()) {
                parent.requestDisallowInterceptTouchEvent(false)
                return false
            }
            startedTrackingY = event.y.toInt()
        } else if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            velocityTracker!!.computeCurrentVelocity(1000)
            val velY = velocityTracker!!.yVelocity
            if (velocityTracker != null) {
                velocityTracker!!.recycle()
                velocityTracker = null
            }
            if (Math.abs(velY) >= 5000) {
                if (velY <= 0) {
                    (parent as InstagramGallery).closePreview()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    init {
        overScrollMode = OVER_SCROLL_NEVER
        addOnScrollListener(object : OnScrollListener() {
            var state = 0
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                state = newState
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (state == SCROLL_STATE_SETTLING) {
                    if (isScrollTop()) {
                        (parent as InstagramGallery).expandPreview()
                    }
                }
            }
        })
    }
}