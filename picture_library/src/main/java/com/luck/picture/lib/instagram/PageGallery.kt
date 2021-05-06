package com.luck.picture.lib.instagram

import android.content.*
import android.graphics.*
import android.view.*
import com.luck.picture.lib.R

/**
 * ================================================
 * Created by JessYan on 2020/4/15 11:59
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class PageGallery(gallery: InstagramGallery) : Page {
    private val mGallery: InstagramGallery
    override fun getView(context: Context?): View {
        return mGallery
    }

    override fun refreshData(context: Context?) {}
    override fun init(position: Int, parent: ViewGroup?) {}
    fun getTitle(context: Context): String {
        return context.getString(R.string.gallery)
    }

    override fun disallowInterceptTouchRect(): Rect? {
        if (!mGallery.isScrollTop()) {
            val rect = Rect()
            mGallery.getPreviewView().getHitRect(rect)
            return rect
        }
        return null
    }

    init {
        mGallery = gallery
    }
}