package com.luck.picture.lib.instagram

import android.animation.Animator
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.util.*

/**
 * @author：luck
 * @date：2016-12-30 12:02
 * @describe：图片列表
 */
class InstagramImageGridAdapter(private val context: Context, config: PictureSelectionConfig) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private var showCamera: Boolean
    private var imageSelectChangedListener: OnPhotoSelectChangedListener? = null
    private var images: List<LocalMedia>? = ArrayList<LocalMedia>()
    private var selectImages: MutableList<LocalMedia?>? = ArrayList<LocalMedia?>()
    private val config: PictureSelectionConfig
    private var mPreviewPosition = 0
    private var lastClickTime: Long = 0

    /**
     * 单选图片
     */
    private var isGo = false
    fun setShowCamera(showCamera: Boolean) {
        this.showCamera = showCamera
    }

    fun isShowCamera(): Boolean {
        return showCamera
    }

    fun bindImagesData(images: List<LocalMedia>?) {
        this.images = images ?: ArrayList<LocalMedia>()
        notifyDataSetChanged()
    }

    fun bindSelectImages(images: List<LocalMedia>?) {
        // 这里重新构构造一个新集合，不然会产生已选集合一变，结果集合也会添加的问题
        val selection: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
        val size = images!!.size
        for (i in 0 until size) {
            val media: LocalMedia = images[i]
            selection.add(media)
        }
        selectImages = selection
        subSelectPosition()
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener!!.onChange(selectImages)
        }
    }

    fun getSelectedImages(): MutableList<LocalMedia>? {
        return if (selectImages == null) ArrayList<LocalMedia>() else selectImages
    }

    fun getImages(): List<LocalMedia> {
        return if (images == null) ArrayList<LocalMedia>() else images!!
    }

    fun isDataEmpty(): Boolean {
        return images == null || images!!.size == 0
    }

    fun getSize(): Int {
        return if (images == null) 0 else images!!.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (showCamera && position == 0) {
            PictureConfig.TYPE_CAMERA
        } else {
            PictureConfig.TYPE_PICTURE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == PictureConfig.TYPE_CAMERA) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.picture_item_camera, parent, false)
            HeaderViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(context).inflate(R.layout.instagram_image_grid_item, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == PictureConfig.TYPE_CAMERA) {
            val headerHolder = holder as HeaderViewHolder
            headerHolder.headerView.setOnClickListener { v: View? ->
                if (imageSelectChangedListener != null) {
                    imageSelectChangedListener!!.onTakePhoto()
                }
            }
        } else {
            val contentHolder = holder as ViewHolder
            val image: LocalMedia = images!![if (showCamera) position - 1 else position]
            image.position = contentHolder.getAdapterPosition()
            val path: String = image.getPath()
            val mimeType: String = image.getMimeType()
            if (config.checkNumMode) {
                notifyCheckChanged(contentHolder, image)
            }
            if (!config.isSingleDirectReturn) {
                selectImage(contentHolder, isSelected(image))
            } else {
                contentHolder.ivPicture.setColorFilter(ContextCompat.getColor(context, R.color.picture_color_20), PorterDuff.Mode.SRC_ATOP)
            }
            val tag = contentHolder.ivPicture.tag
            var isLoadingAnim = false
            if (tag != null && tag is Boolean) {
                isLoadingAnim = tag
            }
            if (!config.isSingleDirectReturn) {
                if (!isLoadingAnim) {
                    if (isSelected(image)) {
                        if (contentHolder.ivPicture.scaleX != 1.12f) {
                            contentHolder.ivPicture.scaleX = 1.12f
                            contentHolder.ivPicture.scaleY = 1.12f
                        }
                    } else {
                        if (contentHolder.ivPicture.scaleX != 1f) {
                            contentHolder.ivPicture.scaleX = 1f
                            contentHolder.ivPicture.scaleY = 1f
                        }
                    }
                }
            } else {
                if (contentHolder.ivPicture.scaleX != 1f) {
                    contentHolder.ivPicture.scaleX = 1f
                    contentHolder.ivPicture.scaleY = 1f
                }
            }
            val gif: Boolean = PictureMimeType.isGif(mimeType)
            contentHolder.tvCheck.setVisibility(if (config.isSingleDirectReturn) View.GONE else View.VISIBLE)
            contentHolder.btnCheck.visibility = if (config.isSingleDirectReturn) View.GONE else View.VISIBLE
            contentHolder.tvIsGif.setVisibility(if (gif) View.VISIBLE else View.GONE)
            val eqImage: Boolean = PictureMimeType.isHasImage(image.getMimeType())
            if (eqImage) {
                val eqLongImg: Boolean = MediaUtils.isLongImg(image)
                contentHolder.tvLongChart.setVisibility(if (eqLongImg) View.VISIBLE else View.GONE)
            } else {
                contentHolder.tvLongChart.setVisibility(View.GONE)
            }
            val isHasVideo: Boolean = PictureMimeType.isHasVideo(mimeType)
            val eqAudio: Boolean = PictureMimeType.isHasAudio(mimeType)
            if (isHasVideo || eqAudio) {
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
            if (mPreviewPosition == position) {
                contentHolder.maskView.visibility = View.VISIBLE
            } else {
                contentHolder.maskView.visibility = View.GONE
            }
            if (config.enablePreview || config.enPreviewVideo || config.enablePreviewAudio) {
                contentHolder.btnCheck.setOnClickListener { v: View? ->
                    if (isFastDoubleClick()) {
                        return@setOnClickListener
                    }
                    // 如原图路径不存在或者路径存在但文件不存在
                    val newPath = if (SdkVersionUtils.checkedAndroid_Q()) PictureFileUtils.getPath(context, Uri.parse(path)) else path
                    if (!TextUtils.isEmpty(newPath) && !File(newPath).exists()) {
                        ToastUtils.s(context, PictureMimeType.s(context, mimeType))
                        return@setOnClickListener
                    }
                    if (SdkVersionUtils.checkedAndroid_Q()) {
                        image.setRealPath(newPath)
                    }
                    changeCheckboxState(contentHolder, image, if (showCamera) position - 1 else position)
                }
            }
            contentHolder.contentView.setOnClickListener { v: View? ->
                // 如原图路径不存在或者路径存在但文件不存在
                val newPath = if (SdkVersionUtils.checkedAndroid_Q()) PictureFileUtils.getPath(context, Uri.parse(path)) else path
                if (!TextUtils.isEmpty(newPath) && !File(newPath).exists()) {
                    ToastUtils.s(context, PictureMimeType.s(context, mimeType))
                    return@setOnClickListener
                }
                val index = if (showCamera) position - 1 else position
                if (index == -1) {
                    return@setOnClickListener
                }
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    image.setRealPath(newPath)
                }
                val eqResult = PictureMimeType.isHasImage(mimeType) && config.enablePreview || PictureMimeType.isHasVideo(mimeType) && (config.enPreviewVideo
                        || config.selectionMode == PictureConfig.SINGLE) || PictureMimeType.isHasAudio(mimeType) && (config.enablePreviewAudio
                        || config.selectionMode == PictureConfig.SINGLE)
                if (eqResult) {
                    if (PictureMimeType.isHasVideo(image.getMimeType())) {
                        if (config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                            // 视频小于最低指定的长度
                            ToastUtils.s(context,
                                    contentHolder.contentView.context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                            return@setOnClickListener
                        }
                        if (config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                            // 视频时长超过了指定的长度
                            ToastUtils.s(context,
                                    contentHolder.contentView.context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                            return@setOnClickListener
                        }
                    }
                    if (imageSelectChangedListener != null) {
                        imageSelectChangedListener!!.onPictureClick(image, index)
                    }
                } else {
                    changeCheckboxState(contentHolder, image, index)
                }
            }
        }
    }

    fun isFastDoubleClick(): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastClickTime < 800) {
            return true
        }
        lastClickTime = time
        return false
    }

    override fun getItemCount(): Int {
        return if (showCamera) images!!.size + 1 else images!!.size
    }

    inner class HeaderViewHolder(var headerView: View) : RecyclerView.ViewHolder(headerView) {
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
        var maskView: View

        init {
            ivPicture = contentView.findViewById(R.id.ivPicture)
            tvCheck = contentView.findViewById<TextView>(R.id.tvCheck)
            btnCheck = contentView.findViewById(R.id.btnCheck)
            tvDuration = contentView.findViewById<TextView>(R.id.tv_duration)
            tvIsGif = contentView.findViewById<TextView>(R.id.tv_isGif)
            tvLongChart = contentView.findViewById<TextView>(R.id.tv_long_chart)
            maskView = contentView.findViewById(R.id.iv_mask)
            if (config.style != null) {
                if (config.style.pictureCheckedStyle !== 0) {
                    tvCheck.setBackgroundResource(config.style.pictureCheckedStyle)
                }
            }
        }
    }

    fun isSelected(image: LocalMedia): Boolean {
        val size = selectImages!!.size
        for (i in 0 until size) {
            val media: LocalMedia? = selectImages!![i]
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
     * 选择按钮更新
     */
    private fun notifyCheckChanged(viewHolder: ViewHolder, imageBean: LocalMedia) {
        viewHolder.tvCheck.setText("")
        val size = selectImages!!.size
        for (i in 0 until size) {
            val media: LocalMedia? = selectImages!![i]
            if (media.getPath().equals(imageBean.getPath())
                    || media.getId() === imageBean.getId()) {
                imageBean.setNum(media.getNum())
                media.setPosition(imageBean.getPosition())
                viewHolder.tvCheck.setText(java.lang.String.valueOf(imageBean.getNum()))
            }
        }
    }

    /**
     * 改变图片选中状态
     *
     * @param contentHolder
     * @param image
     */
    @SuppressLint("StringFormatMatches")
    private fun changeCheckboxState(contentHolder: ViewHolder, image: LocalMedia, position: Int) {
        val isChecked: Boolean = contentHolder.tvCheck.isSelected()
        val size = selectImages!!.size
        val mimeType = if (size > 0) selectImages!![0].getMimeType() else ""
        if (config.isWithVideoImage) {
            // 混选模式
            var videoSize = 0
            var imageSize = 0
            for (i in 0 until size) {
                val media: LocalMedia? = selectImages!![i]
                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    videoSize++
                } else {
                    imageSize++
                }
            }
            if (PictureMimeType.isHasVideo(image.getMimeType())) {
                if (config.maxVideoSelectNum > 0 && videoSize >= config.maxVideoSelectNum && !isChecked) {
                    // 如果选择的是视频
                    ToastUtils.s(context, StringUtils.getMsg(context, image.getMimeType(), config.maxVideoSelectNum))
                    return
                }
                if (!isChecked && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                    // 视频小于最低指定的长度
                    ToastUtils.s(context,
                            contentHolder.contentView.context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                    return
                }
                if (!isChecked && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                    // 视频时长超过了指定的长度
                    ToastUtils.s(context,
                            contentHolder.contentView.context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                    return
                }
            }
            if (PictureMimeType.isHasImage(image.getMimeType()) && imageSize >= config.maxSelectNum && !isChecked) {
                ToastUtils.s(context, StringUtils.getMsg(context, image.getMimeType(), config.maxSelectNum))
                return
            }
        } else {
            // 非混选模式
            if (!TextUtils.isEmpty(mimeType)) {
                val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, image.getMimeType())
                if (!mimeTypeSame) {
                    ToastUtils.s(context, context.getString(R.string.picture_rule))
                    return
                }
            }
            if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) {
                if (size >= config.maxVideoSelectNum && !isChecked) {
                    // 如果先选择的是视频
                    ToastUtils.s(context, StringUtils.getMsg(context, mimeType, config.maxVideoSelectNum))
                    return
                }
                if (!isChecked && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                    // 视频小于最低指定的长度
                    ToastUtils.s(context,
                            contentHolder.contentView.context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                    return
                }
                if (!isChecked && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                    // 视频时长超过了指定的长度
                    ToastUtils.s(context,
                            contentHolder.contentView.context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                    return
                }
            } else {
                if (size >= config.maxSelectNum && !isChecked) {
                    ToastUtils.s(context, StringUtils.getMsg(context, mimeType, config.maxSelectNum))
                    return
                }
                if (PictureMimeType.isHasVideo(image.getMimeType())) {
                    if (!isChecked && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                        // 视频小于最低指定的长度
                        ToastUtils.s(context,
                                contentHolder.contentView.context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                        return
                    }
                    if (!isChecked && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                        // 视频时长超过了指定的长度
                        ToastUtils.s(context,
                                contentHolder.contentView.context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                        return
                    }
                }
            }
        }
        if (isChecked) {
            for (i in 0 until size) {
                val media: LocalMedia? = selectImages!![i]
                if (media == null || TextUtils.isEmpty(media.getPath())) {
                    continue
                }
                if (media.getPath().equals(image.getPath())
                        || media.getId() === image.getId()) {
                    selectImages!!.remove(media)
                    subSelectPosition()
                    if (contentHolder.ivPicture.scaleX == 1.12f) {
                        disZoom(contentHolder.ivPicture, config.zoomAnim)
                    }
                    break
                }
            }
        } else {
            // 如果是单选，则清空已选中的并刷新列表(作单一选择)
            if (config.selectionMode == PictureConfig.SINGLE) {
                singleRadioMediaImage()
            }
            selectImages!!.add(image)
            image.setNum(selectImages!!.size)
            VoiceUtils.getInstance().play()
            if (contentHolder.ivPicture.scaleX == 1f) {
                zoom(contentHolder.ivPicture, config.zoomAnim)
            }
            contentHolder.tvCheck.startAnimation(AnimationUtils.loadAnimation(context, R.anim.picture_anim_modal_in))
        }
        //通知点击项发生了改变
        notifyItemChanged(contentHolder.getAdapterPosition())
        selectImage(contentHolder, !isChecked)
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener!!.onItemChecked(position, image, !isChecked)
            imageSelectChangedListener!!.onChange(selectImages)
        }
    }

    /**
     * 单选模式
     */
    private fun singleRadioMediaImage() {
        if (selectImages != null
                && selectImages!!.size > 0) {
            isGo = true
            val media: LocalMedia? = selectImages!![0]
            notifyItemChanged(if (config.isCamera) media.position else if (isGo) media.position else if (media.position > 0) media.position - 1 else 0)
            selectImages!!.clear()
        }
    }

    /**
     * 更新选择的顺序
     */
    private fun subSelectPosition() {
        if (config.checkNumMode) {
            val size = selectImages!!.size
            var index = 0
            while (index < size) {
                val media: LocalMedia? = selectImages!![index]
                media.setNum(index + 1)
                notifyItemChanged(media.position)
                index++
            }
        }
    }

    fun setPreviewPosition(previewPosition: Int) {
        if (previewPosition < 0 || previewPosition >= getItemCount()) {
            return
        }
        mPreviewPosition = previewPosition
    }

    /**
     * 选中的图片并执行动画
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

    interface OnPhotoSelectChangedListener {
        fun onItemChecked(position: Int, image: LocalMedia?, isCheck: Boolean)

        /**
         * 拍照回调
         */
        fun onTakePhoto()

        /**
         * 已选Media回调
         *
         * @param selectImages
         */
        fun onChange(selectImages: List<LocalMedia?>?)

        /**
         * 图片预览回调
         *
         * @param media
         * @param position
         */
        fun onPictureClick(media: LocalMedia?, position: Int)
    }

    fun setOnPhotoSelectChangedListener(imageSelectChangedListener: OnPhotoSelectChangedListener?) {
        this.imageSelectChangedListener = imageSelectChangedListener
    }

    private fun zoom(view: View, isZoomAnim: Boolean) {
        if (isZoomAnim) {
            val set = AnimatorSet()
            set.playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.12f)
            )
            set.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    view.tag = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    view.tag = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    view.tag = false
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
            set.setDuration(400)
            set.start()
        }
    }

    private fun disZoom(view: View, isZoomAnim: Boolean) {
        if (isZoomAnim) {
            val set = AnimatorSet()
            set.playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1.12f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1.12f, 1f)
            )
            set.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    view.tag = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    view.tag = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    view.tag = false
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
            set.setDuration(400)
            set.start()
        }
    }

    init {
        this.config = config
        showCamera = config.isCamera
    }
}