package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Rect
import android.view.View
import com.luck.picture.lib.camera.listener.CameraListener

/**
 * ================================================
 * Created by JessYan on 2020/4/15 12:02
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class PagePhoto(parentActivity: PictureBaseActivity?, config: PictureSelectionConfig?) : Page {
    private var mParentActivity: PictureBaseActivity?
    private var mConfig: PictureSelectionConfig?
    private var mInstagramCameraView: InstagramCameraView? = null
    private var mCameraListener: CameraListener? = null
    fun getView(context: Context): View {
        mInstagramCameraView = InstagramCameraView(context, mParentActivity, mConfig)
        if (mConfig.recordVideoSecond > 0) {
            mInstagramCameraView.setRecordVideoMaxTime(mConfig.recordVideoSecond)
        }
        if (mConfig.recordVideoMinSecond > 0) {
            mInstagramCameraView.setRecordVideoMinTime(mConfig.recordVideoMinSecond)
        }
        if (mCameraListener != null) {
            mInstagramCameraView.setCameraListener(mCameraListener)
        }
        return mInstagramCameraView
    }

    override fun refreshData(context: Context?) {}
    override fun init(position: Int, parent: ViewGroup?) {}
    override fun onResume() {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.onResume()
        }
    }

    override fun onPause() {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.onPause()
        }
    }

    override fun onDestroy() {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.release()
        }
        mInstagramCameraView = null
        mConfig = null
        mCameraListener = null
        mParentActivity = null
    }

    fun getTitle(context: Context): String {
        return context.getString(R.string.photo)
    }

    override fun disallowInterceptTouchRect(): Rect? {
        return if (mInstagramCameraView == null) {
            null
        } else mInstagramCameraView.disallowInterceptTouchRect()
    }

    fun bindToLifecycle() {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.bindToLifecycle()
        }
    }

    fun isBindCamera(): Boolean {
        return if (mInstagramCameraView == null) {
            false
        } else mInstagramCameraView.isBind()
    }

    fun setCaptureButtonTranslationX(translationX: Float) {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.setCaptureButtonTranslationX(translationX)
        }
    }

    fun setCameraState(cameraState: Int) {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.setCameraState(cameraState)
        }
    }

    fun setCameraListener(cameraListener: CameraListener?) {
        mCameraListener = cameraListener
        if (mInstagramCameraView != null) {
            mInstagramCameraView.setCameraListener(mCameraListener)
        }
    }

    fun setEmptyViewVisibility(visibility: Int) {
        if (mInstagramCameraView != null) {
            mInstagramCameraView.setEmptyViewVisibility(visibility)
        }
    }

    fun isInLongPress(): Boolean {
        return if (mInstagramCameraView == null) {
            false
        } else mInstagramCameraView.isInLongPress()
    }

    init {
        mParentActivity = parentActivity
        mConfig = config
    }
}