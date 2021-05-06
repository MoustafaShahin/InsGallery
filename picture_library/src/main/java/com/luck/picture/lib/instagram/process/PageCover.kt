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
class PageCover(config: PictureSelectionConfig, media: LocalMedia) : Page {
    private val mConfig: PictureSelectionConfig
    private val mMedia: LocalMedia
    private var mContainer: CoverContainer? = null
    private var mContext: Context? = null
    private var mOnSeekListener: CoverContainer.onSeekListener? = null
    fun getView(context: Context): View {
        mContainer = CoverContainer(context, mMedia)
        mContext = context
        return mContainer
    }

    fun refreshData(context: Context?) {}
    fun init(position: Int, parent: ViewGroup?) {
        if (mContainer != null) {
            mContainer.getFrame(mContext!!, mMedia)
            mContainer.setOnSeekListener(mOnSeekListener)
        }
    }

    fun getTitle(context: Context): String {
        return context.getString(R.string.cover)
    }

    fun disallowInterceptTouchRect(): Rect? {
        return null
    }

    fun setOnSeekListener(onSeekListener: CoverContainer.onSeekListener?) {
        mOnSeekListener = onSeekListener
    }

    fun cropCover(count: CountDownLatch) {
        if (mContainer != null) {
            mContainer.cropCover(count)
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
    }
}