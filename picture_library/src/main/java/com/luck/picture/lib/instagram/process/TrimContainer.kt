package com.luck.picture.lib.instagram.process

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.view.View
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * ================================================
 * Created by JessYan on 2020/6/24 17:07
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class TrimContainer(context: Context, config: PictureSelectionConfig, media: LocalMedia, videoView: VideoView?, videoPauseListener: VideoPauseListener) : FrameLayout(context) {
    private val mPadding: Int
    private val mRecyclerView: RecyclerView
    private val mConfig: PictureSelectionConfig
    private val mMedia: LocalMedia
    private val mVideoView: VideoView?
    private val mVideoTrimmerAdapter: VideoTrimmerAdapter
    private var mFrameTask: getAllFrameTask?
    private val mVideoRulerView: VideoRulerView
    private val mRangeSeekBarView: RangeSeekBarView
    private val mLeftShadow: View
    private val mRightShadow: View
    private val mIndicatorView: View
    private var mScrollX = 0
    private var mThumbsCount = 0
    private var mPauseAnim: ObjectAnimator? = null
    private var mIndicatorAnim: ObjectAnimator? = null
    private var mIndicatorPosition = 0f
    private var mInterpolator: LinearInterpolator? = null
    private var isRangeChange = true
    private var mIsPreviewStart = true
    private var mLoadingDialog: InstagramLoadingDialog? = null
    private var mTranscodeFuture: Future<Void>? = null
    private fun changeRange(videoView: VideoView?, videoPauseListener: VideoPauseListener, isPreviewStart: Boolean) {
        videoPauseListener.onChange()
        mIsPreviewStart = isPreviewStart
        if (isPreviewStart) {
            videoView.seekTo(getStartTime().toInt())
        } else {
            videoView.seekTo(getEndTime().toInt())
        }
        if (videoView.isPlaying()) {
            videoPauseListener.onVideoPause()
        }
        isRangeChange = true
        mIndicatorPosition = 0f
    }

    fun playVideo(isPlay: Boolean, videoView: VideoView?) {
        if (mPauseAnim != null && mPauseAnim.isRunning()) {
            mPauseAnim.cancel()
        }
        if (isPlay) {
            if (!mIsPreviewStart) {
                videoView.seekTo(getStartTime().toInt())
            }
            mIndicatorView.visibility = View.VISIBLE
            mPauseAnim = ObjectAnimator.ofFloat(mIndicatorView, "alpha", 0f, 1.0f).setDuration(200)
            if (isRangeChange) {
                isRangeChange = false
                val startTime: Long
                startTime = if (mIndicatorPosition > 0) {
                    Math.round((mIndicatorPosition - ScreenUtils.dip2px(getContext(), 20)) / mVideoRulerView.getInterval() * 1000)
                } else {
                    getStartTime()
                }
                mIndicatorAnim = ObjectAnimator.ofFloat(mIndicatorView, "translationX", if (mIndicatorPosition > 0) mIndicatorPosition else mRangeSeekBarView.getStartLine() + ScreenUtils.dip2px(getContext(), 10),
                        mRangeSeekBarView.getEndLine() + ScreenUtils.dip2px(getContext(), 10)).setDuration(getEndTime() - startTime)
                mIndicatorAnim.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator -> mIndicatorPosition = animation.getAnimatedValue() })
                mIndicatorAnim.addListener(object : AnimatorListenerImpl() {
                    fun onAnimationRepeat(animation: Animator?) {
                        if (videoView != null) {
                            videoView.seekTo(startTime.toInt())
                        }
                    }
                })
                mIndicatorAnim.setRepeatMode(ValueAnimator.RESTART)
                mIndicatorAnim.setRepeatCount(ValueAnimator.INFINITE)
                if (mInterpolator == null) {
                    mInterpolator = LinearInterpolator()
                }
                mIndicatorAnim.setInterpolator(mInterpolator)
                mIndicatorAnim.start()
            } else {
                if (mIndicatorAnim != null && mIndicatorAnim.isPaused()) {
                    mIndicatorAnim.resume()
                }
            }
        } else {
            mPauseAnim = ObjectAnimator.ofFloat(mIndicatorView, "alpha", 1.0f, 0f).setDuration(200)
            mPauseAnim.addListener(object : AnimatorListenerImpl() {
                fun onAnimationEnd(animation: Animator?) {
                    mIndicatorView.visibility = View.GONE
                }
            })
            if (mIndicatorAnim != null && mIndicatorAnim.isRunning()) {
                mIndicatorAnim.pause()
            }
        }
        mPauseAnim.start()
    }

    fun getStartTime(): Long {
        return if (mThumbsCount < 8) {
            Math.round(mRangeSeekBarView.getNormalizedMinValue() * mMedia.getDuration())
        } else {
            val min: Double = mRangeSeekBarView.getNormalizedMinValue() * mRangeSeekBarView.getMeasuredWidth() + mScrollX
            Math.round((if (min > 0) min + ScreenUtils.dip2px(getContext(), 1) else min) / mVideoRulerView.getInterval() * 1000)
        }
    }

    fun getEndTime(): Long {
        return if (mThumbsCount < 8) {
            Math.round(mRangeSeekBarView.getNormalizedMaxValue() * mMedia.getDuration())
        } else {
            val max: Double = mRangeSeekBarView.getNormalizedMaxValue() * mVideoRulerView.getRangWidth() + mScrollX
            Math.round((max - ScreenUtils.dip2px(getContext(), 1)) / mVideoRulerView.getInterval() * 1000)
        }
    }

    fun cropVideo(activity: InstagramMediaProcessActivity?, isAspectRatio: Boolean) {
        showLoadingView(true)
        val startTime = getStartTime()
        val endTime = getEndTime()
        val startTimeUS = getStartTime() * 1000
        val endTimeUS = getEndTime() * 1000
        val transcodeOutputFile: File
        transcodeOutputFile = try {
            val outputDir: File = File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "TrimVideos")
            outputDir.mkdir()
            File.createTempFile(DateUtils.getCreateFileName("trim_"), ".mp4", outputDir)
        } catch (e: IOException) {
            ToastUtils.s(getContext(), "Failed to create temporary file.")
            return
        }
        var resizer: Resizer? = PassThroughResizer()
        if (mConfig.instagramSelectionConfig.isCropVideo()) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            val uri: Uri
            uri = if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mMedia.getPath())) {
                Uri.parse(mMedia.getPath())
            } else {
                Uri.fromFile(File(mMedia.getPath()))
            }
            mediaMetadataRetriever.setDataSource(getContext(), uri)
            val videoWidth: Int = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            val videoHeight: Int = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            val instagramAspectRatio: Float = InstagramPreviewContainer.getInstagramAspectRatio(videoWidth, videoHeight)
            mediaMetadataRetriever.release()
            if (isAspectRatio && instagramAspectRatio > 0) {
                resizer = AspectRatioResizer(instagramAspectRatio)
            } else if (!isAspectRatio) {
                resizer = AspectRatioResizer(1f)
            }
        }
        val videoStrategy: TrackStrategy = DefaultVideoStrategy.Builder()
                .addResizer(resizer)
                .addResizer(FractionResizer(1f))
                .build()
        val sink: DataSink = DefaultDataSink(transcodeOutputFile.absolutePath)
        val builder: TranscoderOptions.Builder = Transcoder.into(sink)
        if (PictureMimeType.isContent(mMedia.getPath())) {
            builder.addDataSource(ClipDataSource(UriDataSource(getContext(), Uri.parse(mMedia.getPath())), startTimeUS, endTimeUS))
        } else {
            builder.addDataSource(ClipDataSource(FilePathDataSource(mMedia.getPath()), startTimeUS, endTimeUS))
        }
        mTranscodeFuture = builder.setListener(TranscoderListenerImpl(this, activity, startTime, endTime, transcodeOutputFile))
                .setVideoTrackStrategy(videoStrategy)
                .transcode()
    }

    private fun showLoadingView(isShow: Boolean) {
        if ((getContext() as Activity).isFinishing()) {
            return
        }
        if (isShow) {
            if (mLoadingDialog == null) {
                mLoadingDialog = InstagramLoadingDialog(getContext())
                mLoadingDialog.setOnCancelListener { dialog ->
                    if (mTranscodeFuture != null) {
                        mTranscodeFuture!!.cancel(true)
                    }
                }
            }
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss()
            }
            mLoadingDialog.updateProgress(0)
            mLoadingDialog.show()
        } else {
            if (mLoadingDialog != null
                    && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss()
            }
        }
    }

    fun trimVideo(activity: InstagramMediaProcessActivity, count: CountDownLatch) {
        activity.showLoadingView(true)
        val startTime = getStartTime()
        val endTime = getEndTime()
        PictureThreadUtils.executeByIo(object : SimpleTask<File?>() {
            fun doInBackground(): File? {
                val inputUri: Uri
                inputUri = if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mMedia.getPath())) {
                    Uri.parse(mMedia.getPath())
                } else {
                    Uri.fromFile(File(mMedia.getPath()))
                }
                try {
                    val outputDir: File = File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "TrimVideos")
                    outputDir.mkdir()
                    val outputFile = File.createTempFile(DateUtils.getCreateFileName("trim_"), ".mp4", outputDir)
                    val parcelFileDescriptor: ParcelFileDescriptor = getContext().getContentResolver().openFileDescriptor(inputUri, "r")
                    if (parcelFileDescriptor != null) {
                        val fileDescriptor: FileDescriptor = parcelFileDescriptor.getFileDescriptor()
                        //                        boolean succeeded = VideoClipUtils.trimUsingMp4Parser(fileDescriptor, outputFile.getAbsolutePath(), startTime, endTime);
//                        if (!succeeded) {
                        val succeeded: Boolean = VideoClipUtils.genVideoUsingMuxer(fileDescriptor, outputFile.absolutePath, startTime, endTime, true, true)
                        //                        }
                        if (succeeded) {
                            count.countDown()
                            try {
                                count.await(1500, TimeUnit.MILLISECONDS)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                            return outputFile
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }

            fun onSuccess(result: File?) {
                if (result != null) {
                    mMedia.setDuration(endTime - startTime)
                    mMedia.setPath(result.absolutePath)
                    mMedia.setAndroidQToPath(if (SdkVersionUtils.checkedAndroid_Q()) result.absolutePath else mMedia.getAndroidQToPath())
                    val list: MutableList<LocalMedia> = ArrayList<LocalMedia>()
                    list.add(mMedia)
                    activity.showLoadingView(false)
                    activity.setResult(Activity.RESULT_OK, Intent().putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST, list as ArrayList<out Parcelable>))
                    activity.finish()
                } else {
                    ToastUtils.s(getContext(), getContext().getString(R.string.video_clip_failed))
                }
            }
        })
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mRecyclerView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 90), MeasureSpec.EXACTLY))
        mRangeSeekBarView.measure(MeasureSpec.makeMeasureSpec(width - ScreenUtils.dip2px(getContext(), 20), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 90), MeasureSpec.EXACTLY))
        mLeftShadow.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 10), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 90), MeasureSpec.EXACTLY))
        mRightShadow.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 10), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 90), MeasureSpec.EXACTLY))
        mVideoRulerView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - ScreenUtils.dip2px(getContext(), 90), MeasureSpec.EXACTLY))
        if (mIndicatorView.visibility == View.VISIBLE) {
            mIndicatorView.measure(MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 2), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ScreenUtils.dip2px(getContext(), 90), MeasureSpec.EXACTLY))
        }
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop = 0
        var viewLeft = 0
        mRecyclerView.layout(viewLeft, viewTop, viewLeft + mRecyclerView.getMeasuredWidth(), viewTop + mRecyclerView.getMeasuredHeight())
        mLeftShadow.layout(viewLeft, viewTop, viewLeft + mLeftShadow.measuredWidth, viewTop + mLeftShadow.measuredHeight)
        viewLeft = getMeasuredWidth() - ScreenUtils.dip2px(getContext(), 10)
        mRightShadow.layout(viewLeft, viewTop, viewLeft + mRightShadow.measuredWidth, viewTop + mRightShadow.measuredHeight)
        viewLeft = ScreenUtils.dip2px(getContext(), 20) - ScreenUtils.dip2px(getContext(), 10)
        mRangeSeekBarView.layout(viewLeft, viewTop, viewLeft + mRangeSeekBarView.getMeasuredWidth(), viewTop + mRangeSeekBarView.getMeasuredHeight())
        viewLeft = 0
        viewTop += mRecyclerView.getMeasuredHeight()
        mVideoRulerView.layout(viewLeft, viewTop, viewLeft + mVideoRulerView.getMeasuredWidth(), viewTop + mVideoRulerView.getMeasuredHeight())
        viewTop = 0
        if (mIndicatorView.visibility == View.VISIBLE) {
            mIndicatorView.layout(viewLeft, viewTop, viewLeft + mIndicatorView.measuredWidth, viewTop + mIndicatorView.measuredHeight)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val rect = Rect()
        mVideoRulerView.getHitRect(rect)
        return if (rect.contains(ev.getX() as Int, ev.getY() as Int)) {
            true
        } else super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mRecyclerView.onTouchEvent(event)
        return true
    }

    fun onResume() {
        if (mVideoView != null) {
            mVideoView.seekTo(getStartTime().toInt())
        }
    }

    fun onPause() {
        if (mIndicatorAnim != null && mIndicatorAnim.isRunning()) {
            mIndicatorAnim.cancel()
        }
        resetStartLine()
    }

    fun resetStartLine() {
        isRangeChange = true
        mIndicatorPosition = 0f
        mIndicatorView.visibility = View.GONE
    }

    fun onDestroy() {
        if (mFrameTask != null) {
            mFrameTask.setStop(true)
            mFrameTask.cancel(true)
            mFrameTask = null
        }
        if (mTranscodeFuture != null) {
            mTranscodeFuture!!.cancel(true)
            mTranscodeFuture = null
        }
    }

    class OnSingleBitmapListenerImpl(container: TrimContainer) : getAllFrameTask.OnSingleBitmapListener {
        private val mContainerWeakReference: WeakReference<TrimContainer>
        override fun onSingleBitmapComplete(bitmap: Bitmap?) {
            val container = mContainerWeakReference.get()
            container?.post(RunnableImpl(container.mVideoTrimmerAdapter, bitmap))
        }

        class RunnableImpl(adapter: VideoTrimmerAdapter, bitmap: Bitmap?) : Runnable {
            private val mAdapterWeakReference: WeakReference<VideoTrimmerAdapter>
            private val mBitmap: Bitmap?
            override fun run() {
                val adapter: VideoTrimmerAdapter? = mAdapterWeakReference.get()
                if (adapter != null) {
                    adapter.addBitmaps(mBitmap)
                }
            }

            init {
                mAdapterWeakReference = WeakReference<VideoTrimmerAdapter>(adapter)
                mBitmap = bitmap
            }
        }

        init {
            mContainerWeakReference = WeakReference(container)
        }
    }

    interface VideoPauseListener {
        fun onChange()
        fun onVideoPause()
    }

    private class TranscoderListenerImpl(container: TrimContainer, activity: InstagramMediaProcessActivity?, startTime: Long, endTime: Long, transcodeOutputFile: File) : TranscoderListener {
        private val mContainerWeakReference: WeakReference<TrimContainer>
        private val mActivityWeakReference: WeakReference<InstagramMediaProcessActivity?>
        private val mStartTime: Long
        private val mEndTime: Long
        private val mTranscodeOutputFile: File
        override fun onTranscodeProgress(progress: Double) {
            val trimContainer = mContainerWeakReference.get() ?: return
            if (trimContainer.mLoadingDialog != null
                    && trimContainer.mLoadingDialog.isShowing()) {
                trimContainer.mLoadingDialog.updateProgress(progress)
            }
        }

        override fun onTranscodeCompleted(successCode: Int) {
            val trimContainer = mContainerWeakReference.get()
            val activity: InstagramMediaProcessActivity? = mActivityWeakReference.get()
            if (trimContainer == null || activity == null) {
                return
            }
            if (successCode == Transcoder.SUCCESS_TRANSCODED) {
                trimContainer.mMedia.setDuration(mEndTime - mStartTime)
                trimContainer.mMedia.setPath(mTranscodeOutputFile.absolutePath)
                trimContainer.mMedia.setAndroidQToPath(if (SdkVersionUtils.checkedAndroid_Q()) mTranscodeOutputFile.absolutePath else trimContainer.mMedia.getAndroidQToPath())
                val list: MutableList<LocalMedia> = ArrayList<LocalMedia>()
                list.add(trimContainer.mMedia)
                activity.setResult(Activity.RESULT_OK, Intent().putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST, list as ArrayList<out Parcelable>))
                activity.finish()
            } else if (successCode == Transcoder.SUCCESS_NOT_NEEDED) {
            }
            trimContainer.showLoadingView(false)
        }

        override fun onTranscodeCanceled() {
            val trimContainer = mContainerWeakReference.get() ?: return
            trimContainer.showLoadingView(false)
        }

        override fun onTranscodeFailed(exception: Throwable) {
            val trimContainer = mContainerWeakReference.get() ?: return
            exception.printStackTrace()
            ToastUtils.s(trimContainer.getContext(), trimContainer.getContext().getString(R.string.video_clip_failed))
            trimContainer.showLoadingView(false)
        }

        init {
            mContainerWeakReference = WeakReference(container)
            mActivityWeakReference = WeakReference<InstagramMediaProcessActivity?>(activity)
            mStartTime = startTime
            mEndTime = endTime
            mTranscodeOutputFile = transcodeOutputFile
        }
    }

    init {
        mPadding = ScreenUtils.dip2px(context, 20)
        mRecyclerView = RecyclerView(context)
        mConfig = config
        mMedia = media
        mVideoView = videoView
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
            mRecyclerView.setBackgroundColor(Color.parseColor("#333333"))
        } else {
            mRecyclerView.setBackgroundColor(Color.BLACK)
        }
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.addItemDecoration(InstagramFrameItemDecoration(mPadding))
        mRecyclerView.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false))
        mVideoTrimmerAdapter = VideoTrimmerAdapter()
        mRecyclerView.setAdapter(mVideoTrimmerAdapter)
        addView(mRecyclerView)
        ObjectAnimator.ofFloat(mRecyclerView, "translationX", ScreenUtils.getScreenWidth(context), 0).setDuration(300).start()
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                mScrollX += dx
                mVideoRulerView.scrollBy(dx, 0)
                if (media.getDuration() > 60000 && dx != 0) {
                    changeRange(videoView, videoPauseListener, true)
                }
            }
        })
        mVideoRulerView = VideoRulerView(context, media.getDuration())
        addView(mVideoRulerView)
        mThumbsCount = if (media.getDuration() > 60000) {
            Math.round(media.getDuration() / 7500f).toInt()
        } else if (media.getDuration() < 15000) {
            Math.round(media.getDuration() / 1875f).toInt()
        } else {
            8
        }
        mRangeSeekBarView = RangeSeekBarView(context, 0, media.getDuration(), mThumbsCount)
        mRangeSeekBarView.setSelectedMinValue(0)
        mRangeSeekBarView.setSelectedMaxValue(media.getDuration())
        mRangeSeekBarView.setStartEndTime(0, media.getDuration())
        mRangeSeekBarView.setMinShootTime(3000L)
        mRangeSeekBarView.setNotifyWhileDragging(true)
        mRangeSeekBarView.setOnRangeSeekBarChangeListener { bar, minValue, maxValue, action, isMin, pressedThumb -> changeRange(videoView, videoPauseListener, pressedThumb === RangeSeekBarView.Thumb.MIN) }
        addView(mRangeSeekBarView)
        mLeftShadow = View(context)
        mLeftShadow.setBackgroundColor(-0x41000000)
        addView(mLeftShadow)
        mRightShadow = View(context)
        mRightShadow.setBackgroundColor(-0x41000000)
        addView(mRightShadow)
        mIndicatorView = View(context)
        mIndicatorView.setBackgroundColor(Color.WHITE)
        addView(mIndicatorView)
        mIndicatorView.visibility = View.GONE
        mVideoTrimmerAdapter.setItemCount(mThumbsCount)
        mFrameTask = getAllFrameTask(context, media, mThumbsCount, 0, (media.getDuration() as Int).toLong(), OnSingleBitmapListenerImpl(this))
        mFrameTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}