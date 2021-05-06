package com.luck.picture.lib.adapter

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.lib.R
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.instagram.InstagramImageGridAdapter
import com.luck.picture.lib.tools.DateUtils
import com.luck.picture.lib.tools.MediaUtils
import com.luck.picture.lib.tools.StringUtils
import com.luck.picture.lib.tools.ToastUtils
import java.io.File
import java.util.*

/**
 * @author：luck
 * @date：2016-12-30 12:02
 * @describe：PictureImageGridAdapter
 */
class PictureImageGridAdapter(private val context: Context, config: PictureSelectionConfig) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    var isShowCamera: Boolean
    private lateinit var imageSelectChangedListener: InstagramImageGridAdapter.OnPhotoSelectChangedListener
    private var data: MutableList<LocalMedia>? = ArrayList<LocalMedia>()
    private var selectData: MutableList<LocalMedia?>? = ArrayList<LocalMedia?>()
    private val config: PictureSelectionConfig

    /**
     * 全量刷新
     *
     * @param data
     */
    fun bindData(data: List<LocalMedia?>?) {
        this.data = (data ?: ArrayList<LocalMedia>()) as MutableList<LocalMedia>?
        this.notifyDataSetChanged()
    }

    fun bindSelectData(images: List<LocalMedia?>) {
        // 这里重新构构造一个新集合，不然会产生已选集合一变，结果集合也会添加的问题
        val selection: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
        val size = images.size
        for (i in 0 until size) {
            val media: LocalMedia? = images[i]
            selection.add(media)
        }
        selectData = selection
        if (!config.isSingleDirectReturn) {
            subSelectPosition()
            if (imageSelectChangedListener != null) {
                imageSelectChangedListener.onChange(selectData)
            }
        }
    }

    val selectedData: MutableList<LocalMedia?>
        get() = if (selectData == null) ArrayList() else selectData!!
    val selectedSize: Int
        get() = if (selectData == null) 0 else selectData!!.size

    fun getData(): List<LocalMedia> {
        return if (data == null) ArrayList<LocalMedia>() else data!!
    }

    val isDataEmpty: Boolean
        get() = data == null || data!!.size == 0

    fun clear() {
        if (size > 0) {
            data!!.clear()
        }
    }

    val size: Int
        get() = if (data == null) 0 else data!!.size

    fun getItem(position: Int): LocalMedia? {
        return if (size > 0) data!![position] else null
    }

    override fun getItemViewType(position: Int): Int {
        return if (isShowCamera && position == 0) {
            PictureConfig.TYPE_CAMERA
        } else {
            PictureConfig.TYPE_PICTURE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == PictureConfig.TYPE_CAMERA) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.picture_item_camera, parent, false)
            CameraViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(context).inflate(R.layout.picture_image_grid_item, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == PictureConfig.TYPE_CAMERA) {
            val headerHolder = holder as CameraViewHolder
            headerHolder.headerView.setOnClickListener { v: View? ->
                if (imageSelectChangedListener != null) {
                    imageSelectChangedListener.onTakePhoto()
                }
            }
        } else {
            val contentHolder = holder as ViewHolder
            val image: LocalMedia = data!![if (isShowCamera) position - 1 else position]
            image.position = contentHolder.getAdapterPosition()
            val path: String = image.getPath().toString()
            val mimeType: String = image.getMimeType()
            if (config.checkNumMode) {
                notifyCheckChanged(contentHolder, image)
            }
            if (config.isSingleDirectReturn) {
                contentHolder.tvCheck.setVisibility(View.GONE)
                contentHolder.btnCheck.visibility = View.GONE
            } else {
                selectImage(contentHolder, isSelected(image))
                contentHolder.tvCheck.setVisibility(View.VISIBLE)
                contentHolder.btnCheck.visibility = View.VISIBLE
                // 启用了蒙层效果
                if (config.isMaxSelectEnabledMask) {
                    dispatchHandleMask(contentHolder, image)
                }
            }
            contentHolder.tvIsGif.setVisibility(if (PictureMimeType.isGif(mimeType)) View.VISIBLE else View.GONE)
            if (PictureMimeType.isHasImage(image.getMimeType())) {
                if (image.loadLongImageStatus === PictureConfig.NORMAL) {
                    image.isLongImage = MediaUtils.isLongImg(image)
                    image.loadLongImageStatus = PictureConfig.LOADED
                }
                contentHolder.tvLongChart.setVisibility(if (image.isLongImage) View.VISIBLE else View.GONE)
            } else {
                image.loadLongImageStatus = PictureConfig.NORMAL
                contentHolder.tvLongChart.setVisibility(View.GONE)
            }
            val isHasVideo: Boolean = PictureMimeType.isHasVideo(mimeType)
            if (isHasVideo || PictureMimeType.isHasAudio(mimeType)) {
                contentHolder.tvDuration.setVisibility(View.VISIBLE)
                contentHolder.tvDuration.setText(DateUtils.formatDurationTime(image.getDuration()))
                contentHolder.tvDuration.setCompoundDrawablesRelativeWithIntrinsicBounds(if (isHasVideo) R.drawable.picture_icon_video else R.drawable.picture_icon_audio,
                        0, 0, 0)
            } else {
                contentHolder.tvDuration.setVisibility(View.GONE)
            }
            if (config.chooseMode == PictureMimeType.ofAudio()) {
                contentHolder.ivPicture.setImageResource(R.drawable.picture_audio_placeholder)
            } else {
                if (PictureSelectionConfig.imageEngine != null) {
                    PictureSelectionConfig.imageEngine.loadGridImage(context, path, contentHolder.ivPicture)
                }
            }
            if (config.enablePreview || config.enPreviewVideo || config.enablePreviewAudio) {
                contentHolder.btnCheck.setOnClickListener { v: View? ->
                    if (config.isMaxSelectEnabledMask) {
                        if (!contentHolder.tvCheck.isSelected() && selectedSize >= config.maxSelectNum) {
                            val msg: String = StringUtils.getMsg(context, if (config.chooseMode == PictureMimeType.ofAll()) null else image.getMimeType(), config.maxSelectNum)
                            showPromptDialog(msg)
                            return@setOnClickListener
                        }
                    }
                    // If the original path does not exist or the path does exist but the file does not exist
                    val newPath: String = image.getRealPath().toString()
                    if (!TextUtils.isEmpty(newPath) && !File(newPath).exists()) {
                        ToastUtils.s(context, PictureMimeType.s(context, mimeType))
                        return@setOnClickListener
                    }
                    // The width and height of the image are reversed if there is rotation information
                    MediaUtils.setOrientationAsynchronous(context, image, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH, null)
                    changeCheckboxState(contentHolder, image)
                }
            }
            contentHolder.contentView.setOnClickListener { v: View? ->
                if (config.isMaxSelectEnabledMask) {
                    if (image.isMaxSelectEnabledMask()) {
                        return@setOnClickListener
                    }
                }
                // If the original path does not exist or the path does exist but the file does not exist
                val newPath: String = image.getRealPath().toString()
                if (!TextUtils.isEmpty(newPath) && !File(newPath).exists()) {
                    ToastUtils.s(context, PictureMimeType.s(context, mimeType))
                    return@setOnClickListener
                }
                val index = if (isShowCamera) position - 1 else position
                if (index == -1) {
                    return@setOnClickListener
                }
                // The width and height of the image are reversed if there is rotation information
                MediaUtils.setOrientationAsynchronous(context, image, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH, null)
                val eqResult = PictureMimeType.isHasImage(mimeType) && config.enablePreview || PictureMimeType.isHasVideo(mimeType) && (config.enPreviewVideo
                        || config.selectionMode == PictureConfig.SINGLE) || PictureMimeType.isHasAudio(mimeType) && (config.enablePreviewAudio
                        || config.selectionMode == PictureConfig.SINGLE)
                if (eqResult) {
                    if (PictureMimeType.isHasVideo(image.getMimeType())) {
                        if (config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                            // The video is less than the minimum specified length
                            showPromptDialog(context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                            return@setOnClickListener
                        }
                        if (config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                            // The length of the video exceeds the specified length
                            showPromptDialog(context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                            return@setOnClickListener
                        }
                    }
                    imageSelectChangedListener.onPictureClick(image, index)
                } else {
                    changeCheckboxState(contentHolder, image)
                }
            }
        }
    }

    /**
     * Handle mask effects
     *
     * @param contentHolder
     * @param item
     */
    private fun dispatchHandleMask(contentHolder: ViewHolder, item: LocalMedia) {
        if (config.isWithVideoImage && config.maxVideoSelectNum > 0) {
            if (selectedSize >= config.maxSelectNum) {
                val isSelected: Boolean = contentHolder.tvCheck.isSelected()
                contentHolder.ivPicture.setColorFilter(ContextCompat.getColor(context, if (isSelected) R.color.picture_color_80 else R.color.picture_color_half_white), PorterDuff.Mode.SRC_ATOP)
                item.setMaxSelectEnabledMask(!isSelected)
            } else {
                item.setMaxSelectEnabledMask(false)
            }
        } else {
            val media: LocalMedia? = if (selectData!!.size > 0) selectData!![0] else null
            if (media != null) {
                val isSelected: Boolean = contentHolder.tvCheck.isSelected()
                if (config.chooseMode == PictureMimeType.ofAll()) {
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                        // All videos are not optional
                        if (!isSelected && !PictureMimeType.isHasImage(item.getMimeType())) {
                            contentHolder.ivPicture.setColorFilter(ContextCompat.getColor(context, if (PictureMimeType.isHasVideo(item.getMimeType())) R.color.picture_color_half_white else R.color.picture_color_20), PorterDuff.Mode.SRC_ATOP)
                        }
                        item.setMaxSelectEnabledMask(PictureMimeType.isHasVideo(item.getMimeType()))
                    } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                        // All images are not optional
                        if (!isSelected && !PictureMimeType.isHasVideo(item.getMimeType())) {
                            contentHolder.ivPicture.setColorFilter(ContextCompat.getColor(context, if (PictureMimeType.isHasImage(item.getMimeType())) R.color.picture_color_half_white else R.color.picture_color_20), PorterDuff.Mode.SRC_ATOP)
                        }
                        item.setMaxSelectEnabledMask(PictureMimeType.isHasImage(item.getMimeType()))
                    }
                } else {
                    if (config.chooseMode == PictureMimeType.ofVideo() && config.maxVideoSelectNum > 0) {
                        if (!isSelected && selectedSize == config.maxVideoSelectNum) {
                            contentHolder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.picture_color_half_white), PorterDuff.Mode.SRC_ATOP)
                        }
                        item.setMaxSelectEnabledMask(!isSelected && selectedSize == config.maxVideoSelectNum)
                    } else {
                        if (!isSelected && selectedSize == config.maxSelectNum) {
                            contentHolder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.picture_color_half_white), PorterDuff.Mode.SRC_ATOP)
                        }
                        item.setMaxSelectEnabledMask(!isSelected && selectedSize == config.maxSelectNum)
                    }
                }
            }
        }
    }

    val itemCount: Int
        get() = if (isShowCamera) data!!.size + 1 else data!!.size

    inner class CameraViewHolder(var headerView: View) : RecyclerView.ViewHolder(headerView) {
        var tvCamera: TextView

        init {
            tvCamera = headerView.findViewById<TextView>(R.id.tvCamera)
            val title = if (config.chooseMode == PictureMimeType.ofAudio()) context.getString(R.string.picture_tape) else context.getString(R.string.picture_take_picture)
            tvCamera.setText(title)
        }
    }

    inner class ViewHolder(var contentView: View) : RecyclerView.ViewHolder(contentView) {
        var ivPicture: ImageView
        var tvCheck: TextView
        var tvDuration: TextView
        var tvIsGif: TextView
        var tvLongChart: TextView
        var btnCheck: View

        init {
            ivPicture = contentView.findViewById(R.id.ivPicture)
            tvCheck = contentView.findViewById<TextView>(R.id.tvCheck)
            btnCheck = contentView.findViewById(R.id.btnCheck)
            tvDuration = contentView.findViewById<TextView>(R.id.tv_duration)
            tvIsGif = contentView.findViewById<TextView>(R.id.tv_isGif)
            tvLongChart = contentView.findViewById<TextView>(R.id.tv_long_chart)
            if (config.style != null) {
                if (config.style.pictureCheckedStyle !== 0) {
                    tvCheck.setBackgroundResource(config.style.pictureCheckedStyle)
                }
            }
        }
    }

    fun isSelected(image: LocalMedia): Boolean {
        val size = selectData!!.size
        for (i in 0 until size) {
            val media: LocalMedia? = selectData!![i]
            if (media == null || TextUtils.isEmpty(media.getPath())) {
                continue
            }
            if (media.getPath()
                            .equals(image.getPath())
                    || media.getId() === image.getId()) {
                return true
            }
        }
        return false
    }

    /**
     * Update button status
     */
    private fun notifyCheckChanged(viewHolder: ViewHolder, imageBean: LocalMedia) {
        viewHolder.tvCheck.setText("")
        val size = selectData!!.size
        for (i in 0 until size) {
            val media: LocalMedia? = selectData!![i]
            if (media.getPath().equals(imageBean.getPath())
                    || media.getId() === imageBean.getId()) {
                imageBean.setNum(media.getNum())
                media.setPosition(imageBean.getPosition())
                viewHolder.tvCheck.setText(java.lang.String.valueOf(imageBean.getNum()))
            }
        }
    }

    /**
     * Update the selected status of the image
     *
     * @param contentHolder
     * @param image
     */
    @SuppressLint("StringFormatMatches")
    private fun changeCheckboxState(contentHolder: ViewHolder, image: LocalMedia) {
        val isChecked: Boolean = contentHolder.tvCheck.isSelected()
        val count = selectData!!.size
        val mimeType = if (count > 0) selectData!![0].getMimeType() else ""
        if (config.isWithVideoImage) {
            // isWithVideoImage mode
            var videoSize = 0
            for (i in 0 until count) {
                val media: LocalMedia? = selectData!![i]
                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    videoSize++
                }
            }
            if (PictureMimeType.isHasVideo(image.getMimeType())) {
                if (config.maxVideoSelectNum <= 0) {
                    showPromptDialog(context.getString(R.string.picture_rule))
                    return
                }
                if (selectedSize >= config.maxSelectNum && !isChecked) {
                    showPromptDialog(context.getString(R.string.picture_message_max_num, config.maxSelectNum))
                    return
                }
                if (videoSize >= config.maxVideoSelectNum && !isChecked) {
                    showPromptDialog(StringUtils.getMsg(context, image.getMimeType(), config.maxVideoSelectNum))
                    return
                }
                if (!isChecked && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                    showPromptDialog(context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                    return
                }
                if (!isChecked && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                    showPromptDialog(context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                    return
                }
            }
            if (PictureMimeType.isHasImage(image.getMimeType())) {
                if (selectedSize >= config.maxSelectNum && !isChecked) {
                    showPromptDialog(context.getString(R.string.picture_message_max_num, config.maxSelectNum))
                    return
                }
            }
        } else {
            if (!TextUtils.isEmpty(mimeType)) {
                val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, image.getMimeType())
                if (!mimeTypeSame) {
                    showPromptDialog(context.getString(R.string.picture_rule))
                    return
                }
            }
            if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) {
                if (count >= config.maxVideoSelectNum && !isChecked) {
                    showPromptDialog(StringUtils.getMsg(context, mimeType, config.maxVideoSelectNum))
                    return
                }
                if (!isChecked && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                    showPromptDialog(context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                    return
                }
                if (!isChecked && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                    showPromptDialog(context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                    return
                }
            } else {
                if (count >= config.maxSelectNum && !isChecked) {
                    showPromptDialog(StringUtils.getMsg(context, mimeType, config.maxSelectNum))
                    return
                }
                if (PictureMimeType.isHasVideo(image.getMimeType())) {
                    if (!isChecked && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                        showPromptDialog(context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                        return
                    }
                    if (!isChecked && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                        showPromptDialog(context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                        return
                    }
                }
            }
        }
        if (isChecked) {
            for (i in 0 until count) {
                val media: LocalMedia? = selectData!![i]
                if (media == null || TextUtils.isEmpty(media.getPath())) {
                    continue
                }
                if (media.getPath().equals(image.getPath())
                        || media.getId() === image.getId()) {
                    selectData!!.remove(media)
                    subSelectPosition()
                    AnimUtils.disZoom(contentHolder.ivPicture, config.zoomAnim)
                    break
                }
            }
        } else {
            // The radio
            if (config.selectionMode == PictureConfig.SINGLE) {
                singleRadioMediaImage()
            }

            // If the width and height are 0, regain the width and height
            if (image.getWidth() === 0 || image.getHeight() === 0) {
                var width = 0
                var height = 0
                image.setOrientation(-1)
                if (PictureMimeType.isContent(image.getPath())) {
                    if (PictureMimeType.isHasVideo(image.getMimeType())) {
                        val size: IntArray = MediaUtils.getVideoSizeForUri(context, Uri.parse(image.getPath()))
                        width = size[0]
                        height = size[1]
                    } else if (PictureMimeType.isHasImage(image.getMimeType())) {
                        val size: IntArray = MediaUtils.getImageSizeForUri(context, Uri.parse(image.getPath()))
                        width = size[0]
                        height = size[1]
                    }
                } else {
                    if (PictureMimeType.isHasVideo(image.getMimeType())) {
                        val size: IntArray = MediaUtils.getVideoSizeForUrl(image.getPath())
                        width = size[0]
                        height = size[1]
                    } else if (PictureMimeType.isHasImage(image.getMimeType())) {
                        val size: IntArray = MediaUtils.getImageSizeForUrl(image.getPath())
                        width = size[0]
                        height = size[1]
                    }
                }
                image.setWidth(width)
                image.setHeight(height)
            }
            selectData!!.add(image)
            image.setNum(selectData!!.size)
            VoiceUtils.getInstance().play()
            AnimUtils.zoom(contentHolder.ivPicture, config.zoomAnim)
            contentHolder.tvCheck.startAnimation(AnimationUtils.loadAnimation(context, R.anim.picture_anim_modal_in))
        }
        var isRefreshAll = false
        if (config.isMaxSelectEnabledMask) {
            if (config.chooseMode == PictureMimeType.ofAll()) {
                // ofAll
                if (config.isWithVideoImage && config.maxVideoSelectNum > 0) {
                    if (selectedSize >= config.maxSelectNum) {
                        isRefreshAll = true
                    }
                    if (isChecked) {
                        // delete
                        if (selectedSize == config.maxSelectNum - 1) {
                            isRefreshAll = true
                        }
                    }
                } else {
                    if (!isChecked && selectedSize == 1) {
                        // add
                        isRefreshAll = true
                    }
                    if (isChecked && selectedSize == 0) {
                        // delete
                        isRefreshAll = true
                    }
                }
            } else {
                // ofImage or ofVideo or ofAudio
                if (config.chooseMode == PictureMimeType.ofVideo() && config.maxVideoSelectNum > 0) {
                    if (!isChecked && selectedSize == config.maxVideoSelectNum) {
                        // add
                        isRefreshAll = true
                    }
                    if (isChecked && selectedSize == config.maxVideoSelectNum - 1) {
                        // delete
                        isRefreshAll = true
                    }
                } else {
                    if (!isChecked && selectedSize == config.maxSelectNum) {
                        // add
                        isRefreshAll = true
                    }
                    if (isChecked && selectedSize == config.maxSelectNum - 1) {
                        // delete
                        isRefreshAll = true
                    }
                }
            }
        }
        if (isRefreshAll) {
            notifyDataSetChanged()
        } else {
            notifyItemChanged(contentHolder.getAdapterPosition())
        }
        selectImage(contentHolder, !isChecked)
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectData)
        }
    }

    /**
     * Radio mode
     */
    private fun singleRadioMediaImage() {
        if (selectData != null
                && selectData!!.size > 0) {
            val media: LocalMedia? = selectData!![0]
            notifyItemChanged(media.position)
            selectData!!.clear()
        }
    }

    /**
     * Update the selection order
     */
    private fun subSelectPosition() {
        if (config.checkNumMode) {
            val size = selectData!!.size
            for (index in 0 until size) {
                val media: LocalMedia? = selectData!![index]
                media.setNum(index + 1)
                notifyItemChanged(media.position)
            }
        }
    }

    /**
     * Select the image and animate it
     *
     * @param holder
     * @param isChecked
     */
    fun selectImage(holder: ViewHolder, isChecked: Boolean) {
        holder.tvCheck.setSelected(isChecked)
        if (isChecked) {
            holder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.picture_color_80), PorterDuff.Mode.SRC_ATOP)
        } else {
            holder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.picture_color_20), PorterDuff.Mode.SRC_ATOP)
        }
    }

    /**
     * Tips
     */
    private fun showPromptDialog(content: String) {
        val dialog = PictureCustomDialog(context, R.layout.picture_prompt_dialog)
        val btnOk: TextView = dialog.findViewById(R.id.btnOk)
        val tvContent: TextView = dialog.findViewById(R.id.tv_content)
        tvContent.setText(content)
        btnOk.setOnClickListener(View.OnClickListener { v: View? -> dialog.dismiss() })
        dialog.show()
    }

    /**
     * Binding listener
     *
     * @param imageSelectChangedListener
     */
    fun setOnPhotoSelectChangedListener(imageSelectChangedListener: OnPhotoSelectChangedListener?) {
        this.imageSelectChangedListener = imageSelectChangedListener
    }

    init {
        this.config = config
        isShowCamera = config.isCamera
    }
}