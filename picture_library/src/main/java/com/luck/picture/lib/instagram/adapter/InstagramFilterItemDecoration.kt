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
class InstagramFilterItemDecoration : ItemDecoration {
    private var spacing: Int
    private var multiplier = 2

    constructor(spacing: Int) {
        this.spacing = spacing
    }

    constructor(spacing: Int, multiplier: Int) {
        this.spacing = spacing
        this.multiplier = multiplier
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == 0) {
            outRect.left = spacing * multiplier
            outRect.right = spacing
        } else if (position == parent.adapter!!.itemCount - 1) {
            outRect.right = spacing * multiplier
        } else {
            outRect.right = spacing
        }
    }
}