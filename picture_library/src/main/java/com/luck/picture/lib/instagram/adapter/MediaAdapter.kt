package com.luck.picture.lib.instagram.adapter

import android.content.Context
import android.view.View
import com.luck.picture.lib.entity.LocalMedia

/**
 * ================================================
 * Created by JessYan on 2020/6/10 11:09
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class MediaAdapter(private val mContext: Context, mediaList: List<LocalMedia>) : RecyclerView.Adapter<MediaAdapter.Holder?>() {
    private var mOnItemClickListener: OnItemClickListener? = null
    private val mMediaList: List<LocalMedia>
    private var mBitmaps: List<Bitmap>? = null
    fun setBitmaps(bitmaps: List<Bitmap>?) {
        mBitmaps = bitmaps
    }

    fun getBitmaps(): List<Bitmap>? {
        return mBitmaps
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(MediaItemView(parent.getContext()))
        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            if (mOnItemClickListener != null) {
                mOnItemClickListener!!.onItemClick(holder.itemView, holder.getAdapterPosition(), mBitmaps!![holder.getAdapterPosition()])
            }
        })
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        (holder.itemView as MediaItemView).setImage(mBitmaps!![position])
    }

    override fun getItemCount(): Int {
        return mBitmaps!!.size
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, position: Int, bitmap: Bitmap?)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    init {
        mMediaList = mediaList
    }
}