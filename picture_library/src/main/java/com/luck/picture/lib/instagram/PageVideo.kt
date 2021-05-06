package com.luck.picture.lib.instagram

import android.content.*
import android.graphics.*
import android.view.*
import android.widget.FrameLayout
import com.luck.picture.lib.R

/**
 * ================================================
 * Created by JessYan on 2020/4/15 12:02
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class PageVideo(pagePhoto: PagePhoto) : Page {
    private val mPagePhoto: PagePhoto
    override fun getView(context: Context?): View {
        val frameLayout = FrameLayout(context!!)
        frameLayout.setBackgroundColor(Color.CYAN)
        return frameLayout
    }

    override fun refreshData(context: Context?) {}
    override fun init(position: Int, parent: ViewGroup?) {}
    fun getTitle(context: Context): String {
        return context.getString(R.string.video)
    }

    override fun disallowInterceptTouchRect(): Rect? {
        return mPagePhoto.disallowInterceptTouchRect()
    }

    init {
        mPagePhoto = pagePhoto
    }
}