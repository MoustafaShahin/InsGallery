package com.luck.picture.lib.instagram.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * ================================================
 * Created by JessYan on 2020/6/10 11:29
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramFrameItemDecoration(private val spacing: Int) : ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == 0) {
            outRect.left = spacing
            outRect.right = 1
        } else if (position == parent.adapter!!.itemCount - 1) {
            outRect.right = spacing
        } else {
            outRect.right = 1
        }
    }
}