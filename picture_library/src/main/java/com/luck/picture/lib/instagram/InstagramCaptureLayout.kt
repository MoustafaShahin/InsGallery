package com.luck.picture.lib.instagram

import android.Manifest
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.View
import com.luck.picture.lib.permissions.PermissionChecker
import java.lang.ref.WeakReference

/**
 * ================================================
 * Created by JessYan on 2020/4/17 11:29
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramCaptureLayout(context: Context, config: PictureSelectionConfig?) : FrameLayout(context) {
    private var mCaptureButton: InstagramCaptureButton?
    private var mRecordButton: InstagramCaptureButton?
    private var mRecordProgressBar: InstagramRecordProgressBar?
    private var mRecordIndicator: InstagramRecordIndicator?
    private var mCaptureListener: InstagramCaptureListener? = null
    private var mHandler: Handler?
    private var mCameraState: Int = InstagramCameraView.STATE_CAPTURE
    var click = false
    var startClickX = 0
    var startClickY = 0
    var time: Long = 0
    private var mInLongPress = false
    private var mIsRecordEnd = false
    private var mRecordedTime = 0
    private var mMaxDurationTime = 0
    private var mMinDurationTime = 0
    private var isCameraBind = false
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        measureChild(mRecordProgressBar, widthMeasureSpec, heightMeasureSpec)
        val diameterSize = (width / 4.2f).toInt()
        mCaptureButton!!.measure(MeasureSpec.makeMeasureSpec(diameterSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(diameterSize, MeasureSpec.EXACTLY))
        mRecordButton!!.measure(MeasureSpec.makeMeasureSpec(diameterSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(diameterSize, MeasureSpec.EXACTLY))
        measureChild(mRecordIndicator, widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop: Int = ScreenUtils.dip2px(getContext(), 1)
        var viewLeft = 0
        mRecordProgressBar!!.layout(viewLeft, viewTop, viewLeft + mRecordProgressBar!!.getMeasuredWidth(), viewTop + mRecordProgressBar!!.getMeasuredHeight())
        viewTop = (getMeasuredHeight() - mCaptureButton!!.getMeasuredHeight()) / 2
        viewLeft = (getMeasuredWidth() - mCaptureButton!!.getMeasuredWidth()) / 2
        mCaptureButton!!.layout(viewLeft, viewTop, viewLeft + mCaptureButton!!.getMeasuredWidth(), viewTop + mCaptureButton!!.getMeasuredHeight())
        viewLeft += getMeasuredWidth()
        mRecordButton!!.layout(viewLeft, viewTop, viewLeft + mCaptureButton!!.getMeasuredWidth(), viewTop + mCaptureButton!!.getMeasuredHeight())
        viewTop = (viewTop - mRecordIndicator.getMeasuredHeight()) / 2
        viewLeft = (getMeasuredWidth() - mRecordIndicator.getMeasuredWidth()) / 2
        mRecordIndicator.layout(viewLeft, viewTop, viewLeft + mRecordIndicator.getMeasuredWidth(), viewTop + mRecordIndicator.getMeasuredHeight())
    }

    fun setCaptureListener(captureListener: InstagramCaptureListener?) {
        mCaptureListener = captureListener
    }

    fun setCaptureButtonTranslationX(translationX: Float) {
        mCaptureButton!!.setTranslationX(translationX)
        mRecordButton!!.setTranslationX(translationX)
    }

    fun disallowInterceptTouchRect(): Rect? {
        if (mCameraState == InstagramCameraView.STATE_RECORDER && mInLongPress) {
            val rect = Rect()
            getHitRect(rect)
            return rect
        }
        return null
    }

    fun setCameraState(cameraState: Int) {
        if (mCameraState == cameraState) {
            return
        }
        mCameraState = cameraState
        if (mCameraState == InstagramCameraView.STATE_RECORDER) {
            InstagramUtils.setViewVisibility(mRecordProgressBar, View.VISIBLE)
            mRecordProgressBar!!.startRecordAnimation()
        } else {
            mRecordProgressBar!!.stopRecordAnimation()
            InstagramUtils.setViewVisibility(mRecordProgressBar, View.INVISIBLE)
        }
    }

    fun setCameraBind(cameraBind: Boolean) {
        isCameraBind = cameraBind
    }

    fun resetRecordEnd() {
        mIsRecordEnd = false
    }

    fun isInLongPress(): Boolean {
        return mInLongPress
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mCaptureButton == null || mRecordButton == null) {
            return super.onTouchEvent(event)
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mCameraState == InstagramCameraView.STATE_CAPTURE) {
                click = true
                startClickX = event.getX()
                startClickY = event.getY()
                val rect = Rect()
                mCaptureButton.getHitRect(rect)
                if (rect.contains(event.getX() as Int, event.getY() as Int)) {
                    if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA) && !isCameraBind) {
                        ToastUtils.s(getContext(), getContext().getString(R.string.camera_init))
                        return super.onTouchEvent(event)
                    }
                    mCaptureButton.pressButton(true)
                }
            }
            if (mCameraState == InstagramCameraView.STATE_RECORDER) {
                val recordRect = Rect()
                mRecordButton.getHitRect(recordRect)
                if (recordRect.contains(event.getX() as Int, event.getY() as Int)) {
                    if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA) && !isCameraBind) {
                        ToastUtils.s(getContext(), getContext().getString(R.string.camera_init))
                        return super.onTouchEvent(event)
                    }
                    mRecordButton.pressButton(true)
                    mInLongPress = false
                    mHandler!!.removeMessages(TIMER)
                    mHandler!!.removeMessages(LONG_PRESS)
                    mHandler!!.sendMessageAtTime(
                            mHandler!!.obtainMessage(
                                    LONG_PRESS),
                            event.getDownTime() + ViewConfiguration.getLongPressTimeout())
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mCameraState == InstagramCameraView.STATE_CAPTURE && click && (Math.abs(event.getX() - startClickX) > ScreenUtils.dip2px(getContext(), 3) || Math.abs(event.getY() - startClickY) > ScreenUtils.dip2px(getContext(), 3))) {
                click = false
                mCaptureButton.pressButton(false)
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mCameraState == InstagramCameraView.STATE_CAPTURE && click) {
                val rect = Rect()
                mCaptureButton.getHitRect(rect)
                if (rect.contains(event.getX() as Int, event.getY() as Int)) {
                    val elapsedRealtime = SystemClock.elapsedRealtime()
                    if (elapsedRealtime - time > 1000) {
                        time = elapsedRealtime
                        click = false
                        if (mCaptureListener != null) {
                            mCaptureListener.takePictures()
                        }
                    }
                }
                mCaptureButton.pressButton(false)
            }
            if (mCameraState == InstagramCameraView.STATE_RECORDER && mRecordButton.isPress() && !mIsRecordEnd) {
                mRecordButton.pressButton(false)
                val isCamera: Boolean = PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                val isRecordAudio: Boolean = PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                if (!isCamera || !isRecordAudio) {
                    return true
                }
                if (mInLongPress) {
                    mInLongPress = false
                    mHandler!!.removeMessages(TIMER)
                    mRecordIndicator!!.stopIndicatorAnimation()
                    mRecordIndicator.setVisibility(View.INVISIBLE)
                    mRecordProgressBar!!.stopRecord()
                    if (mRecordedTime < mMinDurationTime) {
                        if (mCaptureListener != null) {
                            mCaptureListener.recordShort(mRecordedTime.toLong())
                        }
                        ToastUtils.s(getContext(), getContext().getString(R.string.alert_record, mMinDurationTime))
                    } else if (mCaptureListener != null) {
                        mIsRecordEnd = true
                        mCaptureListener.recordEnd(mRecordedTime.toLong())
                    }
                    mRecordedTime = 0
                } else {
                    ToastUtils.s(getContext(), getContext().getString(R.string.press_to_record))
                    mHandler!!.removeMessages(LONG_PRESS)
                    mHandler!!.removeMessages(TIMER)
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mCameraState == InstagramCameraView.STATE_CAPTURE) {
                mCaptureButton.pressButton(false)
            }
            if (mCameraState == InstagramCameraView.STATE_RECORDER) {
                mRecordButton.pressButton(false)
                mHandler!!.removeMessages(LONG_PRESS)
                if (mInLongPress && !mIsRecordEnd) {
                    mHandler!!.removeMessages(TIMER)
                    mRecordIndicator!!.stopIndicatorAnimation()
                    mRecordIndicator.setVisibility(View.INVISIBLE)
                    mRecordProgressBar!!.stopRecord()
                    mRecordedTime = 0
                }
                mInLongPress = false
            }
        }
        return true
    }

    fun setRecordVideoMaxTime(maxDurationTime: Int) {
        mMaxDurationTime = maxDurationTime
        mRecordProgressBar!!.setMaxTime((maxDurationTime * 1000).toLong())
    }

    fun setRecordVideoMinTime(minDurationTime: Int) {
        mMinDurationTime = minDurationTime
    }

    private class GestureHandler(looper: Looper?, captureLayout: InstagramCaptureLayout) : Handler(looper) {
        private val mCaptureLayout: WeakReference<InstagramCaptureLayout>
        override fun handleMessage(msg: Message) {
            val captureLayout = mCaptureLayout.get() ?: return
            when (msg.what) {
                LONG_PRESS -> captureLayout.dispatchLongPress()
                TIMER -> if (captureLayout.mInLongPress) {
                    captureLayout.mRecordedTime++
                    captureLayout.mRecordIndicator!!.setRecordedTime(captureLayout.mRecordedTime)
                    if (captureLayout.mRecordedTime < captureLayout.mMaxDurationTime) {
                        captureLayout.mHandler!!.sendEmptyMessageDelayed(TIMER, 1000)
                    } else {
                        captureLayout.mInLongPress = false
                        captureLayout.mIsRecordEnd = true
                        captureLayout.mRecordIndicator!!.stopIndicatorAnimation()
                        captureLayout.mRecordIndicator.setVisibility(View.INVISIBLE)
                        captureLayout.mRecordProgressBar!!.stopRecord()
                        if (captureLayout.mCaptureListener != null) {
                            captureLayout.mCaptureListener.recordEnd(captureLayout.mRecordedTime.toLong())
                        }
                        captureLayout.mRecordedTime = 0
                    }
                }
                else -> throw RuntimeException("Unknown message $msg") //never
            }
        }

        init {
            mCaptureLayout = WeakReference(captureLayout)
        }
    }

    private fun dispatchLongPress() {
        val isCamera: Boolean = PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
        val isRecordAudio: Boolean = PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
        if (!isCamera || !isRecordAudio) {
            if (mCaptureListener != null) {
                mCaptureListener.recordError()
            }
            return
        }
        if (mIsRecordEnd) {
            return
        }
        mInLongPress = true
        if (mCaptureListener != null) {
            mCaptureListener.recordStart()
        }
        mRecordIndicator.setVisibility(View.VISIBLE)
        mRecordIndicator!!.setRecordedTime(mRecordedTime)
        mRecordIndicator!!.playIndicatorAnimation()
        mRecordProgressBar!!.startRecord()
        mHandler!!.sendEmptyMessageDelayed(TIMER, 1000)
    }

    fun release() {
        if (mHandler != null) {
            mHandler!!.removeCallbacksAndMessages(null)
        }
        if (mRecordProgressBar != null) {
            mRecordProgressBar.release()
        }
        if (mRecordIndicator != null) {
            mRecordIndicator.release()
        }
        mRecordProgressBar = null
        mRecordIndicator = null
        mHandler = null
        mCaptureListener = null
        mCaptureButton = null
        mRecordButton = null
    }

    companion object {
        private const val LONG_PRESS = 1
        private const val TIMER = 2
    }

    init {
        mHandler = GestureHandler(context.mainLooper, this)
        mRecordProgressBar = InstagramRecordProgressBar(context, config)
        addView(mRecordProgressBar)
        mRecordProgressBar.setVisibility(View.INVISIBLE)
        mCaptureButton = InstagramCaptureButton(context)
        addView(mCaptureButton)
        mRecordButton = InstagramCaptureButton(context)
        addView(mRecordButton)
        mRecordIndicator = InstagramRecordIndicator(context, config)
        addView(mRecordIndicator)
        mRecordIndicator.setVisibility(View.INVISIBLE)
    }
}