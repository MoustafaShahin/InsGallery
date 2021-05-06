package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Rect
import android.view.View
import com.luck.picture.lib.entity.LocalMedia
import java.util.concurrent.CountDownLatch

/**
 * ================================================
 * Created by JessYan on 2020/4/15 12:02
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class PageTrim(config: PictureSelectionConfig, media: LocalMedia, videoView: VideoView?, isAspectRatio: Boolean, videoPauseListener: TrimContainer.VideoPauseListener) : Page {
    private val mConfig: PictureSelectionConfig
    private val mMedia: LocalMedia
    private val mVideoView: VideoView?
    private val mIsAspectRatio: Boolean
    private val mVideoPauseListener: TrimContainer.VideoPauseListener
    private var mContainer: TrimContainer? = null
    fun getView(context: Context): View {
        mContainer = TrimContainer(context, mConfig, mMedia, mVideoView, mVideoPauseListener)
        return mContainer
    }

    fun refreshData(context: Context?) {}
    fun init(position: Int, parent: ViewGroup?) {}
    fun getTitle(context: Context): String {
        return context.getString(R.string.trim)
    }

    fun disallowInterceptTouchRect(): Rect? {
        return null
    }

    fun getStartTime(): Long {
        return if (mContainer != null) {
            mContainer.getStartTime()
        } else 0
    }

    fun resetStartLine() {
        if (mContainer != null) {
            mContainer.resetStartLine()
        }
    }

    fun trimVideo(activity: InstagramMediaProcessActivity?, count: CountDownLatch?) {
        if (mContainer != null) {
//            mContainer.trimVideo(activity, count);
            mContainer.cropVideo(activity, mIsAspectRatio)
        }
    }

    fun playVideo(isPlay: Boolean, videoView: VideoView?) {
        if (mContainer != null) {
            mContainer.playVideo(isPlay, videoView)
        }
    }

    fun onResume() {
        if (mContainer != null) {
            mContainer.onResume()
        }
    }

    fun onPause() {
        if (mContainer != null) {
            mContainer.onPause()
        }
    }

    fun onDestroy() {
        if (mContainer != null) {
            mContainer.onDestroy()
            mContainer = null
        }
    }

    init {
        mConfig = config
        mMedia = media
        mVideoView = videoView
        mIsAspectRatio = isAspectRatio
        mVideoPauseListener = videoPauseListener
    }
}