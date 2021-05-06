package com.luck.picture.lib.adapter

import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureMimeType
import java.util.*

/**
 * @author：luck
 * @date：2019-11-30 20:50
 * @describe：WeChat style selected after image preview
 */
class PictureWeChatPreviewGalleryAdapter(config: PictureSelectionConfig?) : RecyclerView.Adapter<PictureWeChatPreviewGalleryAdapter.ViewHolder?>() {
    private var list: MutableList<LocalMedia>? = null
    private val config: PictureSelectionConfig?
    fun setNewData(data: List<LocalMedia?>?) {
        list = data ?: ArrayList<LocalMedia>()
        notifyDataSetChanged()
    }

    fun addSingleMediaToData(media: LocalMedia) {
        if (list != null) {
            list!!.clear()
            list!!.add(media)
            notifyDataSetChanged()
        }
    }

    fun removeMediaToData(media: LocalMedia) {
        if (list != null && list!!.size > 0) {
            list!!.remove(media)
            notifyDataSetChanged()
        }
    }

    fun isDataEmpty(): Boolean {
        return list == null || list!!.size == 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView: View = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.picture_wechat_preview_gallery, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: LocalMedia? = getItem(position)
        if (item != null) {
            holder.viewBorder.visibility = if (item.isChecked()) View.VISIBLE else View.GONE
            if (config != null && PictureSelectionConfig.imageEngine != null) {
                PictureSelectionConfig.imageEngine.loadImage(holder.itemView.getContext(), item.getPath(), holder.ivImage)
            }
            holder.ivPlay.visibility = if (PictureMimeType.isHasVideo(item.getMimeType())) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null && holder.getAdapterPosition() >= 0) {
                    listener!!.onItemClick(holder.getAdapterPosition(), getItem(position), v)
                }
            })
        }
    }

    fun getItem(position: Int): LocalMedia? {
        return if (list != null && list!!.size > 0) list!![position] else null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivImage: ImageView
        var ivPlay: ImageView
        var viewBorder: View

        init {
            ivImage = itemView.findViewById(R.id.ivImage)
            ivPlay = itemView.findViewById(R.id.ivPlay)
            viewBorder = itemView.findViewById(R.id.viewBorder)
        }
    }

    private var listener: OnItemClickListener? = null
    fun setItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int, media: LocalMedia?, v: View?)
    }

    override fun getItemCount(): Int {
        return if (list != null) list!!.size else 0
    }

    init {
        this.config = config
    }
}