package com.luck.picture.lib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.lib.R
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.entity.LocalMediaFolder
import com.luck.picture.lib.listener.OnAlbumItemClickListener
import java.util.*

/**
 * @author：luck
 * @date：2016-12-11 17:02
 * @describe：PictureAlbumDirectoryAdapter
 */
class PictureAlbumDirectoryAdapter(config: PictureSelectionConfig) : RecyclerView.Adapter<PictureAlbumDirectoryAdapter.ViewHolder?>() {
    private var folders: List<LocalMediaFolder>? = ArrayList<LocalMediaFolder>()
    private var chooseMode: Int
    private val config: PictureSelectionConfig
    fun bindFolderData(folders: List<LocalMediaFolder?>?) {
        this.folders = (folders ?: ArrayList<LocalMediaFolder>()) as List<LocalMediaFolder>?
        notifyDataSetChanged()
    }

    fun setChooseMode(chooseMode: Int) {
        this.chooseMode = chooseMode
    }

    val folderData: List<Any>
        get() = if (folders == null) ArrayList() else folders!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView: View = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.picture_album_folder_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder: LocalMediaFolder = folders!![position]
        val name: String = folder.getName().toString()
        val imageNum: Int = folder.getImageNum()
        val imagePath: String = folder.getFirstImagePath().toString()
        val isChecked: Boolean = folder.isChecked()
        val checkedNum: Int = folder.getCheckedNum()
        holder.tvSign.setVisibility(if (checkedNum > 0) View.VISIBLE else View.INVISIBLE)
        holder.itemView.setSelected(isChecked)
        if (config.style != null && config.style.pictureAlbumStyle !== 0) {
            holder.itemView.setBackgroundResource(config.style.pictureAlbumStyle)
        }
        if (chooseMode == PictureMimeType.ofAudio()) {
            holder.ivFirstImage.setImageResource(R.drawable.picture_audio_placeholder)
        } else {
            if (PictureSelectionConfig.imageEngine != null) {
                PictureSelectionConfig.imageEngine.loadFolderImage(holder.itemView.getContext(),
                        imagePath, holder.ivFirstImage)
            }
        }
        val context: Context = holder.itemView.getContext()
        val firstTitle = if (folder.getOfAllType() !== -1) if (folder.getOfAllType() === PictureMimeType.ofAudio()) context.getString(R.string.picture_all_audio) else context.getString(R.string.picture_camera_roll) else name
        holder.tvFolderName.setText(context.getString(R.string.picture_camera_roll_num, firstTitle, imageNum))
        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            if (onAlbumItemClickListener != null) {
                val size = folders!!.size
                for (i in 0 until size) {
                    val mediaFolder: LocalMediaFolder = folders!![i]
                    mediaFolder.setChecked(false)
                }
                folder.setChecked(true)
                notifyDataSetChanged()
                onAlbumItemClickListener!!.onItemClick(position, folder.isCameraFolder(), folder.getBucketId(), folder.getName(), folder.getData())
            }
        })
    }

    override fun getItemCount(): Int {
        return folders!!.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivFirstImage: ImageView
        var tvFolderName: TextView
        var tvSign: TextView

        init {
            ivFirstImage = itemView.findViewById(R.id.first_image)
            tvFolderName = itemView.findViewById<TextView>(R.id.tv_folder_name)
            tvSign = itemView.findViewById<TextView>(R.id.tv_sign)
            if (config.style != null && config.style.pictureFolderCheckedDotStyle !== 0) {
                tvSign.setBackgroundResource(config.style.pictureFolderCheckedDotStyle)
            }
        }
    }

    private var onAlbumItemClickListener: OnAlbumItemClickListener? = null
    fun setOnAlbumItemClickListener(listener: OnAlbumItemClickListener?) {
        onAlbumItemClickListener = listener
    }

    init {
        this.config = config
        chooseMode = config.chooseMode
    }
}