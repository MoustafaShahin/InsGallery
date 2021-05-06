package com.luck.picture.lib.instagram.adapter

import android.graphics.Bitmap
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.lib.R
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/6/24 17:27
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class VideoTrimmerAdapter : RecyclerView.Adapter<Any?>() {
    private val mBitmaps: MutableList<Bitmap> = ArrayList()
    private var mItemCount = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return Holder(FrameItemView(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < mBitmaps.size) {
            (holder.itemView as FrameItemView).setImage(mBitmaps[position])
        } else {
            (holder.itemView as FrameItemView).setImageResource(R.drawable.picture_image_placeholder)
        }
    }

    fun setItemCount(itemCount: Int) {
        mItemCount = itemCount
    }

    override fun getItemCount(): Int {
        return if (mItemCount > 0) mItemCount else mBitmaps.size
    }

    fun addBitmaps(bitmap: Bitmap) {
        mBitmaps.add(bitmap)
        notifyDataSetChanged()
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
}