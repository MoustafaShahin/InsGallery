package com.luck.picture.lib.instagram

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureMimeType
import java.lang.ref.WeakReference
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/3/30 16:33
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramPreviewContainer(context: Context, config: PictureSelectionConfig) : FrameLayout(context) {
    private var mUCropView: UCropView?
    private var mVideoView: VideoView?
    private val config: PictureSelectionConfig
    private val mThumbView: ImageView
    private var mGestureCropImageView: GestureCropImageView?
    private var mOverlayView: OverlayView?
    private val mRatioView: ImageView
    private val mMultiView: ImageView
    private var mCropGridShowing = false
    private var mHandler: Handler?
    private var isAspectRatio = false
    private var isMulti = false
    private var mListener: onSelectionModeChangedListener? = null
    private var mPlayMode = 0
    private var mMediaPlayer: MediaPlayer? = null
    private var isPause = false
    private var mThumbAnimator: ObjectAnimator? = null
    private val mPlayButton: ImageView
    private var isLoadingVideo = false
    private var mPositionWhenPaused = -1
    private var mPlayVideoRunnable: PlayVideoRunnable? = null
    private var mShowGridRunnable: ShowGridRunnable? = null
    private val mImageListener: TransformImageView.TransformImageListener = object : TransformImageListener() {
        fun onRotate(currentAngle: Float) {}
        fun onScale(currentScale: Float) {}
        fun onBitmapLoadComplete(bitmap: Bitmap) {
            resetAspectRatio()
        }

        fun onLoadComplete() {}
        fun onLoadFailure(e: Exception) {}
    }
    private var mAnimatorSet: AnimatorSet? = null
    private var mPlayAnimator: ObjectAnimator? = null
    private var mAspectRadio = 0f
    fun setMultiMode(multi: Boolean) {
        isMulti = multi
        if (multi) {
            mRatioView.visibility = View.GONE
        } else {
            mRatioView.visibility = View.VISIBLE
        }
        if (mListener != null) {
            mListener!!.onSelectionModeChange(multi)
        }
    }

    fun isMulti(): Boolean {
        return isMulti
    }

    private fun pauseVideo() {
        pauseVideo(!isPause)
    }

    fun pauseVideo(pause: Boolean) {
        if (isPause == pause) {
            return
        }
        isPause = pause
        if (mPlayMode != PLAY_VIDEO_MODE) {
            return
        }
        if (mPlayAnimator != null && mPlayAnimator.isRunning()) {
            mPlayAnimator.cancel()
        }
        if (pause) {
            mPlayButton.visibility = View.VISIBLE
            mPlayAnimator = ObjectAnimator.ofFloat(mPlayButton, "alpha", 0f, 1.0f).setDuration(200)
            mVideoView.pause()
        } else {
            mPlayAnimator = ObjectAnimator.ofFloat(mPlayButton, "alpha", 1.0f, 0f).setDuration(200)
            mPlayAnimator.addListener(object : AnimatorListenerImpl() {
                fun onAnimationEnd(animation: Animator?) {
                    mPlayButton.visibility = View.GONE
                }
            })
            mVideoView.start()
        }
        mPlayAnimator.start()
    }

    private fun resetAspectRatio() {
        mAspectRadio = 0f
        if (isAspectRatio) {
            val drawable: Drawable = mGestureCropImageView.getDrawable()
            if (drawable != null) {
                mAspectRadio = getInstagramAspectRatio(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight())
            }
        }
        mGestureCropImageView.setTargetAspectRatio(if (isAspectRatio) mAspectRadio else 1.0f)
        mGestureCropImageView.onImageLaidOut()
    }

    fun isAspectRatio(): Boolean {
        return isAspectRatio
    }

    fun getAspectRadio(): Float {
        return mAspectRadio
    }

    fun setImageUri(inputUri: Uri, outputUri: Uri?) {
        if (mPlayMode != PLAY_IMAGE_MODE) {
            return
        }
        if (inputUri != null && outputUri != null) {
            try {
                val isOnTouch = isOnTouch(inputUri)
                mGestureCropImageView.setScaleEnabled(if (isOnTouch) true else isOnTouch)
                mGestureCropImageView.setImageUri(inputUri, outputUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playVideo(media: LocalMedia?, holder: RecyclerView.ViewHolder?) {
        if (mPlayMode != PLAY_VIDEO_MODE) {
            return
        }
        if (mThumbAnimator != null && mThumbAnimator.isRunning()) {
            mThumbAnimator.cancel()
        }
        if (mPlayVideoRunnable != null) {
            mHandler!!.removeCallbacks(mPlayVideoRunnable)
        }
        var drawable: Drawable? = null
        if (holder != null && holder is InstagramImageGridAdapter.ViewHolder) {
            drawable = (holder as InstagramImageGridAdapter.ViewHolder).ivPicture.getDrawable()
        }
        if (drawable != null) {
            mThumbView.setImageDrawable(drawable)
        } else {
            mThumbView.setImageDrawable(null)
            if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
                mThumbView.setBackgroundColor(Color.BLACK)
            } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
                mThumbView.setBackgroundColor(Color.parseColor("#18222D"))
            } else {
                mThumbView.setBackgroundColor(Color.WHITE)
            }
        }
        mPlayButton.visibility = View.GONE
        isLoadingVideo = false
        mPlayVideoRunnable = PlayVideoRunnable(this, media)
        mHandler!!.postDelayed(mPlayVideoRunnable, 600)
    }

    fun checkModel(mode: Int) {
        mPlayMode = mode
        if (mVideoView.getVisibility() == View.VISIBLE && mVideoView.isPlaying()) {
            mVideoView.pause()
            isPause = true
        }
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel()
        }
        mAnimatorSet = AnimatorSet()
        val animators: MutableList<Animator> = ArrayList()
        if (mode == PLAY_IMAGE_MODE) {
            InstagramUtils.setViewVisibility(mUCropView, View.VISIBLE)
            animators.add(ObjectAnimator.ofFloat(mUCropView, "alpha", 0.1f, 1.0f))
            mAnimatorSet.addListener(object : AnimatorListenerImpl() {
                fun onAnimationEnd(animation: Animator?) {
                    InstagramUtils.setViewVisibility(mVideoView, View.GONE)
                    InstagramUtils.setViewVisibility(mThumbView, View.GONE)
                }
            })
        } else if (mode == PLAY_VIDEO_MODE) {
            InstagramUtils.setViewVisibility(mVideoView, View.VISIBLE)
            InstagramUtils.setViewVisibility(mThumbView, View.VISIBLE)
            InstagramUtils.setViewVisibility(mUCropView, View.GONE)
            animators.add(ObjectAnimator.ofFloat(mVideoView, "alpha", 0f, 1.0f))
            animators.add(ObjectAnimator.ofFloat(mThumbView, "alpha", 0.1f, 1.0f))
        }
        mAnimatorSet.setDuration(800)
        mAnimatorSet.playTogether(animators)
        mAnimatorSet.start()
    }

    /**
     * 是否可以触摸
     *
     * @param inputUri
     * @return
     */
    private fun isOnTouch(inputUri: Uri?): Boolean {
        if (inputUri == null) {
            return true
        }
        val isHttp: Boolean = MimeType.isHttp(inputUri.toString())
        return if (isHttp) {
            // 网络图片
            val lastImgType: String = MimeType.getLastImgType(inputUri.toString())
            !MimeType.isGifForSuffix(lastImgType)
        } else {
            var mimeType: String = MimeType.getMimeTypeFromMediaContentUri(getContext(), inputUri)
            if (mimeType.endsWith("image/*")) {
                val path: String = FileUtils.getPath(getContext(), inputUri)
                mimeType = MimeType.getImageMimeType(path)
            }
            !MimeType.isGif(mimeType)
        }
    }

    fun setListener(listener: onSelectionModeChangedListener?) {
        mListener = listener
    }

    /**
     * 修改预览View的大小, 以用来适配屏幕
     */
    fun changeVideoSize(mediaPlayer: MediaPlayer?, isAspectRatio: Boolean) {
        if (mediaPlayer == null || mVideoView == null) {
            return
        }
        try {
            mediaPlayer.getVideoWidth()
        } catch (e: Exception) {
            return
        }
        val videoWidth: Int = mediaPlayer.getVideoWidth()
        val videoHeight: Int = mediaPlayer.getVideoHeight()
        val parentWidth: Int = getMeasuredWidth()
        val parentHeight: Int = getMeasuredHeight()
        val instagramAspectRatio = getInstagramAspectRatio(videoWidth, videoHeight)
        val targetAspectRatio = videoWidth * 1.0f / videoHeight
        val height = (parentWidth / targetAspectRatio).toInt()
        val adjustWidth: Int
        val adjustHeight: Int
        if (isAspectRatio) {
            if (height > parentHeight) {
                adjustWidth = (parentWidth * if (instagramAspectRatio > 0) instagramAspectRatio else targetAspectRatio).toInt()
                adjustHeight = height
            } else {
                if (instagramAspectRatio > 0) {
                    adjustWidth = (parentHeight * targetAspectRatio).toInt()
                    adjustHeight = (parentHeight / instagramAspectRatio).toInt()
                } else {
                    adjustWidth = parentWidth
                    adjustHeight = height
                }
            }
        } else {
            if (height < parentHeight) {
                adjustWidth = (parentHeight * targetAspectRatio).toInt()
                adjustHeight = parentHeight
            } else {
                adjustWidth = parentWidth
                adjustHeight = height
            }
        }
        val layoutParams: FrameLayout.LayoutParams = mVideoView.getLayoutParams() as FrameLayout.LayoutParams
        layoutParams.width = adjustWidth
        layoutParams.height = adjustHeight
        mVideoView.setLayoutParams(layoutParams)
    }

    fun onPause() {
        // Stop video when the activity is pause.
        if (mPlayMode == PLAY_VIDEO_MODE) {
            mPositionWhenPaused = mVideoView.getCurrentPosition()
            mVideoView.stopPlayback()
        }
    }

    fun createCropAndSaveImageTask(cropCallback: BitmapCropCallback?): AsyncTask<*, *, *> {
        return mGestureCropImageView.createCropAndSaveImageTask(UCropActivity.DEFAULT_COMPRESS_FORMAT, UCropActivity.DEFAULT_COMPRESS_QUALITY, cropCallback)
    }

    fun cropAndSaveImage(activity: PictureSelectorInstagramStyleActivity) {
        mGestureCropImageView.cropAndSaveImage(UCropActivity.DEFAULT_COMPRESS_FORMAT, UCropActivity.DEFAULT_COMPRESS_QUALITY, object : BitmapCropCallback() {
            fun onBitmapCropped(resultUri: Uri, offsetX: Int, offsetY: Int, imageWidth: Int, imageHeight: Int) {
                setResultUri(activity, resultUri, mGestureCropImageView.getTargetAspectRatio(), offsetX, offsetY, imageWidth, imageHeight)
            }

            fun onCropFailure(t: Throwable) {
                setResultError(activity, t)
            }
        })
    }

    protected fun setResultUri(activity: PictureSelectorInstagramStyleActivity, uri: Uri?, resultAspectRatio: Float, offsetX: Int, offsetY: Int, imageWidth: Int, imageHeight: Int) {
        activity.onActivityResult(UCrop.REQUEST_CROP, Activity.RESULT_OK, Intent()
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY))
    }

    protected fun setResultError(activity: PictureSelectorInstagramStyleActivity, throwable: Throwable?) {
        activity.onActivityResult(UCrop.REQUEST_CROP, UCrop.RESULT_ERROR, Intent().putExtra(UCrop.EXTRA_ERROR, throwable))
    }

    fun getPlayMode(): Int {
        return mPlayMode
    }

    fun onResume() {
        // Resume video player
        if (mPlayMode == PLAY_VIDEO_MODE) {
            if (!mVideoView.isPlaying()) {
                mVideoView.start()
            }
            if (isPause) {
                mPlayButton.visibility = View.GONE
                isPause = false
            }
            if (mPositionWhenPaused >= 0) {
                mVideoView.seekTo(mPositionWhenPaused)
                mPositionWhenPaused = -1
            }
        }
    }

    fun onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release()
            mMediaPlayer = null
        }
        if (mThumbAnimator != null && mThumbAnimator.isRunning()) {
            mThumbAnimator.cancel()
            mThumbAnimator = null
        }
        if (mHandler != null) {
            mHandler!!.removeCallbacksAndMessages(null)
            mHandler = null
        }
        mListener = null
        mVideoView = null
        mUCropView = null
        mGestureCropImageView = null
        mOverlayView = null
    }

    private class PlayVideoRunnable internal constructor(previewContainer: InstagramPreviewContainer, media: LocalMedia?) : Runnable {
        private val mPreviewContainer: WeakReference<InstagramPreviewContainer>
        private val mMedia: LocalMedia?
        override fun run() {
            val previewContainer = mPreviewContainer.get() ?: return
            if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mMedia.getPath())) {
                previewContainer.mVideoView.setVideoURI(Uri.parse(mMedia.getPath()))
            } else {
                previewContainer.mVideoView.setVideoPath(mMedia.getPath())
            }
            previewContainer.mVideoView.start()
            previewContainer.isPause = false
            previewContainer.isLoadingVideo = true
        }

        init {
            mPreviewContainer = WeakReference(previewContainer)
            mMedia = media
        }
    }

    private class ShowGridRunnable internal constructor(previewContainer: InstagramPreviewContainer) : Runnable {
        private val mPreviewContainer: WeakReference<InstagramPreviewContainer>
        override fun run() {
            val previewContainer = mPreviewContainer.get() ?: return
            if (previewContainer.mCropGridShowing) {
                previewContainer.mOverlayView.setShowCropGrid(false)
                previewContainer.mOverlayView.invalidate()
                previewContainer.mCropGridShowing = false
            }
        }

        init {
            mPreviewContainer = WeakReference(previewContainer)
        }
    }

    interface onSelectionModeChangedListener {
        fun onSelectionModeChange(isMulti: Boolean)
        fun onRatioChange(isOneToOne: Boolean)
    }

    companion object {
        const val PLAY_IMAGE_MODE = 0
        const val PLAY_VIDEO_MODE = 1
        fun getInstagramAspectRatio(width: Int, height: Int): Float {
            var aspectRatio = 0f
            if (height > width * 1.266f) {
                aspectRatio = width / (width * 1.266f)
            } else if (width > height * 1.9f) {
                aspectRatio = height * 1.9f / height
            }
            return aspectRatio
        }
    }

    init {
        this.config = config
        mHandler = Handler(context.mainLooper)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            setBackgroundColor(Color.parseColor("#363636"))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            setBackgroundColor(Color.parseColor("#004561"))
        } else {
            setBackgroundColor(Color.parseColor("#efefef"))
        }
        mVideoView = VideoView(context)
        addView(mVideoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        mVideoView.setOnClickListener(View.OnClickListener { v: View? -> pauseVideo() })
        mVideoView.setOnPreparedListener(OnPreparedListener { mp: MediaPlayer ->
            mMediaPlayer = mp
            mp.setLooping(true)
            changeVideoSize(mp, isAspectRatio)
            mp.setOnInfoListener(MediaPlayer.OnInfoListener { mp1: MediaPlayer?, what: Int, extra: Int ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    // video started
                    if (mThumbView.visibility == View.VISIBLE && isLoadingVideo) {
                        isLoadingVideo = false
                        mThumbAnimator = ObjectAnimator.ofFloat(mThumbView, "alpha", 1.0f, 0f).setDuration(400)
                        mThumbAnimator.start()
                    }
                    return@setOnInfoListener true
                }
                false
            })
        })
        mThumbView = ImageView(context)
        mThumbView.scaleType = ImageView.ScaleType.CENTER_CROP
        mThumbView.setOnClickListener { v: View? -> pauseVideo() }
        addView(mThumbView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mPlayButton = ImageView(context)
        mPlayButton.setImageResource(R.drawable.discover_play)
        mPlayButton.setOnClickListener { v: View? -> pauseVideo() }
        mPlayButton.visibility = View.GONE
        addView(mPlayButton, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        mUCropView = UCropView(getContext(), null)
        mGestureCropImageView = mUCropView.getCropImageView()
        mOverlayView = mUCropView.getOverlayView()
        mGestureCropImageView.setPadding(0, 0, 0, 0)
        mGestureCropImageView.setTargetAspectRatio(1.0f)
        mGestureCropImageView.setRotateEnabled(false)
        mGestureCropImageView.setTransformImageListener(mImageListener)
        mGestureCropImageView.setMaxScaleMultiplier(15.0f)
        mOverlayView.setPadding(0, 0, 0, 0)
        mOverlayView.setShowCropGrid(false)
        mOverlayView.setShowCropFrame(false)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            mOverlayView.setDimmedColor(Color.parseColor("#363636"))
            mOverlayView.setCropGridColor(ContextCompat.getColor(context, R.color.picture_color_black))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mOverlayView.setDimmedColor(Color.parseColor("#004561"))
            mOverlayView.setCropGridColor(Color.parseColor("#18222D"))
        } else {
            mOverlayView.setDimmedColor(Color.parseColor("#efefef"))
            mOverlayView.setCropGridColor(ContextCompat.getColor(context, R.color.picture_color_white))
        }
        mGestureCropImageView.setOnTouchListener { v, event ->
            if (event.getAction() === MotionEvent.ACTION_DOWN) {
                if (!mCropGridShowing) {
                    mOverlayView.setShowCropGrid(true)
                    mOverlayView.invalidate()
                    mCropGridShowing = true
                } else if (mShowGridRunnable != null) {
                    mHandler!!.removeCallbacks(mShowGridRunnable)
                }
            } else if (event.getAction() === MotionEvent.ACTION_UP || event.getAction() === MotionEvent.ACTION_CANCEL) {
                if (mShowGridRunnable == null) {
                    mShowGridRunnable = ShowGridRunnable(this)
                }
                mHandler!!.postDelayed(mShowGridRunnable, 800)
            }
            false
        }
        addView(mUCropView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mRatioView = ImageView(context)
        val ratiodDrawable = CombinedDrawable(InstagramUtils.createSimpleSelectorCircleDrawable(ScreenUtils.dip2px(context, 30), -0x78000000, Color.BLACK),
                context.resources.getDrawable(R.drawable.discover_telescopic).mutate())
        ratiodDrawable.setCustomSize(ScreenUtils.dip2px(context, 30), ScreenUtils.dip2px(context, 30))
        mRatioView.setImageDrawable(ratiodDrawable)
        val ratioLayoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(ScreenUtils.dip2px(context, 30), ScreenUtils.dip2px(context, 30), Gravity.BOTTOM or Gravity.LEFT)
        ratioLayoutParams.leftMargin = ScreenUtils.dip2px(context, 15)
        ratioLayoutParams.bottomMargin = ScreenUtils.dip2px(context, 12)
        addView(mRatioView, ratioLayoutParams)
        mRatioView.setOnClickListener { v: View? ->
            isAspectRatio = !isAspectRatio
            if (mPlayMode == PLAY_IMAGE_MODE) {
                resetAspectRatio()
            } else if (mPlayMode == PLAY_VIDEO_MODE) {
                changeVideoSize(mMediaPlayer, isAspectRatio)
            }
            if (mListener != null) {
                mListener!!.onRatioChange(isAspectRatio)
            }
        }
        mMultiView = ImageView(context)
        val multiDrawable = CombinedDrawable(InstagramUtils.createSimpleSelectorCircleDrawable(ScreenUtils.dip2px(context, 30), -0x78000000, Color.BLACK),
                context.resources.getDrawable(R.drawable.discover_many).mutate())
        multiDrawable.setCustomSize(ScreenUtils.dip2px(context, 30), ScreenUtils.dip2px(context, 30))
        mMultiView.setImageDrawable(multiDrawable)
        val multiLayoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(ScreenUtils.dip2px(context, 30), ScreenUtils.dip2px(context, 30), Gravity.BOTTOM or Gravity.RIGHT)
        multiLayoutParams.rightMargin = ScreenUtils.dip2px(context, 15)
        multiLayoutParams.bottomMargin = ScreenUtils.dip2px(context, 12)
        addView(mMultiView, multiLayoutParams)
        mMultiView.setOnClickListener { v: View? -> setMultiMode(!isMulti) }
        val divider = View(getContext())
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            divider.setBackgroundColor(ContextCompat.getColor(context, R.color.picture_color_black))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            divider.setBackgroundColor(Color.parseColor("#18222D"))
        } else {
            divider.setBackgroundColor(ContextCompat.getColor(context, R.color.picture_color_white))
        }
        val dividerParms: FrameLayout.LayoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ScreenUtils.dip2px(getContext(), 2), Gravity.BOTTOM)
        addView(divider, dividerParms)
    }
}