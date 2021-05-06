package com.luck.picture.lib

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.luck.picture.lib.broadcast.BroadcastAction
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.*

/**
 * @author：luck
 * @data：2017/01/18 下午1:00
 * @描述: 预览图片
 */
class PictureExternalPreviewActivity : PictureBaseActivity(), View.OnClickListener {
    private var ibLeftBack: ImageButton? = null
    private var tvTitle: TextView? = null
    private var viewPager: PreviewViewPager? = null
    private var images: List<LocalMedia>? = ArrayList<LocalMedia>()
    private var position = 0
    private var adapter: SimpleFragmentAdapter? = null
    private var downloadPath: String? = null
    private var mMimeType: String? = null
    private var ibDelete: ImageButton? = null
    private var titleViewBg: View? = null
    override val resourceId: Int
        get() = R.layout.picture_activity_external_preview

    protected override fun initWidgets() {
        super.initWidgets()
        titleViewBg = findViewById(R.id.titleViewBg)
        tvTitle = findViewById(R.id.picture_title)
        ibLeftBack = findViewById(R.id.left_back)
        ibDelete = findViewById(R.id.ib_delete)
        viewPager = findViewById(R.id.preview_pager)
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0)
        images = getIntent().getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST)
        ibLeftBack.setOnClickListener(this)
        ibDelete.setOnClickListener(this)
        ibDelete.setVisibility(if (config.style != null) if (config.style.pictureExternalPreviewGonePreviewDelete) View.VISIBLE else View.GONE else View.GONE)
        initViewPageAdapterData()
    }

    /**
     * 设置样式
     */
    override fun initPictureSelectorStyle() {
        if (config.style != null) {
            if (config.style.pictureTitleTextColor !== 0) {
                tvTitle.setTextColor(config.style.pictureTitleTextColor)
            }
            if (config.style.pictureTitleTextSize !== 0) {
                tvTitle.setTextSize(config.style.pictureTitleTextSize)
            }
            if (config.style.pictureLeftBackIcon !== 0) {
                ibLeftBack.setImageResource(config.style.pictureLeftBackIcon)
            }
            if (config.style.pictureExternalPreviewDeleteStyle !== 0) {
                ibDelete.setImageResource(config.style.pictureExternalPreviewDeleteStyle)
            }
            if (config.style.pictureTitleBarBackgroundColor !== 0) {
                titleViewBg!!.setBackgroundColor(colorPrimary)
            }
        } else {
            val previewBgColor: Int = AttrsUtils.getTypeValueColor(getContext(), R.attr.picture_ac_preview_title_bg)
            if (previewBgColor != 0) {
                titleViewBg!!.setBackgroundColor(previewBgColor)
            } else {
                titleViewBg!!.setBackgroundColor(colorPrimary)
            }
        }
    }

    private fun initViewPageAdapterData() {
        tvTitle.setText(getString(R.string.picture_preview_image_num,
                position + 1, images!!.size))
        adapter = SimpleFragmentAdapter()
        viewPager.setAdapter(adapter)
        viewPager.setCurrentItem(position)
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(index: Int) {
                tvTitle.setText(getString(R.string.picture_preview_image_num,
                        index + 1, images!!.size))
                position = index
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.left_back) {
            finish()
            exitAnimation()
        } else if (id == R.id.ib_delete) {
            if (images != null && images!!.size > 0) {
                val currentItem: Int = viewPager.getCurrentItem()
                images.removeAt(currentItem)
                adapter!!.removeCacheView(currentItem)
                // 删除通知用户更新
                val bundle = Bundle()
                bundle.putInt(PictureConfig.EXTRA_PREVIEW_DELETE_POSITION, currentItem)
                BroadcastManager.getInstance(getContext())
                        .action(BroadcastAction.ACTION_DELETE_PREVIEW_POSITION)
                        .extras(bundle).broadcast()
                if (images!!.size == 0) {
                    onBackPressed()
                    return
                }
                tvTitle.setText(getString(R.string.picture_preview_image_num,
                        position + 1, images!!.size))
                position = currentItem
                adapter.notifyDataSetChanged()
            }
        }
    }

    inner class SimpleFragmentAdapter : PagerAdapter() {
        /**
         * 缓存view
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

        val count: Int
            get() = if (images != null) images!!.size else 0

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
            if (mCacheView.size() > Companion.MAX_CACHE_SIZE) {
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
            // 常规图控件
            val imageView: PhotoView = contentView.findViewById(R.id.preview_image)
            // 长图控件
            val longImageView: SubsamplingScaleImageView = contentView.findViewById(R.id.longImg)
            // 视频播放按钮
            val ivPlay = contentView.findViewById<ImageView>(R.id.iv_play)
            val media: LocalMedia = images!![position]
            if (media != null) {
                val path: String
                path = if (media.isCut() && !media.isCompressed()) {
                    // 裁剪过
                    media.getCutPath()
                } else if (media.isCompressed() || media.isCut() && media.isCompressed()) {
                    // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                    media.getCompressPath()
                } else if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
                    // AndroidQ特有path
                    media.getAndroidQToPath()
                } else {
                    // 原图
                    media.getPath()
                }
                val isHttp: Boolean = PictureMimeType.isHasHttp(path)
                val mimeType: String = if (isHttp) PictureMimeType.getImageMimeType(media.getPath()) else media.getMimeType()
                val isHasVideo: Boolean = PictureMimeType.isHasVideo(mimeType)
                ivPlay.visibility = if (isHasVideo) View.VISIBLE else View.GONE
                val isGif: Boolean = PictureMimeType.isGif(mimeType)
                val eqLongImg: Boolean = MediaUtils.isLongImg(media)
                imageView.setVisibility(if (eqLongImg && !isGif) View.GONE else View.VISIBLE)
                longImageView.setVisibility(if (eqLongImg && !isGif) View.VISIBLE else View.GONE)
                // 压缩过的gif就不是gif了
                if (isGif && !media.isCompressed()) {
                    if (config != null && PictureSelectionConfig.imageEngine != null) {
                        PictureSelectionConfig.imageEngine.loadAsGifImage(getContext(), path, imageView)
                    }
                } else {
                    if (config != null && PictureSelectionConfig.imageEngine != null) {
                        if (isHttp) {
                            // 网络图片
                            PictureSelectionConfig.imageEngine.loadImage(contentView.context, path,
                                    imageView, longImageView, object : OnImageCompleteCallback() {
                                fun onShowLoading() {
                                    showPleaseDialog()
                                }

                                fun onHideLoading() {
                                    dismissDialog()
                                }
                            })
                        } else {
                            if (eqLongImg) {
                                displayLongPic(if (PictureMimeType.isContent(path)) Uri.parse(path) else Uri.fromFile(File(path)), longImageView)
                            } else {
                                PictureSelectionConfig.imageEngine.loadImage(contentView.context, path, imageView)
                            }
                        }
                    }
                }
                imageView.setOnViewTapListener { view, x, y ->
                    finish()
                    exitAnimation()
                }
                longImageView.setOnClickListener { v ->
                    finish()
                    exitAnimation()
                }
                if (!isHasVideo) {
                    longImageView.setOnLongClickListener { v ->
                        if (config.isNotPreviewDownload) {
                            if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                downloadPath = path
                                val currentMimeType: String = if (PictureMimeType.isHasHttp(path)) PictureMimeType.getImageMimeType(media.getPath()) else media.getMimeType()
                                mMimeType = if (PictureMimeType.isJPG(currentMimeType)) PictureMimeType.MIME_TYPE_JPEG else currentMimeType
                                showDownLoadDialog()
                            } else {
                                PermissionChecker.requestPermissions(this@PictureExternalPreviewActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE)
                            }
                        }
                        true
                    }
                }
                if (!isHasVideo) {
                    imageView.setOnLongClickListener { v ->
                        if (config.isNotPreviewDownload) {
                            if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                downloadPath = path
                                val currentMimeType: String = if (PictureMimeType.isHasHttp(path)) PictureMimeType.getImageMimeType(media.getPath()) else media.getMimeType()
                                mMimeType = if (PictureMimeType.isJPG(currentMimeType)) PictureMimeType.MIME_TYPE_JPEG else currentMimeType
                                showDownLoadDialog()
                            } else {
                                PermissionChecker.requestPermissions(this@PictureExternalPreviewActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE)
                            }
                        }
                        true
                    }
                }
                ivPlay.setOnClickListener { v: View? ->
                    if (PictureSelectionConfig.customVideoPlayCallback != null) {
                        PictureSelectionConfig.customVideoPlayCallback.startPlayVideo(media)
                    } else {
                        val intent = Intent()
                        val bundle = Bundle()
                        bundle.putString(PictureConfig.EXTRA_VIDEO_PATH, path)
                        intent.putExtras(bundle)
                        JumpUtils.startPictureVideoPlayActivity(container.getContext(), bundle, PictureConfig.PREVIEW_VIDEO_CODE)
                    }
                }
            }
            container.addView(contentView, 0)
            return contentView
        }

        companion object {
            /**
             * 最大缓存图片数量
             */
            private const val MAX_CACHE_SIZE = 20
        }

        init {
            mCacheView = SparseArray<View>()
        }
    }

    /**
     * 加载长图
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

    /**
     * 下载图片提示
     */
    private fun showDownLoadDialog() {
        if (!isFinishing() && !TextUtils.isEmpty(downloadPath)) {
            val dialog = PictureCustomDialog(getContext(), R.layout.picture_wind_base_dialog)
            val btn_cancel: Button = dialog.findViewById(R.id.btn_cancel)
            val btn_commit: Button = dialog.findViewById(R.id.btn_commit)
            val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
            val tv_content: TextView = dialog.findViewById(R.id.tv_content)
            tvTitle.setText(getString(R.string.picture_prompt))
            tv_content.setText(getString(R.string.picture_prompt_content))
            btn_cancel.setOnClickListener { v: View? ->
                if (!isFinishing()) {
                    dialog.dismiss()
                }
            }
            btn_commit.setOnClickListener { view: View? ->
                val isHttp: Boolean = PictureMimeType.isHasHttp(downloadPath)
                showPleaseDialog()
                if (isHttp) {
                    PictureThreadUtils.executeByIo(object : SimpleTask<String?>() {
                        fun doInBackground(): String {
                            return@setOnClickListener showLoadingImage(downloadPath)
                        }

                        fun onSuccess(result: String) {
                            onSuccessful(result)
                        }
                    })
                } else {
                    // 有可能本地图片
                    try {
                        if (PictureMimeType.isContent(downloadPath)) {
                            savePictureAlbumAndroidQ(if (PictureMimeType.isContent(downloadPath)) Uri.parse(downloadPath) else Uri.fromFile(File(downloadPath)))
                        } else {
                            // 把文件插入到系统图库
                            savePictureAlbum()
                        }
                    } catch (e: Exception) {
                        ToastUtils.s(getContext(), getString(R.string.picture_save_error).toString() + "\n" + e.message)
                        dismissDialog()
                        e.printStackTrace()
                    }
                }
                if (!isFinishing()) {
                    dialog.dismiss()
                }
            }
            dialog.show()
        }
    }

    /**
     * 保存相片至本地相册
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun savePictureAlbum() {
        val suffix: String = PictureMimeType.getLastImgSuffix(mMimeType)
        val state = Environment.getExternalStorageState()
        val rootDir = if (state == Environment.MEDIA_MOUNTED) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) else getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (rootDir != null && !rootDir.exists() && rootDir.mkdirs()) {
        }
        val folderDir = File(if (SdkVersionUtils.checkedAndroid_Q() || state != Environment.MEDIA_MOUNTED) rootDir!!.absolutePath else rootDir!!.absolutePath + File.separator + PictureMimeType.CAMERA + File.separator)
        if (folderDir != null && !folderDir.exists() && folderDir.mkdirs()) {
        }
        val fileName: String = DateUtils.getCreateFileName("IMG_").toString() + suffix
        val file = File(folderDir, fileName)
        PictureFileUtils.copyFile(downloadPath, file.absolutePath)
        onSuccessful(file.absolutePath)
    }

    /**
     * 图片保存成功
     *
     * @param result
     */
    private fun onSuccessful(result: String) {
        dismissDialog()
        if (!TextUtils.isEmpty(result)) {
            try {
                if (!SdkVersionUtils.checkedAndroid_Q()) {
                    val file = File(result)
                    MediaStore.Images.Media.insertImage(getContentResolver(), file.absolutePath, file.name, null)
                    PictureMediaScannerConnection(getContext(), file.absolutePath) {}
                }
                ToastUtils.s(getContext(), getString(R.string.picture_save_success).toString() + "\n" + result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            ToastUtils.s(getContext(), getString(R.string.picture_save_error))
        }
    }

    /**
     * 保存图片到picture 目录，Android Q适配，最简单的做法就是保存到公共目录，不用SAF存储
     *
     * @param inputUri
     */
    private fun savePictureAlbumAndroidQ(inputUri: Uri) {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"))
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, ValueOf.toString(System.currentTimeMillis()))
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mMimeType)
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM)
        val uri: Uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            ToastUtils.s(getContext(), getString(R.string.picture_save_error))
            return
        }
        PictureThreadUtils.executeByIo(object : SimpleTask<String?>() {
            fun doInBackground(): String {
                var buffer: BufferedSource? = null
                try {
                    buffer = source(Objects.requireNonNull(getContentResolver().openInputStream(inputUri))).buffer()
                    val outputStream: OutputStream = getContentResolver().openOutputStream(uri)
                    val bufferCopy: Boolean = PictureFileUtils.bufferCopy(buffer, outputStream)
                    if (bufferCopy) {
                        return PictureFileUtils.getPath(getContext(), uri)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (buffer != null && buffer.isOpen()) {
                        PictureFileUtils.close(buffer)
                    }
                }
                return ""
            }

            fun onSuccess(result: String) {
                PictureThreadUtils.cancel(PictureThreadUtils.getIoPool())
                onSuccessful(result)
            }
        })
    }

    /**
     * 针对Q版本创建uri
     *
     * @return
     */
    private fun createOutImageUri(): Uri {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"))
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, ValueOf.toString(System.currentTimeMillis()))
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mMimeType)
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM)
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    // 下载图片保存至手机
    fun showLoadingImage(urlPath: String?): String? {
        var outImageUri: Uri? = null
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null
        var inBuffer: BufferedSource? = null
        try {
            if (SdkVersionUtils.checkedAndroid_Q()) {
                outImageUri = createOutImageUri()
            } else {
                val suffix: String = PictureMimeType.getLastImgSuffix(mMimeType)
                val state = Environment.getExternalStorageState()
                val rootDir = if (state == Environment.MEDIA_MOUNTED) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) else getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                if (rootDir != null) {
                    if (!rootDir.exists()) {
                        rootDir.mkdirs()
                    }
                    val folderDir = File(if (state != Environment.MEDIA_MOUNTED) rootDir.absolutePath else rootDir.absolutePath + File.separator + PictureMimeType.CAMERA + File.separator)
                    if (!folderDir.exists() && folderDir.mkdirs()) {
                    }
                    val fileName: String = DateUtils.getCreateFileName("IMG_").toString() + suffix
                    val file = File(folderDir, fileName)
                    outImageUri = Uri.fromFile(file)
                }
            }
            if (outImageUri != null) {
                outputStream = Objects.requireNonNull(getContentResolver().openOutputStream(outImageUri))
                val u = URL(urlPath)
                inputStream = u.openStream()
                inBuffer = inputStream.source().buffer()
                val bufferCopy: Boolean = PictureFileUtils.bufferCopy(inBuffer, outputStream)
                if (bufferCopy) {
                    return PictureFileUtils.getPath(this, outImageUri)
                }
            }
        } catch (e: Exception) {
            if (outImageUri != null && SdkVersionUtils.checkedAndroid_Q()) {
                getContentResolver().delete(outImageUri, null, null)
            }
        } finally {
            PictureFileUtils.close(inputStream)
            PictureFileUtils.close(outputStream)
            PictureFileUtils.close(inBuffer)
        }
        return null
    }

    fun onBackPressed() {
        super.onBackPressed()
        finish()
        exitAnimation()
    }

    private fun exitAnimation() {
        overridePendingTransition(R.anim.picture_anim_fade_in, if (config.windowAnimationStyle != null
                && config.windowAnimationStyle.activityPreviewExitAnimation !== 0) config.windowAnimationStyle.activityPreviewExitAnimation else R.anim.picture_anim_exit)
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (adapter != null) {
            adapter!!.clear()
        }
        if (PictureSelectionConfig.customVideoPlayCallback != null) {
            PictureSelectionConfig.customVideoPlayCallback = null
        }
        if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
            PictureSelectionConfig.onCustomCameraInterfaceListener = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE ->                 // 存储权限
            {
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        showDownLoadDialog()
                    } else {
                        ToastUtils.s(getContext(), getString(R.string.picture_jurisdiction))
                    }
                    i++
                }
            }
        }
    }
}