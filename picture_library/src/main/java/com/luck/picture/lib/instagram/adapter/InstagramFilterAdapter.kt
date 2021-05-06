package com.luck.picture.lib.instagram.adapter

import android.content.Context
import android.view.View
import com.luck.picture.lib.instagram.filter.FilterType
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/6/2 16:09
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramFilterAdapter(private val mContext: Context, config: PictureSelectionConfig) : RecyclerView.Adapter<InstagramFilterAdapter.Holder?>() {
    private val mConfig: PictureSelectionConfig
    private val mFilters: List<FilterType>
    private val mBitmaps: MutableList<Bitmap>
    private var mOnItemClickListener: OnItemClickListener? = null
    private var mSelectionPosition = 0
    fun getThumbnailBitmaps(context: Context?, bitmap: Bitmap?) {
        val imageFilters: List<GPUImageFilter> = FilterType.createImageFilterList(context!!)
        GPUImage.getBitmapForMultipleFilters(bitmap, imageFilters, ResponseListener<Bitmap> { item: Bitmap -> mBitmaps.add(item) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(FilterItemView(mContext, mConfig))
        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            if (mOnItemClickListener != null) {
                mOnItemClickListener!!.onItemClick(holder.itemView, holder.getAdapterPosition(), mFilters[holder.getAdapterPosition()])
            }
            val set = AnimatorSet()
            set.playTogether(
                    ObjectAnimator.ofFloat(holder.itemView, "scaleX", 1f, 1.05f, 1f),
                    ObjectAnimator.ofFloat(holder.itemView, "scaleY", 1f, 1.05f, 1f)
            )
            set.setDuration(200)
            set.start()
        })
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        (holder.itemView as FilterItemView).refreshFilter(mFilters[position], mBitmaps[position], position, mSelectionPosition)
    }

    fun getItem(position: Int): FilterType {
        return mFilters[position]
    }

    override fun getItemCount(): Int {
        return mFilters.size
    }

    fun setSelectionPosition(selectionPosition: Int) {
        mSelectionPosition = selectionPosition
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, position: Int, filterType: FilterType?)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    init {
        mConfig = config
        mFilters = FilterType.createFilterList()
        mBitmaps = ArrayList<Bitmap>()
    }
}