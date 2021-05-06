package com.luck.picture.lib.adapter

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.util.*

/**
 * @author：luck
 * @data：2018/1/27 下午7:50
 * @describe:PictureSimpleFragmentAdapter
 */
class PictureSimpleFragmentAdapter(config: PictureSelectionConfig?,
                                   onBackPressed: OnCallBackActivity?) : PagerAdapter() {
    private var data: List<LocalMedia>? = null
    private val onBackPressed: OnCallBackActivity?
    private val config: PictureSelectionConfig?

    /**
     * To cache the view
     */
    private var mCacheView: SparseArray<View>?
    fun clear() {
        if (null != mCacheView) {
            mCacheView.clear()
            mCacheView = null
        }
    }

    fun removeCacheView(position: Int) {
        if (mCacheView != null && position < mCacheView.size()) {
            mCacheView.removeAt(position)
        }
    }

    interface OnCallBackActivity {
        /**
         * Close Activity
         */
        fun onActivityBackPressed()
    }

    /**
     * bind data
     *
     * @param data
     */
    fun bindData(data: List<LocalMedia>?) {
        this.data = data
    }

    /**
     * get data
     *
     * @return
     */
    fun getData(): List<LocalMedia> {
        return if (data == null) ArrayList<LocalMedia>() else data!!
    }

    fun getSize(): Int {
        return if (data == null) 0 else data!!.size
    }

    fun remove(currentItem: Int) {
        if (getSize() > currentItem) {
            data.removeAt(currentItem)
        }
    }

    fun getItem(position: Int): LocalMedia? {
        return if (getSize() > 0 && position < getSize()) data!![position] else null
    }

    override fun getCount(): Int {
        return if (data != null) data!!.size else 0
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
        if (mCacheView.size() > MAX_CACHE_SIZE) {
            mCacheView.remove(position)
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        var contentView: View = mCacheView.get(position)
        if (contentView == null) {
            contentView = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.picture_image_preview, container, false)
            mCacheView.put(position, contentView)
        }
        val imageView: PhotoView = contentView.findViewById(R.id.preview_image)
        val longImg: SubsamplingScaleImageView = contentView.findViewById(R.id.longImg)
        val ivPlay = contentView.findViewById<ImageView>(R.id.iv_play)
        val media: LocalMedia? = getItem(position)
        if (media != null) {
            val mimeType: String = media.getMimeType()
            val path: String
            path = if (media.isCut() && !media.isCompressed()) {
                media.getCutPath()
            } else if (media.isCompressed() || media.isCut() && media.isCompressed()) {
                media.getCompressPath()
            } else {
                media.getPath()
            }
            val isGif: Boolean = PictureMimeType.isGif(mimeType)
            val isHasVideo: Boolean = PictureMimeType.isHasVideo(mimeType)
            ivPlay.visibility = if (isHasVideo) View.VISIBLE else View.GONE
            ivPlay.setOnClickListener { v: View? ->
                if (PictureSelectionConfig.customVideoPlayCallback != null) {
                    PictureSelectionConfig.customVideoPlayCallback.startPlayVideo(media)
                } else {
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putBoolean(PictureConfig.EXTRA_PREVIEW_VIDEO, true)
                    bundle.putString(PictureConfig.EXTRA_VIDEO_PATH, path)
                    intent.putExtras(bundle)
                    JumpUtils.startPictureVideoPlayActivity(container.getContext(), bundle, PictureConfig.PREVIEW_VIDEO_CODE)
                }
            }
            val eqLongImg: Boolean = MediaUtils.isLongImg(media)
            imageView.setVisibility(if (eqLongImg && !isGif) View.GONE else View.VISIBLE)
            imageView.setOnViewTapListener { view, x, y -> onBackPressed?.onActivityBackPressed() }
            longImg.setVisibility(if (eqLongImg && !isGif) View.VISIBLE else View.GONE)
            longImg.setOnClickListener { v -> onBackPressed?.onActivityBackPressed() }
            if (isGif && !media.isCompressed()) {
                if (config != null && PictureSelectionConfig.imageEngine != null) {
                    PictureSelectionConfig.imageEngine.loadAsGifImage(contentView.context, path, imageView)
                }
            } else {
                if (config != null && PictureSelectionConfig.imageEngine != null) {
                    if (eqLongImg) {
                        displayLongPic(if (PictureMimeType.isContent(path)) Uri.parse(path) else Uri.fromFile(File(path)), longImg)
                    } else {
                        PictureSelectionConfig.imageEngine.loadImage(contentView.context, path, imageView)
                    }
                }
            }
        }
        container.addView(contentView, 0)
        return contentView
    }

    /**
     * load long image
     *
     * @param uri
     * @param longImg
     */
    private fun displayLongPic(uri: Uri, longImg: SubsamplingScaleImageView) {
        longImg.setQuickScaleEnabled(true)
        longImg.setZoomEnabled(true)
        longImg.setPanEnabled(true)
        longImg.setDoubleTapZoomDuration(100)
        longImg.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)
        longImg.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        longImg.setImage(ImageSource.uri(uri), ImageViewState(0, PointF(0, 0), 0))
    }

    companion object {
        /**
         * Maximum number of cached images
         */
        private const val MAX_CACHE_SIZE = 20
    }

    init {
        this.config = config
        this.onBackPressed = onBackPressed
        mCacheView = SparseArray<View>()
    }
}