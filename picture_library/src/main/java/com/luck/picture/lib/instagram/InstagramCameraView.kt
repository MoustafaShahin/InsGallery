package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.camera.view.CameraView
import com.luck.picture.lib.camera.listener.CameraListener
import java.io.File
import java.lang.ref.WeakReference

/**
 * ================================================
 * Created by JessYan on 2020/4/16 18:40
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramCameraView(context: Context, activity: AppCompatActivity?, config: PictureSelectionConfig?) : FrameLayout(context) {
    private var mTypeFlash = TYPE_FLASH_OFF
    private var mConfig: PictureSelectionConfig?
    private var mActivity: WeakReference<AppCompatActivity?>?
    private var mCameraView: CameraView?
    private val mSwitchView: ImageView
    private val mFlashView: ImageView
    private var mCaptureLayout: InstagramCaptureLayout?
    private var isBind = false
    private var mCameraState = STATE_CAPTURE
    private var isFront = false
    private var mCameraListener: CameraListener? = null
    private var mRecordTime: Long = 0
    private val mCameraEmptyView: InstagramCameraEmptyView
    fun createVideoFile(): File {
        return if (SdkVersionUtils.checkedAndroid_Q()) {
            val diskCacheDir: String = PictureFileUtils.getVideoDiskCacheDir(getContext())
            val rootDir = File(diskCacheDir)
            if (!rootDir.exists() && rootDir.mkdirs()) {
            }
            val isOutFileNameEmpty: Boolean = TextUtils.isEmpty(mConfig.cameraFileName)
            val suffix: String = if (TextUtils.isEmpty(mConfig.suffixType)) PictureMimeType.MP4 else mConfig.suffixType
            val newFileImageName: String = if (isOutFileNameEmpty) DateUtils.getCreateFileName("VID_").toString() + suffix else mConfig.cameraFileName
            val cameraFile = File(rootDir, newFileImageName)
            val outUri = getOutUri(PictureMimeType.ofVideo())
            if (outUri != null) {
                mConfig.cameraPath = outUri.toString()
            }
            cameraFile
        } else {
            var cameraFileName = ""
            if (!TextUtils.isEmpty(mConfig.cameraFileName)) {
                val isSuffixOfImage: Boolean = PictureMimeType.isSuffixOfImage(mConfig.cameraFileName)
                mConfig.cameraFileName = if (!isSuffixOfImage) StringUtils
                        .renameSuffix(mConfig.cameraFileName, PictureMimeType.MP4) else mConfig.cameraFileName
                cameraFileName = if (mConfig.camera) mConfig.cameraFileName else StringUtils.rename(mConfig.cameraFileName)
            }
            val cameraFile: File = PictureFileUtils.createCameraFile(getContext(),
                    PictureMimeType.ofVideo(), cameraFileName, mConfig.suffixType, mConfig.outPutCameraPath)
            mConfig.cameraPath = cameraFile.absolutePath
            cameraFile
        }
    }

    fun createImageFile(): File {
        return if (SdkVersionUtils.checkedAndroid_Q()) {
            val diskCacheDir: String = PictureFileUtils.getDiskCacheDir(getContext())
            val rootDir = File(diskCacheDir)
            if (!rootDir.exists() && rootDir.mkdirs()) {
            }
            val isOutFileNameEmpty: Boolean = TextUtils.isEmpty(mConfig.cameraFileName)
            val suffix: String = if (TextUtils.isEmpty(mConfig.suffixType)) PictureFileUtils.POSTFIX else mConfig.suffixType
            val newFileImageName: String = if (isOutFileNameEmpty) DateUtils.getCreateFileName("IMG_").toString() + suffix else mConfig.cameraFileName
            val cameraFile = File(rootDir, newFileImageName)
            val outUri = getOutUri(PictureMimeType.ofImage())
            if (outUri != null) {
                mConfig.cameraPath = outUri.toString()
            }
            cameraFile
        } else {
            var cameraFileName = ""
            if (!TextUtils.isEmpty(mConfig.cameraFileName)) {
                val isSuffixOfImage: Boolean = PictureMimeType.isSuffixOfImage(mConfig.cameraFileName)
                mConfig.cameraFileName = if (!isSuffixOfImage) StringUtils.renameSuffix(mConfig.cameraFileName, PictureMimeType.JPEG) else mConfig.cameraFileName
                cameraFileName = if (mConfig.camera) mConfig.cameraFileName else StringUtils.rename(mConfig.cameraFileName)
            }
            val cameraFile: File = PictureFileUtils.createCameraFile(getContext(),
                    PictureMimeType.ofImage(), cameraFileName, mConfig.suffixType, mConfig.outPutCameraPath)
            mConfig.cameraPath = cameraFile.absolutePath
            cameraFile
        }
    }

    private fun getOutUri(type: Int): Uri {
        return if (type == PictureMimeType.ofVideo()) MediaUtils.createVideoUri(getContext(), mConfig.suffixType) else MediaUtils.createImageUri(getContext(), mConfig.suffixType)
    }

    @SuppressLint("MissingPermission")
    fun bindToLifecycle() {
        if (!isBind && mCameraView != null) {
            isBind = true
            if (mCaptureLayout != null) {
                mCaptureLayout.setCameraBind(true)
            }
            val activity: AppCompatActivity = mActivity!!.get() ?: return
            mCameraView.bindToLifecycle(activity)
        }
    }

    fun isBind(): Boolean {
        return isBind
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mCameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width - ScreenUtils.dip2px(getContext(), 2), MeasureSpec.EXACTLY))
        mCameraEmptyView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width - ScreenUtils.dip2px(getContext(), 2), MeasureSpec.EXACTLY))
        mSwitchView.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.EXACTLY))
        mFlashView.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 30), MeasureSpec.EXACTLY))
        mCaptureLayout.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - width + ScreenUtils.dip2px(getContext(), 2), MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop = 0
        var viewLeft = 0
        mCameraView.layout(viewLeft, viewTop, viewLeft + mCameraView.getMeasuredWidth(), viewTop + mCameraView.getMeasuredHeight())
        mCameraEmptyView.layout(viewLeft, viewTop, viewLeft + mCameraView.getMeasuredWidth(), viewTop + mCameraView.getMeasuredHeight())
        viewTop = getMeasuredWidth() - ScreenUtils.dip2px(getContext(), 12) - mSwitchView.measuredHeight
        viewLeft = ScreenUtils.dip2px(getContext(), 14)
        mSwitchView.layout(viewLeft, viewTop, viewLeft + mSwitchView.measuredWidth, viewTop + mSwitchView.measuredHeight)
        viewLeft = getMeasuredWidth() - ScreenUtils.dip2px(getContext(), 10) - mFlashView.measuredWidth
        mFlashView.layout(viewLeft, viewTop, viewLeft + mFlashView.measuredWidth, viewTop + mFlashView.measuredHeight)
        viewTop = getMeasuredWidth() - ScreenUtils.dip2px(getContext(), 2)
        viewLeft = 0
        mCaptureLayout.layout(viewLeft, viewTop, viewLeft + mCaptureLayout.getMeasuredWidth(), viewTop + mCaptureLayout.getMeasuredHeight())
    }

    fun setCaptureButtonTranslationX(translationX: Float) {
        if (mCaptureLayout != null) {
            mCaptureLayout.setCaptureButtonTranslationX(translationX)
        }
    }

    fun setCameraState(cameraState: Int) {
        if (mCameraState == cameraState || mCaptureLayout == null) {
            return
        }
        mCameraState = cameraState
        if (mCameraState == STATE_CAPTURE) {
            InstagramUtils.setViewVisibility(mFlashView, View.VISIBLE)
        } else if (mCameraState == STATE_RECORDER) {
            InstagramUtils.setViewVisibility(mFlashView, View.INVISIBLE)
        }
        mCaptureLayout.setCameraState(cameraState)
    }

    fun getCameraView(): CameraView? {
        return mCameraView
    }

    fun isInLongPress(): Boolean {
        return if (mCaptureLayout == null) {
            false
        } else mCaptureLayout.isInLongPress()
    }

    private fun setFlashRes() {
        if (mCameraView == null) {
            return
        }
        when (mTypeFlash) {
            TYPE_FLASH_AUTO -> {
                mFlashView.setImageResource(R.drawable.discover_flash_a)
                mCameraView.setFlash(ImageCapture.FLASH_MODE_AUTO)
            }
            TYPE_FLASH_ON -> {
                mFlashView.setImageResource(R.drawable.discover_flash_on)
                mCameraView.setFlash(ImageCapture.FLASH_MODE_ON)
            }
            TYPE_FLASH_OFF -> {
                mFlashView.setImageResource(R.drawable.discover_flash_off)
                mCameraView.setFlash(ImageCapture.FLASH_MODE_OFF)
            }
            else -> {
            }
        }
    }

    fun disallowInterceptTouchRect(): Rect? {
        return if (mCaptureLayout == null) {
            null
        } else mCaptureLayout.disallowInterceptTouchRect()
    }

    fun setRecordVideoMaxTime(maxDurationTime: Int) {
        if (mCaptureLayout != null) {
            mCaptureLayout.setRecordVideoMaxTime(maxDurationTime)
        }
    }

    fun setRecordVideoMinTime(minDurationTime: Int) {
        if (mCaptureLayout != null) {
            mCaptureLayout.setRecordVideoMinTime(minDurationTime)
        }
    }

    fun setCameraListener(cameraListener: CameraListener?) {
        mCameraListener = cameraListener
    }

    fun setEmptyViewVisibility(visibility: Int) {
        InstagramUtils.setViewVisibility(mCameraEmptyView, visibility)
    }

    fun onResume() {
        if (mCameraView != null && mCameraView.isRecording()) {
            mCameraView.stopRecording()
        }
    }

    fun onPause() {
        if (mCameraView != null && mCameraView.isRecording()) {
            mRecordTime = 0
            mCameraView.stopRecording()
        }
    }

    @SuppressLint("RestrictedApi")
    fun release() {
        if (mCaptureLayout != null) {
            mCaptureLayout.release()
        }
        mCameraView = null
        mCaptureLayout = null
        mConfig = null
        mActivity = null
        mCameraListener = null
    }

    private class OnImageSavedCallbackImpl internal constructor(cameraView: InstagramCameraView, imageOutFile: File?) : ImageCapture.OnImageSavedCallback {
        private val mCameraView: WeakReference<InstagramCameraView>
        private val mImageOutFile: File?
        override fun onImageSaved(outputFileResults: OutputFileResults) {
            val cameraView = mCameraView.get() ?: return
            if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(cameraView.mConfig.cameraPath)) {
                PictureThreadUtils.executeBySingle(object : SimpleTask<Boolean?>() {
                    fun doInBackground(): Boolean {
                        return AndroidQTransformUtils.copyPathToDCIM(cameraView.getContext(),
                                mImageOutFile, Uri.parse(cameraView.mConfig.cameraPath))
                    }

                    fun onSuccess(result: Boolean) {
                        if (result && cameraView.mCameraListener != null) {
                            cameraView.mCameraListener.onPictureSuccess(mImageOutFile!!)
                        }
                        PictureThreadUtils.cancel(PictureThreadUtils.getSinglePool())
                    }
                })
            } else if (mImageOutFile != null && mImageOutFile.exists() && cameraView.mCameraListener != null) {
                cameraView.mCameraListener.onPictureSuccess(mImageOutFile)
            }
        }

        override fun onError(exception: ImageCaptureException) {
            val cameraView = mCameraView.get()
            if (cameraView != null && cameraView.mCameraListener != null) {
                cameraView.mCameraListener.onError(exception.getImageCaptureError(), exception.message, exception.cause)
            }
        }

        init {
            mCameraView = WeakReference(cameraView)
            mImageOutFile = imageOutFile
        }
    }

    private class OnVideoSavedCallbackImpl internal constructor(cameraView: InstagramCameraView) : OnVideoSavedCallback {
        private val mCameraView: WeakReference<InstagramCameraView>
        override fun onVideoSaved(file: File) {
            val cameraView = mCameraView.get() ?: return
            if (cameraView.mRecordTime < cameraView.mConfig.recordVideoMinSecond) {
                if (file.exists()) {
                    file.delete()
                }
                return
            }
            if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(cameraView.mConfig.cameraPath)) {
                PictureThreadUtils.executeBySingle(object : SimpleTask<Boolean?>() {
                    fun doInBackground(): Boolean {
                        return AndroidQTransformUtils.copyPathToDCIM(cameraView.getContext(),
                                file, Uri.parse(cameraView.mConfig.cameraPath))
                    }

                    fun onSuccess(result: Boolean) {
                        if (result && cameraView.mCameraListener != null) {
                            cameraView.mCameraListener.onRecordSuccess(file)
                        }
                        PictureThreadUtils.cancel(PictureThreadUtils.getSinglePool())
                    }
                })
            } else if (file.exists() && cameraView.mCameraListener != null) {
                cameraView.mCameraListener.onRecordSuccess(file)
            }
        }

        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
            val cameraView = mCameraView.get()
            if (cameraView != null) {
                cameraView.mCaptureLayout!!.resetRecordEnd()
                if (cameraView.mCameraListener != null) {
                    cameraView.mCameraListener.onError(videoCaptureError, message, cause)
                }
            }
        }

        init {
            mCameraView = WeakReference(cameraView)
        }
    }

    companion object {
        const val STATE_CAPTURE = 1
        const val STATE_RECORDER = 2
        private const val TYPE_FLASH_AUTO = 0x021
        private const val TYPE_FLASH_ON = 0x022
        private const val TYPE_FLASH_OFF = 0x023
    }

    init {
        mActivity = WeakReference<AppCompatActivity?>(activity)
        mConfig = config
        mCameraView = CameraView(context)
        addView(mCameraView)
        mSwitchView = ImageView(context)
        mSwitchView.setImageResource(R.drawable.discover_flip)
        mSwitchView.setOnClickListener { v: View? ->
            if (mCaptureLayout!!.isInLongPress()) {
                return@setOnClickListener
            }
            isFront = !isFront
            ObjectAnimator.ofFloat(mSwitchView, "rotation", mSwitchView.rotation - 180f).setDuration(400).start()
            mCameraView.toggleCamera()
        }
        addView(mSwitchView)
        mFlashView = ImageView(context)
        mFlashView.setImageResource(R.drawable.discover_flash_off)
        mFlashView.setOnClickListener { v: View? ->
            mTypeFlash++
            if (mTypeFlash > TYPE_FLASH_OFF) {
                mTypeFlash = TYPE_FLASH_AUTO
            }
            setFlashRes()
        }
        addView(mFlashView)
        mCaptureLayout = InstagramCaptureLayout(context, config)
        addView(mCaptureLayout)
        mCaptureLayout.setCaptureListener(object : InstagramCaptureListener() {
            override fun takePictures() {
                if (mCameraView == null) {
                    return
                }
                mCameraView.setCaptureMode(CameraView.CaptureMode.IMAGE)
                val imageOutFile = createImageFile()
                val options: OutputFileOptions = OutputFileOptions.Builder(imageOutFile).build()
                mCameraView.takePicture(options, ContextCompat.getMainExecutor(getContext().getApplicationContext()), OnImageSavedCallbackImpl(this@InstagramCameraView, imageOutFile))
            }

            override fun recordStart() {
                if (mCameraView == null) {
                    return
                }
                mCameraView.setCaptureMode(CameraView.CaptureMode.VIDEO)
                mCameraView.startRecording(createVideoFile(), ContextCompat.getMainExecutor(getContext().getApplicationContext()), OnVideoSavedCallbackImpl(this@InstagramCameraView))
            }

            override fun recordEnd(time: Long) {
                if (mCameraView == null) {
                    return
                }
                mRecordTime = time
                mCameraView.stopRecording()
            }

            override fun recordShort(time: Long) {
                if (mCameraView == null) {
                    return
                }
                mRecordTime = time
                mCameraView.stopRecording()
            }

            override fun recordError() {
                if (mCameraListener != null) {
                    mCameraListener.onError(-1, "No permission", null)
                }
            }
        })
        mCameraEmptyView = InstagramCameraEmptyView(context, config)
        addView(mCameraEmptyView)
        mCameraEmptyView.setVisibility(View.INVISIBLE)
    }
}