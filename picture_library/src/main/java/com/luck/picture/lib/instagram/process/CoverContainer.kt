package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.entity.LocalMedia
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch

/**
 * ================================================
 * Created by JessYan on 2020/7/8 16:32
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class CoverContainer(context: Context, media: LocalMedia) : FrameLayout(context) {
    private val mImageViews = arrayOfNulls<ImageView>(7)
    private val mImageViewHeight: Int
    private var mImageViewWidth = 0
    private var mFrameTask: getAllFrameTask? = null
    private val mMaskView: View
    private val mZoomView: ZoomView
    private var startedTrackingX = 0
    private var startedTrackingY = 0
    var startClickX = 0
    var startClickY = 0
    private var scrollHorizontalPosition = 0f
    private var mOnSeekListener: onSeekListener? = null
    private val mMedia: LocalMedia
    private var mChangeTime: Long = 0
    private var mGetFrameBitmapTask: getFrameBitmapTask? = null
    private var mCurrentPercent = 0f
    fun getFrame(context: Context, media: LocalMedia) {
        mGetFrameBitmapTask = getFrameBitmapTask(context, media, false, -1, mImageViewHeight, mImageViewHeight, OnCompleteListenerImpl(mZoomView))
        mGetFrameBitmapTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        mFrameTask = getAllFrameTask(context, media, mImageViews.size, 0, (media.getDuration() as Int).toLong(), OnSingleBitmapListenerImpl(this))
        mFrameTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mImageViewWidth = (width - ScreenUtils.dip2px(getContext(), 40)) / mImageViews.size
        for (imageView in mImageViews) {
            imageView!!.measure(MeasureSpec.makeMeasureSpec(mImageViewWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mImageViewHeight, MeasureSpec.EXACTLY))
        }
        mMaskView.measure(MeasureSpec.makeMeasureSpec(width - ScreenUtils.dip2px(getContext(), 40) + mImageViews.size - 1, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mImageViewHeight, MeasureSpec.EXACTLY))
        mZoomView.measure(MeasureSpec.makeMeasureSpec(mImageViewHeight, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mImageViewHeight, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val viewTop: Int = (getMeasuredHeight() - mImageViewHeight) / 2
        var viewLeft: Int
        for (i in mImageViews.indices) {
            viewLeft = i * (mImageViewWidth + 1) + ScreenUtils.dip2px(getContext(), 20)
            mImageViews[i]!!.layout(viewLeft, viewTop, viewLeft + mImageViews[i]!!.measuredWidth, viewTop + mImageViews[i]!!.measuredHeight)
        }
        viewLeft = ScreenUtils.dip2px(getContext(), 20)
        mMaskView.layout(viewLeft, viewTop, viewLeft + mMaskView.measuredWidth, viewTop + mMaskView.measuredHeight)
        mZoomView.layout(viewLeft, viewTop, viewLeft + mZoomView.getMeasuredWidth(), viewTop + mZoomView.getMeasuredHeight())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = Rect()
        mMaskView.getHitRect(rect)
        if (!rect.contains(event.getX() as Int, event.getY() as Int)) {
            return super.onTouchEvent(event)
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startedTrackingX = event.getX()
            startedTrackingY = event.getY()
            startClickX = event.getX()
            startClickY = event.getY()
            setScrollHorizontalPosition(startClickX - ScreenUtils.dip2px(getContext(), 20) - mZoomView.getMeasuredWidth() / 2)
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            val dx: Float = (event.getX() - startedTrackingX) as Int.toFloat()
            val dy = (event.getY() as Int - startedTrackingY).toFloat()
            moveByX(dx)
            startedTrackingX = event.getX()
            startedTrackingY = event.getY()
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
            if (mOnSeekListener != null) {
                mOnSeekListener!!.onSeekEnd()
            }
        }
        return true
    }

    fun moveByX(dx: Float) {
        setScrollHorizontalPosition(scrollHorizontalPosition + dx)
    }

    fun setScrollHorizontalPosition(value: Float) {
        val oldHorizontalPosition = scrollHorizontalPosition
        scrollHorizontalPosition = Math.min(Math.max(0f, value), mMaskView.measuredWidth - mZoomView.getMeasuredWidth())
        if (oldHorizontalPosition == scrollHorizontalPosition) {
            return
        }
        mZoomView.setTranslationX(scrollHorizontalPosition)
        mCurrentPercent = scrollHorizontalPosition / (mMaskView.measuredWidth - mZoomView.getMeasuredWidth())
        if (SystemClock.uptimeMillis() - mChangeTime > 200) {
            mChangeTime = SystemClock.uptimeMillis()
            val time = Math.round(mMedia.getDuration() * mCurrentPercent * 1000)
            mGetFrameBitmapTask = getFrameBitmapTask(getContext(), mMedia, false, time, mImageViewHeight, mImageViewHeight, OnCompleteListenerImpl(mZoomView))
            mGetFrameBitmapTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        if (mOnSeekListener != null) {
            mOnSeekListener!!.onSeek(mCurrentPercent)
        }
    }

    fun cropCover(count: CountDownLatch) {
        val time: Long
        time = if (mCurrentPercent > 0) {
            Math.round(mMedia.getDuration() * mCurrentPercent * 1000)
        } else {
            -1
        }
        getFrameBitmapTask(getContext(), mMedia, false, time) label@{ bitmap ->
            PictureThreadUtils.executeByIo(object : SimpleTask<File?>() {
                fun doInBackground(): File {
                    val fileName = System.currentTimeMillis().toString() + ".jpg"
                    val path: File = getContext().getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val file = File(path, "Covers/$fileName")
                    var outputStream: OutputStream? = null
                    try {
                        file.parentFile.mkdirs()
                        outputStream = getContext().getApplicationContext().getContentResolver().openOutputStream(Uri.fromFile(file))
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        bitmap.recycle()
                        MediaScannerConnection.scanFile(getContext().getApplicationContext(), arrayOf(
                                file.toString()
                        ), null,
                                OnScanCompletedListener { path1: String?, uri: Uri? ->
                                    mMedia.setCoverPath(path1)
                                    count.countDown()
                                })
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } finally {
                        BitmapLoadUtils.close(outputStream)
                    }
                    return@label null
                }

                fun onSuccess(result: File?) {}
            })
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun onPause() {}
    fun onDestroy() {
        if (mFrameTask != null) {
            mFrameTask.setStop(true)
            mFrameTask.cancel(true)
            mFrameTask = null
        }
        if (mGetFrameBitmapTask != null) {
            mGetFrameBitmapTask.cancel(true)
            mGetFrameBitmapTask = null
        }
    }

    class OnSingleBitmapListenerImpl(coverContainer: CoverContainer) : getAllFrameTask.OnSingleBitmapListener {
        private val mContainerWeakReference: WeakReference<CoverContainer>
        private var index = 0
        override fun onSingleBitmapComplete(bitmap: Bitmap?) {
            val container = mContainerWeakReference.get()
            if (container != null) {
                container.post(RunnableImpl(container.mImageViews[index], bitmap))
                index++
            }
        }

        class RunnableImpl(imageView: ImageView?, bitmap: Bitmap?) : Runnable {
            private val mViewWeakReference: WeakReference<ImageView?>
            private val mBitmap: Bitmap?
            override fun run() {
                val imageView = mViewWeakReference.get()
                imageView?.setImageBitmap(mBitmap)
            }

            init {
                mViewWeakReference = WeakReference(imageView)
                mBitmap = bitmap
            }
        }

        init {
            mContainerWeakReference = WeakReference(coverContainer)
        }
    }

    class OnCompleteListenerImpl(view: ZoomView) : getFrameBitmapTask.OnCompleteListener {
        private val mViewWeakReference: WeakReference<ZoomView>
        override fun onGetBitmapComplete(bitmap: Bitmap?) {
            val view: ZoomView? = mViewWeakReference.get()
            if (view != null) {
                view.setBitmap(bitmap)
            }
        }

        init {
            mViewWeakReference = WeakReference<ZoomView>(view)
        }
    }

    fun setOnSeekListener(onSeekListener: onSeekListener?) {
        mOnSeekListener = onSeekListener
    }

    interface onSeekListener {
        fun onSeek(percent: Float)
        fun onSeekEnd()
    }

    init {
        mMedia = media
        mImageViewHeight = ScreenUtils.dip2px(getContext(), 60)
        for (i in mImageViews.indices) {
            mImageViews[i] = ImageView(context)
            mImageViews[i]!!.scaleType = ImageView.ScaleType.CENTER_CROP
            mImageViews[i]!!.setImageResource(R.drawable.picture_image_placeholder)
            addView(mImageViews[i])
        }
        mMaskView = View(context)
        mMaskView.setBackgroundColor(0x77FFFFFF)
        addView(mMaskView)
        mZoomView = ZoomView(context)
        addView(mZoomView)
    }
}