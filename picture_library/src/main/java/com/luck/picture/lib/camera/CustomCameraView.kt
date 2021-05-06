package com.luck.picture.lib.camera

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.widget.ImageView
import androidx.camera.view.CameraView
import androidx.lifecycle.Lifecycle
import com.luck.picture.lib.camera.listener.CameraListener
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * @author：luck
 * @date：2020-01-04 13:41
 * @describe：自定义相机View
 */
class CustomCameraView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {
    private var type_flash = TYPE_FLASH_OFF
    private var mConfig: PictureSelectionConfig? = null

    /**
     * 回调监听
     */
    private var mCameraListener: CameraListener? = null
    private var mOnClickListener: ClickListener? = null
    private var mImageCallbackListener: ImageCallbackListener? = null
    private var mCameraView: CameraView? = null
    private var mImagePreview: ImageView? = null
    private var mSwitchCamera: ImageView? = null
    private var mFlashLamp: ImageView? = null
    private var mCaptureLayout: CaptureLayout? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mTextureView: TextureView? = null
    private var recordTime: Long = 0
    private var mVideoFile: File? = null
    private var mPhotoFile: File? = null
    fun initView() {
        setWillNotDraw(false)
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.picture_color_black))
        val view: View = LayoutInflater.from(getContext()).inflate(R.layout.picture_camera_view, this)
        mCameraView = view.findViewById(R.id.cameraView)
        mCameraView.enableTorch(true)
        mTextureView = view.findViewById<TextureView>(R.id.video_play_preview)
        mImagePreview = view.findViewById(R.id.image_preview)
        mSwitchCamera = view.findViewById(R.id.image_switch)
        mSwitchCamera.setImageResource(R.drawable.picture_ic_camera)
        mFlashLamp = view.findViewById(R.id.image_flash)
        setFlashRes()
        mFlashLamp.setOnClickListener(View.OnClickListener { v: View? ->
            type_flash++
            if (type_flash > 0x023) type_flash = TYPE_FLASH_AUTO
            setFlashRes()
        })
        mCaptureLayout = view.findViewById(R.id.capture_layout)
        mCaptureLayout.setDuration(15 * 1000)
        //切换摄像头
        mSwitchCamera.setOnClickListener(View.OnClickListener { v: View? -> mCameraView.toggleCamera() })
        //拍照 录像
        mCaptureLayout.setCaptureListener(object : CaptureListener() {
            fun takePictures() {
                mSwitchCamera.setVisibility(View.INVISIBLE)
                mFlashLamp.setVisibility(View.INVISIBLE)
                mCameraView.setCaptureMode(CameraView.CaptureMode.IMAGE)
                val imageOutFile = createImageFile() ?: return
                mPhotoFile = imageOutFile
                val options: OutputFileOptions = OutputFileOptions.Builder(imageOutFile).build()
                mCameraView.takePicture(options, ContextCompat.getMainExecutor(getContext()),
                        MyImageResultCallback(getContext(), mConfig, imageOutFile,
                                mImagePreview, mCaptureLayout, mImageCallbackListener, mCameraListener))
            }

            fun recordStart() {
                mSwitchCamera.setVisibility(View.INVISIBLE)
                mFlashLamp.setVisibility(View.INVISIBLE)
                mCameraView.setCaptureMode(CameraView.CaptureMode.VIDEO)
                mCameraView.startRecording(createVideoFile(), ContextCompat.getMainExecutor(getContext()),
                        object : OnVideoSavedCallback {
                            override fun onVideoSaved(file: File) {
                                mVideoFile = file
                                if (recordTime < 1500 && mVideoFile!!.exists() && mVideoFile!!.delete()) {
                                    return
                                }
                                if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mConfig.cameraPath)) {
                                    PictureThreadUtils.executeByIo(object : SimpleTask<Boolean?>() {
                                        fun doInBackground(): Boolean {
                                            return AndroidQTransformUtils.copyPathToDCIM(getContext(),
                                                    file, Uri.parse(mConfig.cameraPath))
                                        }

                                        fun onSuccess(result: Boolean?) {
                                            PictureThreadUtils.cancel(PictureThreadUtils.getIoPool())
                                        }
                                    })
                                }
                                mTextureView.setVisibility(View.VISIBLE)
                                mCameraView.setVisibility(View.INVISIBLE)
                                if (mTextureView.isAvailable()) {
                                    startVideoPlay(mVideoFile)
                                } else {
                                    mTextureView.setSurfaceTextureListener(surfaceTextureListener)
                                }
                            }

                            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                                if (mCameraListener != null) {
                                    mCameraListener.onError(videoCaptureError, message, cause)
                                }
                            }
                        })
            }

            fun recordShort(time: Long) {
                recordTime = time
                mSwitchCamera.setVisibility(View.VISIBLE)
                mFlashLamp.setVisibility(View.VISIBLE)
                mCaptureLayout.resetCaptureLayout()
                mCaptureLayout.setTextWithAnimation(getContext().getString(R.string.picture_recording_time_is_short))
                mCameraView.stopRecording()
            }

            fun recordEnd(time: Long) {
                recordTime = time
                mCameraView.stopRecording()
            }

            fun recordZoom(zoom: Float) {}
            fun recordError() {
                if (mCameraListener != null) {
                    mCameraListener.onError(0, "An unknown error", null)
                }
            }
        })
        //确认 取消
        mCaptureLayout.setTypeListener(object : TypeListener() {
            fun cancel() {
                stopVideoPlay()
                resetState()
            }

            fun confirm() {
                if (mCameraView.getCaptureMode() == CameraView.CaptureMode.VIDEO) {
                    if (mVideoFile == null) {
                        return
                    }
                    stopVideoPlay()
                    if (mCameraListener != null || !mVideoFile!!.exists()) {
                        mCameraListener!!.onRecordSuccess(mVideoFile!!)
                    }
                } else {
                    if (mPhotoFile == null || !mPhotoFile!!.exists()) {
                        return
                    }
                    mImagePreview.setVisibility(View.INVISIBLE)
                    if (mCameraListener != null) {
                        mCameraListener.onPictureSuccess(mPhotoFile!!)
                    }
                }
            }
        })
        mCaptureLayout.setLeftClickListener {
            if (mOnClickListener != null) {
                mOnClickListener.onClick()
            }
        }
    }

    /**
     * 拍照回调
     */
    private class MyImageResultCallback(context: Context, config: PictureSelectionConfig?,
                                        imageOutFile: File?, imagePreview: ImageView?,
                                        captureLayout: CaptureLayout?, imageCallbackListener: ImageCallbackListener?,
                                        cameraListener: CameraListener?) : ImageCapture.OnImageSavedCallback {
        private val mContextReference: WeakReference<Context>
        private val mConfigReference: WeakReference<PictureSelectionConfig?>
        private val mFileReference: WeakReference<File?>
        private val mImagePreviewReference: WeakReference<ImageView?>
        private val mCaptureLayoutReference: WeakReference<CaptureLayout?>
        private val mImageCallbackListenerReference: WeakReference<ImageCallbackListener?>
        private val mCameraListenerReference: WeakReference<CameraListener?>
        override fun onImageSaved(outputFileResults: OutputFileResults) {
            if (mConfigReference.get() != null) {
                if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mConfigReference.get().cameraPath)) {
                    PictureThreadUtils.executeByIo(object : SimpleTask<Boolean?>() {
                        fun doInBackground(): Boolean {
                            return AndroidQTransformUtils.copyPathToDCIM(mContextReference.get(),
                                    mFileReference.get(), Uri.parse(mConfigReference.get().cameraPath))
                        }

                        fun onSuccess(result: Boolean?) {
                            PictureThreadUtils.cancel(PictureThreadUtils.getIoPool())
                        }
                    })
                }
            }
            if (mImageCallbackListenerReference.get() != null && mFileReference.get() != null && mImagePreviewReference.get() != null) {
                mImageCallbackListenerReference.get().onLoadImage(mFileReference.get(), mImagePreviewReference.get())
            }
            if (mImagePreviewReference.get() != null) {
                mImagePreviewReference.get()!!.visibility = View.VISIBLE
            }
            if (mCaptureLayoutReference.get() != null) {
                mCaptureLayoutReference.get().startTypeBtnAnimator()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            if (mCameraListenerReference.get() != null) {
                mCameraListenerReference.get().onError(exception.getImageCaptureError(), exception.message, exception.cause)
            }
        }

        init {
            mContextReference = WeakReference(context)
            mConfigReference = WeakReference<PictureSelectionConfig?>(config)
            mFileReference = WeakReference(imageOutFile)
            mImagePreviewReference = WeakReference(imagePreview)
            mCaptureLayoutReference = WeakReference<CaptureLayout?>(captureLayout)
            mImageCallbackListenerReference = WeakReference<ImageCallbackListener?>(imageCallbackListener)
            mCameraListenerReference = WeakReference<CameraListener?>(cameraListener)
        }
    }

    private val surfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            startVideoPlay(mVideoFile)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    fun createImageFile(): File? {
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
            if (cameraFile != null) {
                mConfig.cameraPath = cameraFile.absolutePath
            }
            cameraFile
        }
    }

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

    private fun getOutUri(type: Int): Uri {
        return if (type == PictureMimeType.ofVideo()) MediaUtils.createVideoUri(getContext(), mConfig.suffixType) else MediaUtils.createImageUri(getContext(), mConfig.suffixType)
    }

    fun setCameraListener(cameraListener: CameraListener?) {
        mCameraListener = cameraListener
    }

    fun setPictureSelectionConfig(config: PictureSelectionConfig?) {
        mConfig = config
    }

    fun setBindToLifecycle(lifecycleOwner: LifecycleOwner) {
        mCameraView.bindToLifecycle(lifecycleOwner)
        lifecycleOwner.getLifecycle().addObserver(LifecycleEventObserver { source: LifecycleOwner?, event: Lifecycle.Event? -> } as LifecycleEventObserver?)
    }

    /**
     * 设置录制视频最大时长 秒
     */
    fun setRecordVideoMaxTime(maxDurationTime: Int) {
        mCaptureLayout.setDuration(maxDurationTime * 1000)
    }

    /**
     * 设置录制视频最小时长 秒
     */
    fun setRecordVideoMinTime(minDurationTime: Int) {
        mCaptureLayout.setMinDuration(minDurationTime * 1000)
    }

    /**
     * 关闭相机界面按钮
     *
     * @param clickListener
     */
    fun setOnClickListener(clickListener: ClickListener?) {
        mOnClickListener = clickListener
    }

    fun setImageCallbackListener(mImageCallbackListener: ImageCallbackListener?) {
        this.mImageCallbackListener = mImageCallbackListener
    }

    private fun setFlashRes() {
        when (type_flash) {
            TYPE_FLASH_AUTO -> {
                mFlashLamp!!.setImageResource(R.drawable.picture_ic_flash_auto)
                mCameraView.setFlash(ImageCapture.FLASH_MODE_AUTO)
            }
            TYPE_FLASH_ON -> {
                mFlashLamp!!.setImageResource(R.drawable.picture_ic_flash_on)
                mCameraView.setFlash(ImageCapture.FLASH_MODE_ON)
            }
            TYPE_FLASH_OFF -> {
                mFlashLamp!!.setImageResource(R.drawable.picture_ic_flash_off)
                mCameraView.setFlash(ImageCapture.FLASH_MODE_OFF)
            }
        }
    }

    val cameraView: CameraView?
        get() = mCameraView
    val captureLayout: CaptureLayout?
        get() = mCaptureLayout

    /**
     * 重置状态
     */
    private fun resetState() {
        if (mCameraView.getCaptureMode() == CameraView.CaptureMode.VIDEO) {
            if (mCameraView.isRecording()) {
                mCameraView.stopRecording()
            }
            if (mVideoFile != null && mVideoFile!!.exists()) {
                mVideoFile!!.delete()
                if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mConfig.cameraPath)) {
                    getContext().getContentResolver().delete(Uri.parse(mConfig.cameraPath), null, null)
                } else {
                    PictureMediaScannerConnection(getContext(), mVideoFile!!.absolutePath)
                }
            }
        } else {
            mImagePreview!!.visibility = View.INVISIBLE
            if (mPhotoFile != null && mPhotoFile!!.exists()) {
                mPhotoFile!!.delete()
                if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mConfig.cameraPath)) {
                    getContext().getContentResolver().delete(Uri.parse(mConfig.cameraPath), null, null)
                } else {
                    PictureMediaScannerConnection(getContext(), mPhotoFile!!.absolutePath)
                }
            }
        }
        mSwitchCamera!!.visibility = View.VISIBLE
        mFlashLamp!!.visibility = View.VISIBLE
        mCameraView.setVisibility(View.VISIBLE)
        mCaptureLayout.resetCaptureLayout()
    }

    /**
     * 开始循环播放视频
     *
     * @param videoFile
     */
    private fun startVideoPlay(videoFile: File?) {
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer()
            }
            mMediaPlayer.setDataSource(videoFile!!.absolutePath)
            mMediaPlayer.setSurface(Surface(mTextureView.getSurfaceTexture()))
            mMediaPlayer.setLooping(true)
            mMediaPlayer.setOnPreparedListener(OnPreparedListener { mp: MediaPlayer ->
                mp.start()
                val ratio: Float = mp.getVideoWidth() * 1f / mp.getVideoHeight()
                val width1: Int = mTextureView.getWidth()
                val layoutParams: ViewGroup.LayoutParams = mTextureView.getLayoutParams()
                layoutParams.height = (width1 / ratio).toInt()
                mTextureView.setLayoutParams(layoutParams)
            })
            mMediaPlayer.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 停止视频播放
     */
    private fun stopVideoPlay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop()
            mMediaPlayer.release()
            mMediaPlayer = null
        }
        mTextureView.setVisibility(View.GONE)
    }

    companion object {
        /**
         * 只能拍照
         */
        const val BUTTON_STATE_ONLY_CAPTURE = 0x101

        /**
         * 只能录像
         */
        const val BUTTON_STATE_ONLY_RECORDER = 0x102

        /**
         * 两者都可以
         */
        const val BUTTON_STATE_BOTH = 0x103

        /**
         * 闪关灯状态
         */
        private const val TYPE_FLASH_AUTO = 0x021
        private const val TYPE_FLASH_ON = 0x022
        private const val TYPE_FLASH_OFF = 0x023
    }

    init {
        initView()
    }
}