package com.luck.picture.lib.instagram.process

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureMimeType
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * ================================================
 * Created by JessYan on 2020/6/1 15:27
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramMediaSingleVideoContainer(context: Context, config: PictureSelectionConfig, media: LocalMedia, isAspectRatio: Boolean) : FrameLayout(context), ProcessStateCallBack, LifecycleCallBack {
    private val mTopContainer: FrameLayout
    private var mVideoView: VideoView?
    private val isAspectRatio: Boolean
    private val mThumbView: ImageView
    private val mPlayButton: ImageView
    private var mInstagramViewPager: InstagramViewPager?
    private var mMediaPlayer: MediaPlayer? = null
    private val mConfig: PictureSelectionConfig
    private val mMedia: LocalMedia
    private var isStart = false
    private var mPlayAnimator: ObjectAnimator? = null
    private var isFrist = false
    private var isVolumeOff = false
    private val mList: MutableList<Page>
    private var mCoverPlayPosition = 0
    private var isPlay = false
    private var needPause = false
    private var needSeekCover = false
    private fun startVideo(start: Boolean) {
        if (isStart == start) {
            return
        }
        if (isFrist && start && mInstagramViewPager.getSelectedPosition() === 1) {
            return
        }
        isStart = start
        if (!isFrist) {
            isFrist = true
            mVideoView.setVisibility(View.VISIBLE)
        }
        if (mInstagramViewPager.getSelectedPosition() === 0 || mInstagramViewPager.getSelectedPosition() === 1 && !start) {
            (mList[0] as PageTrim).playVideo(start, mVideoView)
        }
        if (mPlayAnimator != null && mPlayAnimator.isRunning()) {
            mPlayAnimator.cancel()
        }
        if (!start) {
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

    private fun offVolume(view: ImageView, off: Boolean) {
        if (isVolumeOff == off) {
            return
        }
        isVolumeOff = off
        if (off) {
            view.setColorFilter(PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.picture_color_1766FF), PorterDuff.Mode.MULTIPLY))
            ToastUtils.s(getContext(), getContext().getString(R.string.video_sound_off))
        } else {
            if (mConfig.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
                view.setColorFilter(PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.picture_color_black), PorterDuff.Mode.MULTIPLY))
            } else {
                view.setColorFilter(PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY))
            }
            ToastUtils.s(getContext(), getContext().getString(R.string.video_sound_on))
        }
        if (mMediaPlayer != null) {
            setVolume(off)
        }
    }

    private fun setVolume(off: Boolean) {
        if (off) {
            mMediaPlayer.setVolume(0f, 0f)
        } else {
            mMediaPlayer.setVolume(1f, 1f)
        }
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mTopContainer.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
        mInstagramViewPager.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - width, MeasureSpec.EXACTLY))
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var viewTop = 0
        val viewLeft = 0
        mTopContainer.layout(viewLeft, viewTop, viewLeft + mTopContainer.getMeasuredWidth(), viewTop + mTopContainer.getMeasuredHeight())
        viewTop = mTopContainer.getMeasuredHeight()
        mInstagramViewPager.layout(viewLeft, viewTop, viewLeft + mInstagramViewPager.getMeasuredWidth(), viewTop + mInstagramViewPager.getMeasuredHeight())
    }

    fun onBack(activity: InstagramMediaProcessActivity) {
        activity.setResult(InstagramMediaProcessActivity.RESULT_MEDIA_PROCESS_CANCELED)
        activity.finish()
    }

    fun onCenterFeature(activity: InstagramMediaProcessActivity?, view: ImageView) {
        offVolume(view, !isVolumeOff)
    }

    override fun onProcess(activity: InstagramMediaProcessActivity?) {
        var c = 1
        if (mConfig.instagramSelectionConfig.haveCover()) {
            c++
        }
        val count = CountDownLatch(c)
        (mList[0] as PageTrim).trimVideo(activity, count)
        if (mConfig.instagramSelectionConfig.haveCover()) {
            (mList[1] as PageCover).cropCover(count)
        }
    }

    override fun onActivityResult(activity: InstagramMediaProcessActivity?, requestCode: Int, resultCode: Int, data: Intent?) {}
    override fun onStart(activity: InstagramMediaProcessActivity?) {}
    override fun onResume(activity: InstagramMediaProcessActivity?) {
        if (mInstagramViewPager.getSelectedPosition() === 0) {
            mThumbView.visibility = View.VISIBLE
            mThumbView.alpha = 1f
            mPlayButton.visibility = View.VISIBLE
            mPlayButton.alpha = 1f
        } else if (mInstagramViewPager.getSelectedPosition() === 1) {
            if (!mVideoView.isPlaying()) {
                mVideoView.start()
            }
            needPause = true
            needSeekCover = true
        }
        isStart = false
        if (mInstagramViewPager != null) {
            mInstagramViewPager.onResume()
        }
    }

    override fun onPause(activity: InstagramMediaProcessActivity?) {
        if (mInstagramViewPager.getSelectedPosition() === 1) {
            mCoverPlayPosition = mVideoView.getCurrentPosition()
        }
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback()
        }
        if (mInstagramViewPager != null) {
            mInstagramViewPager.onPause()
        }
        isPlay = false
        needPause = false
    }

    override fun onDestroy(activity: InstagramMediaProcessActivity?) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release()
            mMediaPlayer = null
        }
        if (mInstagramViewPager != null) {
            mInstagramViewPager.onDestroy()
            mInstagramViewPager = null
        }
        mVideoView = null
    }

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
        val parentHeight: Int = getMeasuredWidth()
        val instagramAspectRatio: Float = InstagramPreviewContainer.getInstagramAspectRatio(videoWidth, videoHeight)
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

    class OnCompleteListenerImpl(imageView: ImageView) : getFrameBitmapTask.OnCompleteListener {
        private val mImageViewWeakReference: WeakReference<ImageView>
        override fun onGetBitmapComplete(bitmap: Bitmap?) {
            val imageView = mImageViewWeakReference.get()
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
        }

        init {
            mImageViewWeakReference = WeakReference(imageView)
        }
    }

    init {
        mConfig = config
        mMedia = media
        this.isAspectRatio = isAspectRatio
        mTopContainer = FrameLayout(context)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            mTopContainer.setBackgroundColor(Color.parseColor("#363636"))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mTopContainer.setBackgroundColor(Color.parseColor("#004561"))
        } else {
            mTopContainer.setBackgroundColor(Color.parseColor("#efefef"))
        }
        addView(mTopContainer)
        mVideoView = VideoView(context)
        mTopContainer.addView(mVideoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        mVideoView.setVisibility(View.GONE)
        if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(media.getPath())) {
            mVideoView.setVideoURI(Uri.parse(media.getPath()))
        } else {
            mVideoView.setVideoPath(media.getPath())
        }
        mVideoView.setOnClickListener(View.OnClickListener { v: View? -> startVideo(!isStart) })
        mVideoView.setOnPreparedListener(OnPreparedListener { mp: MediaPlayer ->
            mMediaPlayer = mp
            setVolume(isVolumeOff)
            mp.setLooping(true)
            changeVideoSize(mp, isAspectRatio)
            mp.setOnInfoListener(MediaPlayer.OnInfoListener { mp1: MediaPlayer?, what: Int, extra: Int ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    // video started
                    isPlay = true
                    if (needSeekCover && mCoverPlayPosition >= 0) {
                        mVideoView.seekTo(mCoverPlayPosition)
                        mCoverPlayPosition = -1
                        needSeekCover = false
                    }
                    if (needPause) {
                        mVideoView.pause()
                        needPause = false
                    }
                    if (mThumbView.visibility == View.VISIBLE) {
                        ObjectAnimator.ofFloat(mThumbView, "alpha", 1.0f, 0f).setDuration(400).start()
                    }
                    return@setOnInfoListener true
                }
                false
            })
        })
        mThumbView = ImageView(context)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            mThumbView.setBackgroundColor(Color.parseColor("#363636"))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mThumbView.setBackgroundColor(Color.parseColor("#004561"))
        } else {
            mThumbView.setBackgroundColor(Color.parseColor("#efefef"))
        }
        mTopContainer.addView(mThumbView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mThumbView.setOnClickListener { v: View? ->
            if (mInstagramViewPager.getSelectedPosition() === 0) {
                startVideo(!isStart)
            }
        }
        mPlayButton = ImageView(context)
        mPlayButton.setImageResource(R.drawable.discover_play)
        mPlayButton.setOnClickListener { v: View? -> startVideo(!isStart) }
        mTopContainer.addView(mPlayButton, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        mList = ArrayList<Page>()
        mList.add(PageTrim(config, media, mVideoView, isAspectRatio, object : VideoPauseListener() {
            fun onChange() {
                if (!isFrist) {
                    startVideo(true)
                }
            }

            fun onVideoPause() {
                if (isPlay) {
                    startVideo(false)
                }
            }
        }))
        if (config.instagramSelectionConfig.haveCover()) {
            mList.add(PageCover(config, media))
            (mList[1] as PageCover).setOnSeekListener(object : onSeekListener() {
                fun onSeek(percent: Float) {
                    if (!isFrist) {
                        startVideo(true)
                    }
                    mVideoView.seekTo((mMedia.getDuration() * percent) as Int)
                }

                fun onSeekEnd() {
                    needPause = true
                    if (isStart && isPlay) {
                        startVideo(false)
                    }
                    mPlayButton.visibility = View.GONE
                }
            })
        }
        mInstagramViewPager = InstagramViewPager(getContext(), mList, config)
        mInstagramViewPager.displayTabLayout(config.instagramSelectionConfig.haveCover())
        mInstagramViewPager.setScrollEnable(false)
        addView(mInstagramViewPager)
        mInstagramViewPager.setOnPageChangeListener(object : OnPageChangeListener() {
            fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            fun onPageSelected(position: Int) {
                if (position == 1) {
                    if (isStart) {
                        startVideo(false)
                    }
                    mPlayButton.visibility = View.GONE
                    if (mCoverPlayPosition >= 0) {
                        mVideoView.seekTo(mCoverPlayPosition)
                        mCoverPlayPosition = -1
                    }
                } else if (position == 0) {
                    mCoverPlayPosition = mVideoView.getCurrentPosition()
                    (mList[0] as PageTrim).resetStartLine()
                    mVideoView.seekTo((mList[0] as PageTrim).getStartTime() as Int)
                    if (!isStart) {
                        mPlayButton.visibility = View.VISIBLE
                    }
                }
            }
        })
        getFrameBitmapTask(context, media, isAspectRatio, -1, OnCompleteListenerImpl(mThumbView)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}