package com.luck.picture.lib.instagram

import android.Manifest
import android.net.Uri
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.luck.picture.lib.camera.listener.CameraListener
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author：luck
 * @data：2018/1/27 19:12
 * @描述: Media 选择页面
 */
class PictureSelectorInstagramStyleActivity : PictureBaseActivity(), View.OnClickListener, OnAlbumItemClickListener, InstagramImageGridAdapter.OnPhotoSelectChangedListener, OnItemClickListener {
    protected var mIvPictureLeftBack: ImageView? = null
    protected var mIvArrow: ImageView? = null
    protected var titleViewBg: View? = null
    protected var mTvPictureTitle: TextView? = null
    protected var mTvPictureRight: TextView? = null
    protected var mTvEmpty: TextView? = null
    protected var mTvPlayPause: TextView? = null
    protected var mTvStop: TextView? = null
    protected var mTvQuit: TextView? = null
    protected var mTvMusicStatus: TextView? = null
    protected var mTvMusicTotal: TextView? = null
    protected var mTvMusicTime: TextView? = null
    protected var mPictureRecycler: RecyclerView? = null
    protected var mAdapter: InstagramImageGridAdapter? = null
    protected var images: MutableList<LocalMedia>? = ArrayList<LocalMedia>()
    protected var foldersList: List<LocalMediaFolder> = ArrayList<LocalMediaFolder>()
    protected var folderWindow: FolderPopWindow? = null
    protected var mediaPlayer: MediaPlayer? = null
    protected var musicSeekBar: SeekBar? = null
    protected var isPlayAudio = false
    protected var audioDialog: PictureCustomDialog? = null
    protected var oldCurrentListSize = 0
    protected var isFirstEnterActivity = false
    protected var isEnterSetting = false
    protected var mInstagramGallery: InstagramGallery? = null
    private var mPreviewContainer: InstagramPreviewContainer? = null
    private var mPreviewPosition = -1
    private var mInstagramViewPager: InstagramViewPager? = null
    private var isRunningBind = false
    private var mTitle: String? = null
    private var mList: MutableList<Page>? = null
    private var intervalClickTime: Long = 0
    private var mLruCache: LruCache<LocalMedia, AsyncTask<*, *, *>>? = null
    private var isCroppingImage = false
    private var mFolderPosition = 0
    private var mPreviousFolderPosition = 0
    private var isChangeFolder = false
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            oldCurrentListSize = savedInstanceState.getInt(PictureConfig.EXTRA_OLD_CURRENT_LIST_SIZE, 0)
            // 防止拍照内存不足时activity被回收，导致拍照后的图片未选中
            selectionMedias = PictureSelector.obtainSelectorList(savedInstanceState)
            if (mAdapter != null) {
                mAdapter.bindSelectImages(selectionMedias)
            }
        }
    }

    protected override fun onResume() {
        super.onResume()
        if (mInstagramViewPager!!.getSelectedPosition() === 0 && mPreviewContainer != null) {
            mPreviewContainer.onResume()
        }
        if (mInstagramViewPager != null) {
            mInstagramViewPager.onResume()
        }
        // 这里只针对权限被手动拒绝后进入设置页面重新获取权限后的操作
        if (isEnterSetting) {
            if (PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (mAdapter!!.isDataEmpty()) {
                    readLocalMedia()
                }
            } else {
                showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
            }
            isEnterSetting = false
        }
        if (mInstagramViewPager != null) {
            if (mInstagramViewPager.getSelectedPosition() === 1 || mInstagramViewPager.getSelectedPosition() === 2) {
                if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                    initCamera()
                }
            }
        }
    }

    protected override fun onPause() {
        super.onPause()
        if (mPreviewContainer != null) {
            mPreviewContainer.onPause()
        }
        if (mInstagramViewPager != null) {
            mInstagramViewPager.onPause()
        }
    }

    override fun getResourceId(): Int {
        return R.layout.picture_instagram_style_selector
    }

    protected override fun initWidgets() {
        super.initWidgets()
        container = findViewById<View>(R.id.container)
        titleViewBg = findViewById<View>(R.id.titleViewBg)
        mIvPictureLeftBack = findViewById<ImageView>(R.id.pictureLeftBack)
        mTvPictureTitle = findViewById<TextView>(R.id.picture_title)
        mTvPictureRight = findViewById<TextView>(R.id.picture_right)
        mIvArrow = findViewById<ImageView>(R.id.ivArrow)
        config.isCamera = false
        config.selectionMode = PictureConfig.SINGLE
        config.isSingleDirectReturn = true
        config.isWithVideoImage = false
        config.maxVideoSelectNum = 1
        config.aspect_ratio_x = 1
        config.aspect_ratio_y = 1
        config.enableCrop = true
        //        config.recordVideoMinSecond = 3;
        mPictureRecycler = GalleryViewImpl(context)
        mPreviewContainer = InstagramPreviewContainer(context, config)
        mInstagramGallery = InstagramGallery(context, mPreviewContainer, mPictureRecycler)
        mInstagramGallery.setPreviewBottomMargin(ScreenUtils.dip2px(context, 2))
        mPreviewContainer.setListener(object : onSelectionModeChangedListener() {
            fun onSelectionModeChange(isMulti: Boolean) {
                if (isMulti) {
                    config.selectionMode = PictureConfig.MULTIPLE
                    config.isSingleDirectReturn = false
                    if (mInstagramViewPager != null) {
                        mInstagramGallery.setInitGalleryHeight()
                        mInstagramViewPager.setScrollEnable(false)
                        mInstagramViewPager.displayTabLayout(false)
                    }
                    if (mLruCache == null) {
                        mLruCache = LruCache(20)
                    }
                    bindPreviewPosition()
                } else {
                    config.selectionMode = PictureConfig.SINGLE
                    config.isSingleDirectReturn = true
                    if (mInstagramViewPager != null) {
                        mInstagramGallery.setInitGalleryHeight()
                        mInstagramViewPager.setScrollEnable(true)
                        mInstagramViewPager.displayTabLayout(true)
                    }
                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged()
                }
            }

            fun onRatioChange(isOneToOne: Boolean) {
                if (isOneToOne) {
                    config.aspect_ratio_x = 0
                    config.aspect_ratio_y = 0
                } else {
                    config.aspect_ratio_x = 1
                    config.aspect_ratio_y = 1
                }
            }
        })
        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        params.addRule(RelativeLayout.BELOW, R.id.titleViewBg)
        mList = ArrayList<Page>()
        mList.add(PageGallery(mInstagramGallery))
        val pagePhoto = PagePhoto(this, config)
        mList.add(pagePhoto)
        mList.add(PageVideo(pagePhoto))
        mInstagramViewPager = InstagramViewPager(context, mList, config)
        (container as RelativeLayout).addView(mInstagramViewPager, params)
        pagePhoto.setCameraListener(object : CameraListener() {
            override fun onPictureSuccess(file: File) {
                val intent = Intent()
                intent.putExtra(PictureConfig.EXTRA_MEDIA_PATH, file.absolutePath)
                requestCamera(intent)
            }

            override fun onRecordSuccess(file: File) {
                val intent = Intent()
                intent.putExtra(PictureConfig.EXTRA_MEDIA_PATH, file.absolutePath)
                requestCamera(intent)
            }

            override fun onError(videoCaptureError: Int, message: String?, cause: Throwable?) {
                if (videoCaptureError == -1) {
                    onTakePhoto()
                } else {
                    ToastUtils.s(context, message)
                }
            }
        })
        mInstagramViewPager.setSkipRange(1)
        mInstagramViewPager.setOnPageChangeListener(object : OnPageChangeListener() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (position == 0) {
                    if (positionOffset >= 0.5f) {
                        mPreviewContainer.pauseVideo(true)
                    } else {
                        mPreviewContainer.pauseVideo(false)
                    }
                }
                if (isRunningBind) {
                    mHandler.removeCallbacks(mBindRunnable)
                    isRunningBind = false
                }
                if (position == 1) {
                    (mList.get(1) as PagePhoto).setCaptureButtonTranslationX((-positionOffsetPixels).toFloat())
                } else if (position == 2 && positionOffsetPixels == 0) {
                    (mList.get(1) as PagePhoto).setCaptureButtonTranslationX(-mInstagramViewPager.getMeasuredWidth())
                }
            }

            override fun onPageSelected(position: Int) {
                if (position == 1 || position == 2) {
                    onTakePhoto()
                }
                changeTabState(position)
                if (position == 1) {
                    (mList.get(1) as PagePhoto).setCameraState(InstagramCameraView.STATE_CAPTURE)
                } else if (position == 2) {
                    (mList.get(1) as PagePhoto).setCameraState(InstagramCameraView.STATE_RECORDER)
                }
            }
        })
        mTvEmpty = mInstagramGallery.getEmptyView()
        isNumComplete(numComplete)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DEFAULT) {
            mIvPictureLeftBack.setColorFilter(PorterDuffColorFilter(ContextCompat.getColor(context, R.color.picture_color_black), PorterDuff.Mode.MULTIPLY))
            mIvArrow.setColorFilter(PorterDuffColorFilter(ContextCompat.getColor(context, R.color.picture_color_black), PorterDuff.Mode.MULTIPLY))
        }
        if (config.isAutomaticTitleRecyclerTop) {
            titleViewBg!!.setOnClickListener(this)
        }
        mIvPictureLeftBack!!.setOnClickListener(this)
        mTvPictureRight.setOnClickListener(this)
        mTvPictureTitle.setOnClickListener(this)
        mIvArrow!!.setOnClickListener(this)
        mTitle = if (config.chooseMode === PictureMimeType.ofAudio()) getString(R.string.picture_all_audio) else getString(R.string.picture_camera_roll)
        mTvPictureTitle.setText(mTitle)
        folderWindow = FolderPopWindow(this, config)
        folderWindow.setArrowImageView(mIvArrow)
        folderWindow.setOnAlbumItemClickListener(this)
        mPictureRecycler.setHasFixedSize(true)
        mPictureRecycler.addItemDecoration(SpacingItemDecoration(config.imageSpanCount,
                ScreenUtils.dip2px(this, 2), false))
        mPictureRecycler.setLayoutManager(GridLayoutManager(context, config.imageSpanCount))
        // 解决调用 notifyItemChanged 闪烁问题,取消默认动画
        (mPictureRecycler.getItemAnimator() as SimpleItemAnimator)
                .setSupportsChangeAnimations(false)
        if (config.isFallbackVersion2
                || Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            loadAllMediaData()
        }
        mTvEmpty.setText(if (config.chooseMode === PictureMimeType.ofAudio()) getString(R.string.picture_audio_empty) else getString(R.string.picture_empty))
        StringUtils.tempTextFont(mTvEmpty, config.chooseMode)
        mAdapter = InstagramImageGridAdapter(context, config)
        mAdapter.setOnPhotoSelectChangedListener(this)
        mPictureRecycler.setAdapter(mAdapter)
    }

    private fun bindPreviewPosition() {
        if (mAdapter != null) {
            val selectedImages: MutableList<LocalMedia> = mAdapter.getSelectedImages()
            val size = selectedImages.size
            val mimeType = if (size > 0) selectedImages[0].getMimeType() else ""
            val previewMedia: LocalMedia = mAdapter.getImages().get(mPreviewPosition)
            if (selectedImages.contains(previewMedia) || containsMedia(selectedImages, previewMedia)) {
                return
            }
            if (!TextUtils.isEmpty(mimeType)) {
                val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, previewMedia.getMimeType())
                if (!mimeTypeSame) {
                    ToastUtils.s(context, getString(R.string.picture_rule))
                    return
                }
            }
            if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) {
                if (size >= config.maxVideoSelectNum) {
                    // 如果先选择的是视频
                    ToastUtils.s(context, StringUtils.getMsg(context, mimeType, config.maxVideoSelectNum))
                    return
                }
                if (config.videoMinSecond > 0 && previewMedia.getDuration() < config.videoMinSecond) {
                    // 视频小于最低指定的长度
                    ToastUtils.s(context,
                            context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                    return
                }
                if (config.videoMaxSecond > 0 && previewMedia.getDuration() > config.videoMaxSecond) {
                    // 视频时长超过了指定的长度
                    ToastUtils.s(context,
                            context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                    return
                }
            } else {
                if (size >= config.maxSelectNum) {
                    ToastUtils.s(context, StringUtils.getMsg(context, mimeType, config.maxSelectNum))
                    return
                }
                if (PictureMimeType.isHasVideo(previewMedia.getMimeType())) {
                    if (config.videoMinSecond > 0 && previewMedia.getDuration() < config.videoMinSecond) {
                        // 视频小于最低指定的长度
                        ToastUtils.s(context,
                                context.getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                        return
                    }
                    if (config.videoMaxSecond > 0 && previewMedia.getDuration() > config.videoMaxSecond) {
                        // 视频时长超过了指定的长度
                        ToastUtils.s(context,
                                context.getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                        return
                    }
                }
            }
            selectedImages.add(previewMedia)
            mAdapter.bindSelectImages(selectedImages)
        }
    }

    fun containsMedia(selectedImages: List<LocalMedia>?, media: LocalMedia?): Boolean {
        if (selectedImages != null && media != null) {
            for (selectedImage in selectedImages) {
                if (selectedImage.getPath().equals(media.getPath()) || selectedImage.getId() === media.getId()) {
                    return true
                }
            }
        }
        return false
    }

    private fun changeTabState(position: Int) {
        val title: String?
        val enable: Boolean
        if (position == 1) {
            title = getString(R.string.photo)
            enable = false
        } else if (position == 2) {
            title = getString(R.string.video)
            enable = false
        } else {
            title = mTitle
            enable = true
        }
        if (enable) {
            mIvArrow!!.visibility = View.VISIBLE
            mTvPictureRight.setVisibility(View.VISIBLE)
        } else {
            mIvArrow!!.visibility = View.INVISIBLE
            mTvPictureRight.setVisibility(View.INVISIBLE)
        }
        mTvPictureTitle.setEnabled(enable)
        mTvPictureTitle.setText(title)
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            if (!config.isFallbackVersion2) {
                if (!isFirstEnterActivity) {
                    loadAllMediaData()
                    isFirstEnterActivity = true
                }
            }
        }
    }

    /**
     * 加载数据
     */
    private fun loadAllMediaData() {
        if (PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            readLocalMedia()
        } else {
            PermissionChecker.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE)
        }
    }

    /**
     * 动态设置相册主题
     */
    override fun initPictureSelectorStyle() {
        if (config.style != null) {
            if (config.style.pictureTitleDownResId !== 0) {
                val drawable: Drawable = ContextCompat.getDrawable(this, config.style.pictureTitleDownResId)
                mIvArrow!!.setImageDrawable(drawable)
            }
            if (config.style.pictureTitleTextColor !== 0) {
                mTvPictureTitle.setTextColor(config.style.pictureTitleTextColor)
            }
            if (config.style.pictureTitleTextSize !== 0) {
                mTvPictureTitle.setTextSize(config.style.pictureTitleTextSize)
            }
            if (config.style.pictureRightDefaultTextColor !== 0) {
                mTvPictureRight.setTextColor(config.style.pictureRightDefaultTextColor)
            } else {
                if (config.style.pictureCancelTextColor !== 0) {
                    mTvPictureRight.setTextColor(config.style.pictureCancelTextColor)
                }
            }
            if (config.style.pictureRightTextSize !== 0) {
                mTvPictureRight.setTextSize(config.style.pictureRightTextSize)
            }
            if (config.style.pictureLeftBackIcon !== 0) {
                mIvPictureLeftBack!!.setImageResource(config.style.pictureLeftBackIcon)
            }
            if (config.style.pictureContainerBackgroundColor !== 0) {
                container.setBackgroundColor(config.style.pictureContainerBackgroundColor)
            }
            if (!TextUtils.isEmpty(config.style.pictureRightDefaultText)) {
                mTvPictureRight.setText(config.style.pictureRightDefaultText)
            }
        } else {
            if (config.downResId !== 0) {
                val drawable: Drawable = ContextCompat.getDrawable(this, config.downResId)
                mIvArrow!!.setImageDrawable(drawable)
            }
        }
        titleViewBg!!.setBackgroundColor(colorPrimary)
        mAdapter!!.bindSelectImages(selectionMedias)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (images != null) {
            // 保存当前列表中图片或视频个数
            outState.putInt(PictureConfig.EXTRA_OLD_CURRENT_LIST_SIZE, images!!.size)
        }
        if (mAdapter != null && mAdapter.getSelectedImages() != null) {
            val selectedImages: List<LocalMedia> = mAdapter.getSelectedImages()
            PictureSelector.saveSelectorList(outState, selectedImages)
        }
    }

    /**
     * none number style
     */
    private fun isNumComplete(numComplete: Boolean) {
        if (numComplete) {
            initCompleteText(0)
        }
    }

    /**
     * init 完成文案
     */
    protected override fun initCompleteText(startCount: Int) {}

    /**
     * get LocalMedia s
     */
    protected fun readLocalMedia() {
        showPleaseDialog()
        PictureThreadUtils.executeByCached(object : SimpleTask<List<LocalMediaFolder?>?>() {
            fun doInBackground(): List<LocalMediaFolder> {
                return LocalMediaLoader(context, config).loadAllMedia()
            }

            fun onSuccess(folders: List<LocalMediaFolder>?) {
                dismissDialog()
                PictureThreadUtils.cancel(PictureThreadUtils.getCachedPool())
                if (folders != null) {
                    if (folders.size > 0) {
                        foldersList = folders
                        val folder: LocalMediaFolder = folders[0]
                        folder.setChecked(true)
                        val result: MutableList<LocalMedia> = folder.getData()
                        if (images == null) {
                            images = ArrayList<LocalMedia>()
                        }
                        // 这里解决有些机型会出现拍照完，相册列表不及时刷新问题
                        // 因为onActivityResult里手动添加拍照后的照片，
                        // 如果查询出来的图片大于或等于当前adapter集合的图片则取更新后的，否则就取本地的
                        val currentSize = images!!.size
                        val resultSize = result.size
                        oldCurrentListSize = oldCurrentListSize + currentSize
                        if (resultSize >= currentSize) {
                            if (currentSize > 0 && currentSize < resultSize && oldCurrentListSize != resultSize) {
                                // 这种情况多数是由于拍照导致Activity和数据被回收数据不一致
                                images!!.addAll(result)
                                // 更新相机胶卷目录
                                val media: LocalMedia = images!![0]
                                folder.setFirstImagePath(media.getPath())
                                folder.getData().add(0, media)
                                folder.setCheckedNum(1)
                                folder.setImageNum(folder.getImageNum() + 1)
                                // 更新相片所属目录
                                updateMediaFolder(foldersList, media)
                            } else {
                                // 正常情况下
                                images = result
                            }
                            folderWindow.bindFolder(folders)
                        }
                    }
                    if (mAdapter != null) {
                        mAdapter.bindImagesData(images)
                        val isEmpty = images!!.size > 0
                        if (!isEmpty) {
                            mTvEmpty.setText(getString(R.string.picture_empty))
                            mTvEmpty.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.picture_icon_no_data, 0, 0)
                        } else {
                            //默认预览第一张图片
                            startPreview(images, 0)
                        }
                        mTvEmpty.setVisibility(if (isEmpty) View.INVISIBLE else View.VISIBLE)
                        mInstagramGallery!!.setViewVisibility(if (isEmpty) View.VISIBLE else View.INVISIBLE)
                        val enabled = isEmpty || config.returnEmpty
                        mTvPictureRight.setEnabled(enabled)
                        mTvPictureRight.setTextColor(if (enabled) config.style.pictureRightDefaultTextColor else ContextCompat.getColor(context, R.color.picture_color_9B9B9D))
                    }
                } else {
                    mTvEmpty.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.picture_icon_data_error, 0, 0)
                    mTvEmpty.setText(getString(R.string.picture_data_exception))
                    mTvEmpty.setVisibility(if (images!!.size > 0) View.INVISIBLE else View.VISIBLE)
                    mInstagramGallery!!.setViewVisibility(if (images!!.size > 0) View.VISIBLE else View.INVISIBLE)
                    val enabled = images!!.size > 0 || config.returnEmpty
                    mTvPictureRight.setEnabled(enabled)
                    mTvPictureRight.setTextColor(if (enabled) config.style.pictureRightDefaultTextColor else ContextCompat.getColor(context, R.color.picture_color_9B9B9D))
                }
            }
        })
    }

    /**
     * open camera
     */
    fun startCamera() {
        // 防止快速点击，但是单独拍照不管
        if (!DoubleUtils.isFastDoubleClick()) {
            if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                // 用户需要自定义拍照处理
                if (config.chooseMode === PictureConfig.TYPE_ALL) {
                    // 如果是全部类型下，单独拍照就默认图片 (因为单独拍照不会new此PopupWindow对象)
                    val selectedDialog: PhotoItemSelectedDialog = PhotoItemSelectedDialog.newInstance()
                    selectedDialog.setOnItemClickListener(this)
                    selectedDialog.show(getSupportFragmentManager(), "PhotoItemSelectedDialog")
                } else {
                    PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(context, config, config.chooseMode)
                }
                return
            }
            if (config.isUseCustomCamera) {
                startCustomCamera()
                return
            }
            when (config.chooseMode) {
                PictureConfig.TYPE_ALL -> {
                    // 如果是全部类型下，单独拍照就默认图片 (因为单独拍照不会new此PopupWindow对象)
                    val selectedDialog: PhotoItemSelectedDialog = PhotoItemSelectedDialog.newInstance()
                    selectedDialog.setOnItemClickListener(this)
                    selectedDialog.show(getSupportFragmentManager(), "PhotoItemSelectedDialog")
                }
                PictureConfig.TYPE_IMAGE ->                     // 拍照
                    startOpenCamera()
                PictureConfig.TYPE_VIDEO ->                     // 录视频
                    startOpenCameraVideo()
                PictureConfig.TYPE_AUDIO ->                     // 录音
                    startOpenCameraAudio()
                else -> {
                }
            }
        }
    }

    /**
     * 启动自定义相机
     */
    private fun startCustomCamera() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            val intent = Intent(this, PictureCustomCameraActivity::class.java)
            startActivityForResult(intent, PictureConfig.REQUEST_CAMERA)
            val windowAnimationStyle: PictureWindowAnimationStyle = config.windowAnimationStyle
            overridePendingTransition(if (windowAnimationStyle != null &&
                    windowAnimationStyle.activityEnterAnimation !== 0) windowAnimationStyle.activityEnterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
        } else {
            PermissionChecker
                    .requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE)
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.pictureLeftBack) {
            if (folderWindow != null && folderWindow.isShowing()) {
                folderWindow.dismiss()
            } else {
                onBackPressed()
            }
        } else if (id == R.id.picture_title || id == R.id.ivArrow) {
            if (folderWindow.isShowing()) {
                folderWindow.dismiss()
            } else {
                if (images != null && images!!.size > 0) {
                    folderWindow.showAsDropDown(titleViewBg)
                    if (!config.isSingleDirectReturn) {
                        val selectedImages: List<LocalMedia> = mAdapter!!.getSelectedImages()
                        folderWindow.updateFolderCheckStatus(selectedImages)
                    }
                }
            }
        } else if (id == R.id.picture_right) {
            onComplete()
        } else if (id == R.id.titleViewBg) {
            if (mInstagramViewPager!!.getSelectedPosition() === 0 && config.isAutomaticTitleRecyclerTop) {
                val intervalTime = 500
                if (SystemClock.uptimeMillis() - intervalClickTime < intervalTime) {
                    if (mAdapter!!.getItemCount() > 0) {
                        mPictureRecycler.smoothScrollToPosition(0)
                    }
                } else {
                    intervalClickTime = SystemClock.uptimeMillis()
                }
            }
        }
    }

    /**
     * 完成选择
     */
    private fun onComplete() {
        val result: MutableList<LocalMedia> = mAdapter!!.getSelectedImages()
        if (config.selectionMode === PictureConfig.SINGLE) {
            if (result.size > 0) {
                result.clear()
            }
            if (mAdapter!!.getImages().size() > 0) {
                result.add(mAdapter!!.getImages().get(mPreviewPosition))
            }
        }
        val size = result.size
        val image: LocalMedia? = if (result.size > 0) result[0] else null
        val mimeType = if (image != null) image.getMimeType() else ""
        // 如果设置了图片最小选择数量，则判断是否满足条件
        val eqImg: Boolean = PictureMimeType.isHasImage(mimeType)
        if (config.isWithVideoImage) {
            // 混选模式
            var videoSize = 0
            var imageSize = 0
            for (i in 0 until size) {
                val media: LocalMedia = result[i]
                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    videoSize++
                } else {
                    imageSize++
                }
            }
            if (config.selectionMode === PictureConfig.MULTIPLE) {
                if (config.minSelectNum > 0) {
                    if (imageSize < config.minSelectNum) {
                        ToastUtils.s(context, getString(R.string.picture_min_img_num, config.minSelectNum))
                        return
                    }
                }
                if (config.minVideoSelectNum > 0) {
                    if (videoSize < config.minVideoSelectNum) {
                        ToastUtils.s(context, getString(R.string.picture_min_video_num, config.minVideoSelectNum))
                        return
                    }
                }
            }
        } else {
            if (config.selectionMode === PictureConfig.MULTIPLE) {
                if (PictureMimeType.isHasImage(mimeType) && config.minSelectNum > 0 && size < config.minSelectNum) {
                    val str: String = getString(R.string.picture_min_img_num, config.minSelectNum)
                    ToastUtils.s(context, str)
                    return
                }
                if (PictureMimeType.isHasVideo(mimeType) && config.minVideoSelectNum > 0 && size < config.minVideoSelectNum) {
                    val str: String = getString(R.string.picture_min_video_num, config.minVideoSelectNum)
                    ToastUtils.s(context, str)
                    return
                }
            }
        }

        // 如果没选并且设置了可以空返回则直接回到结果页
        if (size == 0) {
            if (config.returnEmpty) {
                if (PictureSelectionConfig.listener != null) {
                    PictureSelectionConfig.listener.onResult(result)
                } else {
                    val intent: Intent = PictureSelector.putIntentResult(result)
                    setResult(Activity.RESULT_OK, intent)
                }
                closeActivity()
                return
            }
            if (config.minSelectNum > 0 && size < config.minSelectNum) {
                val str: String = getString(R.string.picture_min_img_num, config.minSelectNum)
                ToastUtils.s(context, str)
                return
            }
            if (config.minVideoSelectNum > 0 && size < config.minVideoSelectNum) {
                val str: String = getString(R.string.picture_min_video_num, config.minVideoSelectNum)
                ToastUtils.s(context, str)
                return
            }
        }
        if (config.isCheckOriginalImage) {
            onResult(result)
            return
        }
        if (config.chooseMode === PictureMimeType.ofAll() && config.isWithVideoImage) {
            // 视频和图片可以同选
            bothMimeTypeWith(eqImg, result)
        } else {
            // 单一类型
            separateMimeTypeWith(eqImg, result)
        }
    }

    /**
     * 两者不同类型的处理方式
     *
     * @param eqImg
     * @param images
     */
    private fun bothMimeTypeWith(eqImg: Boolean, images: List<LocalMedia>?) {
        val image: LocalMedia? = if (images!!.size > 0) images[0] else null
        if (config.enableCrop) {
            if (config.selectionMode === PictureConfig.SINGLE && eqImg) {
                config.originalPath = image.getPath()
                startCrop(config.originalPath, image.getMimeType())
            } else {
                // 是图片和选择压缩并且是多张，调用批量压缩
                val cuts: ArrayList<CutInfo> = ArrayList<CutInfo>()
                val count = images.size
                var imageNum = 0
                for (i in 0 until count) {
                    val media: LocalMedia = images[i]
                    if (media == null
                            || TextUtils.isEmpty(media.getPath())) {
                        continue
                    }
                    val isHasImage: Boolean = PictureMimeType.isHasImage(media.getMimeType())
                    if (isHasImage) {
                        imageNum++
                    }
                    val cutInfo = CutInfo()
                    cutInfo.setId(media.getId())
                    cutInfo.setPath(media.getPath())
                    cutInfo.setImageWidth(media.getWidth())
                    cutInfo.setImageHeight(media.getHeight())
                    cutInfo.setMimeType(media.getMimeType())
                    cutInfo.setDuration(media.getDuration())
                    cutInfo.setRealPath(media.getRealPath())
                    cuts.add(cutInfo)
                }
                if (imageNum <= 0) {
                    // 全是视频
                    onResult(images)
                } else {
                    // 图片和视频共存
                    startCrop(cuts)
                }
            }
        } else if (config.isCompress) {
            val size = images.size
            var imageNum = 0
            for (i in 0 until size) {
                val media: LocalMedia = images[i]
                val isHasImage: Boolean = PictureMimeType.isHasImage(media.getMimeType())
                if (isHasImage) {
                    imageNum++
                    break
                }
            }
            if (imageNum <= 0) {
                // 全是视频不压缩
                onResult(images)
            } else {
                // 图片才压缩
                compressImage(images)
            }
        } else {
            onResult(images)
        }
    }

    /**
     * 同一类型的图片或视频处理逻辑
     *
     * @param eqImg
     * @param images
     */
    private fun separateMimeTypeWith(eqImg: Boolean, images: List<LocalMedia>?) {
        val image: LocalMedia? = if (images!!.size > 0) images[0] else null
        val mimeType = if (image != null) image.getMimeType() else ""
        if (config.enableCrop && eqImg) {
            if (config.selectionMode === PictureConfig.SINGLE) {
                if (mPreviewContainer != null) {
                    mPreviewContainer.cropAndSaveImage(this)
                }
            } else {
                // 是图片和选择压缩并且是多张，调用批量压缩
//                ArrayList<CutInfo> cuts = new ArrayList<>();
//                int count = images.size();
//                for (int i = 0; i < count; i++) {
//                    LocalMedia media = images.get(i);
//                    if (media == null
//                            || TextUtils.isEmpty(media.getPath())) {
//                        continue;
//                    }
//                    CutInfo cutInfo = new CutInfo();
//                    cutInfo.setId(media.getId());
//                    cutInfo.setPath(media.getPath());
//                    cutInfo.setImageWidth(media.getWidth());
//                    cutInfo.setImageHeight(media.getHeight());
//                    cutInfo.setMimeType(media.getMimeType());
//                    cutInfo.setDuration(media.getDuration());
//                    cutInfo.setRealPath(media.getRealPath());
//                    cuts.add(cutInfo);
//                }
//                startCrop(cuts);
                savePreviousPositionCropInfo(mAdapter!!.getImages().get(mPreviewPosition))
                startMultiCrop()
            }
        } else if (config.isCompress
                && eqImg) {
            // 图片才压缩，视频不管
            compressImage(images)
        } else if (PictureMimeType.isHasVideo(mimeType)) {
            val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
            result.addAll(images)
            var bundle: Bundle? = null
            if (mPreviewContainer != null) {
                bundle = Bundle()
                bundle.putBoolean(InstagramMediaProcessActivity.EXTRA_ASPECT_RATIO, mPreviewContainer.isAspectRatio())
            }
            InstagramMediaProcessActivity.launchActivity(this, config, result, bundle, InstagramMediaProcessActivity.REQUEST_SINGLE_VIDEO_PROCESS)
        } else {
            onResult(images)
        }
    }
    //    private void cropVideo(List<LocalMedia> images) {
    //        if (images.isEmpty()) {
    //            return;
    //        }
    //        LocalMedia media = images.get(0);
    //        File transcodeOutputFile;
    //        try {
    //            File outputDir = new File(getExternalFilesDir(null), "outputs");
    //            //noinspection ResultOfMethodCallIgnored
    //            outputDir.mkdir();
    //            transcodeOutputFile = File.createTempFile("transcode_" + media.getId(), ".mp4", outputDir);
    //        } catch (IOException e) {
    //            ToastUtils.s(this, "Failed to create temporary file.");
    //            return;
    //        }
    //
    //        showPleaseDialog();
    //
    //        Resizer resizer = new PassThroughResizer();
    //        if (mPreviewContainer != null) {
    //            if (mPreviewContainer.isAspectRatio() && mPreviewContainer.getAspectRadio() > 0) {
    //                resizer = new AspectRatioResizer(mPreviewContainer.getAspectRadio());
    //            } else if (!mPreviewContainer.isAspectRatio()) {
    //                resizer = new AspectRatioResizer(1f);
    //            }
    //        }
    //        TrackStrategy videoStrategy = new DefaultVideoStrategy.Builder()
    //                .addResizer(resizer)
    //                .build();
    //
    //        DataSink sink = new DefaultDataSink(transcodeOutputFile.getAbsolutePath());
    //        TranscoderOptions.Builder builder = Transcoder.into(sink);
    //        if (PictureMimeType.isContent(media.getPath())) {
    //            builder.addDataSource(getContext(), Uri.parse(media.getPath()));
    //        } else {
    //            builder.addDataSource(media.getPath());
    //        }
    //        builder.setListener(new TranscoderListener() {
    //            @Override
    //            public void onTranscodeProgress(double progress) {
    //
    //            }
    //
    //            @Override
    //            public void onTranscodeCompleted(int successCode) {
    //                if (successCode == Transcoder.SUCCESS_TRANSCODED) {
    //                    File file = transcodeOutputFile;
    //                    String type = "video/mp4";
    //                    Uri uri = FileProvider.getUriForFile(PictureSelectorInstagramStyleActivity.this,
    //                            "com.luck.pictureselector.provider", file);
    //                    startActivity(new Intent(Intent.ACTION_VIEW)
    //                            .setDataAndType(uri, type)
    //                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    //                } else if (successCode == Transcoder.SUCCESS_NOT_NEEDED) {
    //
    //                }
    //                dismissDialog();
    //            }
    //
    //            @Override
    //            public void onTranscodeCanceled() {
    //                dismissDialog();
    //            }
    //
    //            @Override
    //            public void onTranscodeFailed(@NonNull Throwable exception) {
    //                dismissDialog();
    //            }
    //        })
    //                .setVideoTrackStrategy(videoStrategy)
    //                .transcode();
    //    }
    /**
     * 播放音频
     *
     * @param path
     */
    private fun audioDialog(path: String) {
        if (!isFinishing()) {
            audioDialog = PictureCustomDialog(context, R.layout.picture_audio_dialog)
            audioDialog.getWindow().setWindowAnimations(R.style.Picture_Theme_Dialog_AudioStyle)
            mTvMusicStatus = audioDialog.findViewById(R.id.tv_musicStatus)
            mTvMusicTime = audioDialog.findViewById(R.id.tv_musicTime)
            musicSeekBar = audioDialog.findViewById(R.id.musicSeekBar)
            mTvMusicTotal = audioDialog.findViewById(R.id.tv_musicTotal)
            mTvPlayPause = audioDialog.findViewById(R.id.tv_PlayPause)
            mTvStop = audioDialog.findViewById(R.id.tv_Stop)
            mTvQuit = audioDialog.findViewById(R.id.tv_Quit)
            if (mHandler != null) {
                mHandler.postDelayed(Runnable { initPlayer(path) }, 30)
            }
            mTvPlayPause.setOnClickListener(audioOnClick(path))
            mTvStop.setOnClickListener(audioOnClick(path))
            mTvQuit.setOnClickListener(audioOnClick(path))
            musicSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            audioDialog.setOnDismissListener { dialog ->
                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable)
                }
                Handler().postDelayed({ stop(path) }, 30)
                try {
                    if (audioDialog != null
                            && audioDialog.isShowing()) {
                        audioDialog.dismiss()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (mHandler != null) {
                mHandler.post(mRunnable)
            }
            audioDialog.show()
        }
    }

    //  通过 Handler 更新 UI 上的组件状态
    var mRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer != null) {
                    mTvMusicTime.setText(DateUtils.formatDurationTime(mediaPlayer.getCurrentPosition()))
                    musicSeekBar.setProgress(mediaPlayer.getCurrentPosition())
                    musicSeekBar.setMax(mediaPlayer.getDuration())
                    mTvMusicTotal.setText(DateUtils.formatDurationTime(mediaPlayer.getDuration()))
                    if (mHandler != null) {
                        mHandler.postDelayed(this, 200)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var mBindRunnable = Runnable {
        (mList!![1] as PagePhoto).bindToLifecycle()
        isRunningBind = false
    }

    /**
     * 初始化音频播放组件
     *
     * @param path
     */
    private fun initPlayer(path: String) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            mediaPlayer.setLooping(true)
            playAudio()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 播放音频点击事件
     */
    inner class audioOnClick(private val path: String) : View.OnClickListener {
        override fun onClick(v: View) {
            val id = v.id
            if (id == R.id.tv_PlayPause) {
                playAudio()
            }
            if (id == R.id.tv_Stop) {
                mTvMusicStatus.setText(getString(R.string.picture_stop_audio))
                mTvPlayPause.setText(getString(R.string.picture_play_audio))
                stop(path)
            }
            if (id == R.id.tv_Quit) {
                if (mHandler != null) {
                    mHandler.postDelayed(Runnable { stop(path) }, 30)
                    try {
                        if (audioDialog != null
                                && audioDialog.isShowing()) {
                            audioDialog.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    mHandler.removeCallbacks(mRunnable)
                }
            }
        }
    }

    /**
     * 播放音频
     */
    private fun playAudio() {
        if (mediaPlayer != null) {
            musicSeekBar.setProgress(mediaPlayer.getCurrentPosition())
            musicSeekBar.setMax(mediaPlayer.getDuration())
        }
        val ppStr: String = mTvPlayPause.getText().toString()
        if (ppStr == getString(R.string.picture_play_audio)) {
            mTvPlayPause.setText(getString(R.string.picture_pause_audio))
            mTvMusicStatus.setText(getString(R.string.picture_play_audio))
            playOrPause()
        } else {
            mTvPlayPause.setText(getString(R.string.picture_play_audio))
            mTvMusicStatus.setText(getString(R.string.picture_pause_audio))
            playOrPause()
        }
        if (isPlayAudio == false) {
            if (mHandler != null) {
                mHandler.post(mRunnable)
            }
            isPlayAudio = true
        }
    }

    /**
     * 停止播放
     *
     * @param path
     */
    fun stop(path: String?) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepare()
                mediaPlayer.seekTo(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 暂停播放
     */
    fun playOrPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause()
                } else {
                    mediaPlayer.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onItemClick(position: Int, isCameraFolder: Boolean, bucketId: Long, folderName: String?, images: List<LocalMedia>) {
        mPreviousFolderPosition = mFolderPosition
        mFolderPosition = position
        isChangeFolder = if (mPreviousFolderPosition != mFolderPosition) {
            true
        } else {
            false
        }
        val camera = config.isCamera && isCameraFolder
        mAdapter!!.setShowCamera(camera)
        mTitle = folderName
        mTvPictureTitle.setText(folderName)
        folderWindow.dismiss()
        mAdapter!!.bindImagesData(images)
        mPictureRecycler.smoothScrollToPosition(0)
        if (!images.isEmpty()) {
            //默认预览第一张图片
            startPreview(images, 0)
        }
    }

    override fun onItemChecked(position: Int, image: LocalMedia, isCheck: Boolean) {
        if (isCheck) {
            val images: List<LocalMedia> = mAdapter!!.getImages()
            startPreview(images, position)
        } else if (mLruCache != null) {
            if (mLruCache.remove(image) == null) {
                for ((key) in mLruCache.entrySet()) {
                    if (key.getPath().equals(image.getPath()) || key.getId() === image.getId()) {
                        mLruCache.remove(key)
                        break
                    }
                }
            }
        }
    }

    override fun onTakePhoto() {
        // 启动相机拍照,先判断手机是否有拍照权限
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            // 获取到相机权限再验证是否有存储权限
            if (PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                initCamera()
                if (mInstagramViewPager != null) {
                    if (mInstagramViewPager.getSelectedPosition() === 2) {
                        takeAudioPermissions()
                    }
                }
            } else {
                PermissionChecker.requestPermissions(this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE), PictureConfig.APPLY_CAMERA_STORAGE_PERMISSIONS_CODE)
            }
        } else {
            PermissionChecker
                    .requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE)
        }
    }

    private fun takeAudioPermissions() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
        } else if (SPUtils.getPictureSpUtils().getBoolean(RECORD_AUDIO_PERMISSION) && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            showPermissionsDialog(true, getString(R.string.picture_audio))
        } else {
            SPUtils.getPictureSpUtils().put(RECORD_AUDIO_PERMISSION, true)
            PermissionChecker
                    .requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE)
        }
    }

    private fun initCamera() {
        if (!(mList!![1] as PagePhoto).isBindCamera()) {
            (mList!![1] as PagePhoto).setEmptyViewVisibility(View.INVISIBLE)
            isRunningBind = true
            mHandler.postDelayed(mBindRunnable, 500)
        }
    }

    override fun onChange(selectImages: List<LocalMedia?>?) {}
    override fun onPictureClick(media: LocalMedia?, position: Int) {
        val images: List<LocalMedia> = mAdapter!!.getImages()
        startPreview(images, position)
    }

    /**
     * preview image and video
     *
     * @param previewImages
     * @param position
     */
    fun startPreview(previewImages: List<LocalMedia>?, position: Int) {
        if (!isChangeFolder && mPreviewPosition == position) {
            return
        }
        val holder: RecyclerView.ViewHolder = mPictureRecycler.findViewHolderForAdapterPosition(position)
        if (!mInstagramGallery!!.isScrollTop()) {
            if (position == 0) {
                mPictureRecycler.smoothScrollToPosition(0)
            } else if (holder != null && holder.itemView != null) {
                mPictureRecycler.smoothScrollBy(0, holder.itemView.getTop())
            }
        }
        if (mInstagramGallery != null) {
            mInstagramGallery.expandPreview(object : AnimationCallback() {
                fun onAnimationStart() {}
                fun onAnimationEnd() {
                    if (position == 0) {
                        mPictureRecycler.smoothScrollToPosition(0)
                    } else if (holder != null && holder.itemView != null) {
                        mPictureRecycler.smoothScrollBy(0, holder.itemView.getTop())
                    }
                }
            })
        }
        if (mPreviewPosition >= 0) {
            savePreviousPositionCropInfo(if (isChangeFolder) foldersList[mPreviousFolderPosition].getData().get(mPreviewPosition) else previewImages!![mPreviewPosition])
        }
        if (isChangeFolder) {
            isChangeFolder = false
        }
        setPreviewPosition(position)
        val media: LocalMedia = previewImages!![position]
        val mimeType: String = media.getMimeType()
        if (PictureMimeType.isHasVideo(mimeType)) {
            // video
            mPreviewContainer!!.checkModel(InstagramPreviewContainer.PLAY_VIDEO_MODE)
            mPreviewContainer!!.playVideo(media, holder)
        } else if (PictureMimeType.isHasAudio(mimeType)) {
            // audio
            audioDialog(media.getPath())
        } else {
            // image
            if (media != null) {
                mPreviewContainer!!.checkModel(InstagramPreviewContainer.PLAY_IMAGE_MODE)
                val path: String
                //                if (media.isCut() && !media.isCompressed()) {
//                    // 裁剪过
//                    path = media.getCutPath();
//                } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
//                    // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
//                    path = media.getCompressPath();
//                } else {
                path = media.getPath()
                //                }
                val isHttp: Boolean = PictureMimeType.isHasHttp(path)
                val isAndroidQ: Boolean = SdkVersionUtils.checkedAndroid_Q()
                val uri = if (isHttp || isAndroidQ) Uri.parse(path) else Uri.fromFile(File(path))
                val suffix = mimeType.replace("image/", ".")
                val file: File = File(PictureFileUtils.getDiskCacheDir(this),
                        if (TextUtils.isEmpty(config.renameCropFileName)) DateUtils.getCreateFileName("IMG_").toString() + suffix else config.renameCropFileName)
                mPreviewContainer!!.setImageUri(uri, Uri.fromFile(file))
            }
        }
    }

    private fun savePreviousPositionCropInfo(previousMedia: LocalMedia?) {
        if (previousMedia == null || mLruCache == null || mPreviewContainer == null || config.selectionMode === PictureConfig.SINGLE || !PictureMimeType.isHasImage(previousMedia.getMimeType())) {
            return
        }
        val selectedImages: List<LocalMedia> = mAdapter!!.getSelectedImages()
        if (selectedImages.contains(previousMedia)) {
            mLruCache.put(previousMedia, mPreviewContainer.createCropAndSaveImageTask(BitmapCropCallbackImpl(previousMedia)))
        } else {
            for (selectedImage in selectedImages) {
                if (selectedImage.getPath().equals(previousMedia.getPath()) || selectedImage.getId() === previousMedia.getId()) {
                    mLruCache.put(selectedImage, mPreviewContainer.createCropAndSaveImageTask(BitmapCropCallbackImpl(selectedImage)))
                    break
                }
            }
        }
    }

    private fun startMultiCrop() {
        if (mLruCache == null || mAdapter == null || mPreviewContainer == null || isCroppingImage) {
            return
        }
        isCroppingImage = true
        showPleaseDialog()
        for ((_, value) in mLruCache.entrySet()) {
            Objects.requireNonNull<Any>(value as BitmapCropTask).execute()
        }
        FinishMultiCropTask(this, mPreviewContainer, mAdapter.getSelectedImages(), config).execute()
    }

    class FinishMultiCropTask(activity: PictureSelectorInstagramStyleActivity, previewContainer: InstagramPreviewContainer, selectedImages: List<LocalMedia>?, config: PictureSelectionConfig) : AsyncTask<Void?, Void?, Void?>() {
        private val mContainerWeakReference: WeakReference<InstagramPreviewContainer>
        private val mActivityWeakReference: WeakReference<PictureSelectorInstagramStyleActivity>
        private val mSelectedImages: List<LocalMedia>?
        private val mConfig: PictureSelectionConfig
        protected override fun doInBackground(vararg voids: Void): Void {
            return null
        }

        protected override fun onPostExecute(aVoid: Void) {
            val previewContainer: InstagramPreviewContainer? = mContainerWeakReference.get()
            val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
            result.addAll(mSelectedImages!!)
            var bundle: Bundle? = null
            if (previewContainer != null) {
                bundle = Bundle()
                bundle.putBoolean(InstagramMediaProcessActivity.EXTRA_ASPECT_RATIO, previewContainer.isAspectRatio())
            }
            val activity = mActivityWeakReference.get()
            if (activity != null) {
                activity.dismissDialog()
                InstagramMediaProcessActivity.launchActivity(activity, mConfig, result, bundle, InstagramMediaProcessActivity.REQUEST_MULTI_IMAGE_PROCESS)
            }
        }

        init {
            mContainerWeakReference = WeakReference<InstagramPreviewContainer>(previewContainer)
            mActivityWeakReference = WeakReference(activity)
            mSelectedImages = selectedImages
            mConfig = config
        }
    }

    private class BitmapCropCallbackImpl(localMedia: LocalMedia?) : BitmapCropCallback {
        private var mLocalMedia: LocalMedia?
        fun setLocalMedia(localMedia: LocalMedia?) {
            mLocalMedia = localMedia
        }

        fun onBitmapCropped(resultUri: Uri, offsetX: Int, offsetY: Int, imageWidth: Int, imageHeight: Int) {
            if (mLocalMedia != null) {
                mLocalMedia.setCut(true)
                mLocalMedia.setCutPath(resultUri.path)
                mLocalMedia.setWidth(imageWidth)
                mLocalMedia.setHeight(imageHeight)
                mLocalMedia.setSize(File(resultUri.path).length())
                mLocalMedia.setAndroidQToPath(if (SdkVersionUtils.checkedAndroid_Q()) resultUri.path else mLocalMedia.getAndroidQToPath())
            }
        }

        fun onCropFailure(t: Throwable) {
            t.printStackTrace()
        }

        init {
            mLocalMedia = localMedia
        }
    }

    private fun setPreviewPosition(position: Int) {
        if (mPreviewPosition != position && mAdapter != null && position < mAdapter.getItemCount()) {
            val previousPosition = mPreviewPosition
            mPreviewPosition = position
            mAdapter.setPreviewPosition(position)
            mAdapter.notifyItemChanged(previousPosition)
            mAdapter.notifyItemChanged(position)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.PREVIEW_VIDEO_CODE -> if (data != null) {
                    val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
                    if (list != null && list.size > 0) {
                        onResult(list)
                    }
                }
                UCrop.REQUEST_CROP -> singleImageFilterHandle(data)
                InstagramMediaProcessActivity.REQUEST_SINGLE_IMAGE_PROCESS -> singleCropHandleResult(data)
                InstagramMediaProcessActivity.REQUEST_MULTI_IMAGE_PROCESS -> if (mAdapter!!.getSelectedImages()!!.size() > 1) {
                    if (data != null) {
                        val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
                        if (list != null) {
                            mAdapter!!.bindSelectImages(list)
                            mAdapter.notifyDataSetChanged()
                        }
                    }
                    handlerResult(mAdapter!!.getSelectedImages())
                } else {
                    singleCropHandleResult(data)
                }
                InstagramMediaProcessActivity.REQUEST_SINGLE_VIDEO_PROCESS -> {
                    if (data != null) {
                        val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
                        if (list != null) {
                            mAdapter!!.bindSelectImages(list)
                            mAdapter.notifyDataSetChanged()
                        }
                    }
                    onResult(mAdapter!!.getSelectedImages())
                }
                UCrop.REQUEST_MULTI_CROP -> multiCropHandleResult(data)
                PictureConfig.REQUEST_CAMERA -> requestCamera(data)
                else -> {
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            previewCallback(data)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val throwable = data.getSerializableExtra(UCrop.EXTRA_ERROR) as Throwable
                ToastUtils.s(context, throwable.message)
            }
        } else if (resultCode == InstagramMediaProcessActivity.RESULT_MEDIA_PROCESS_CANCELED) {
            val result: MutableList<LocalMedia> = mAdapter!!.getSelectedImages()
            if (!result.isEmpty()) {
                result.clear()
            }
            if (mLruCache != null && mLruCache.size() > 0) {
                mLruCache.clear()
            }
            if (mPreviewContainer != null && mPreviewContainer.isMulti()) {
                mPreviewContainer.setMultiMode(false)
            }
            isCroppingImage = false
        }
    }

    private fun singleImageFilterHandle(data: Intent?) {
        val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
        if (mAdapter!!.getSelectedImages()!!.size() > 0) {
            result.add(mAdapter!!.getSelectedImages()!!.get(0))
        }
        val bundle = Bundle()
        if (data != null && !result.isEmpty()) {
            val media: LocalMedia = result[0]
            val resultUri: Uri = UCrop.getOutput(data)
            if (resultUri != null) {
                media.setCut(true)
                media.setCutPath(resultUri.path)
            }
        }
        if (mPreviewContainer != null && !data.getBooleanExtra(BitmapCropSquareTask.EXTRA_FROM_CAMERA, false)) {
            bundle.putBoolean(InstagramMediaProcessActivity.EXTRA_ASPECT_RATIO, mPreviewContainer.isAspectRatio())
        }
        InstagramMediaProcessActivity.launchActivity(this, config, result, bundle, InstagramMediaProcessActivity.REQUEST_SINGLE_IMAGE_PROCESS)
    }

    /**
     * 预览界面回调处理
     *
     * @param data
     */
    private fun previewCallback(data: Intent?) {
        if (data == null) {
            return
        }
        // 在预览界面按返回键或已完成的处理逻辑
        val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
        if (mAdapter != null && list != null) {
            // 判断预览界面是点击已完成按钮还是仅仅是勾选图片
            val isCompleteOrSelected: Boolean = data.getBooleanExtra(PictureConfig.EXTRA_COMPLETE_SELECTED, false)
            if (isCompleteOrSelected) {
                onChangeData(list)
                if (config.isWithVideoImage) {
                    // 混选模式
                    val size = list.size
                    var imageSize = 0
                    for (i in 0 until size) {
                        val media: LocalMedia = list[i]
                        if (PictureMimeType.isHasImage(media.getMimeType())) {
                            imageSize++
                            break
                        }
                    }
                    if (imageSize <= 0 || !config.isCompress || config.isCheckOriginalImage) {
                        // 全是视频
                        onResult(list)
                    } else {
                        // 去压缩
                        compressImage(list)
                    }
                } else {
                    // 取出第1个判断是否是图片，视频和图片只能二选一，不必考虑图片和视频混合
                    val mimeType = if (list.size > 0) list[0].getMimeType() else ""
                    if (config.isCompress && PictureMimeType.isHasImage(mimeType)
                            && !config.isCheckOriginalImage) {
                        compressImage(list)
                    } else {
                        onResult(list)
                    }
                }
            }
            mAdapter.bindSelectImages(list)
            mAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 预览界面返回更新回调
     *
     * @param list
     */
    protected fun onChangeData(list: List<LocalMedia>?) {}

    /**
     * singleDirectReturn模式摄像头后处理方式
     *
     * @param mimeType
     */
    private fun singleDirectReturnCameraHandleResult(mimeType: String?) {
        val eqImg: Boolean = PictureMimeType.isHasImage(mimeType)
        if (config.enableCrop && eqImg) {
            // 去裁剪
            config.originalPath = config.cameraPath
            //            startCrop(config.cameraPath, mimeType);
            startSingleCrop(config.cameraPath, mimeType)
        } else if (config.isCompress && eqImg) {
            // 去压缩
            val selectedImages: List<LocalMedia> = mAdapter!!.getSelectedImages()
            compressImage(selectedImages)
        } else {
            // 不裁剪 不压缩 直接返回结果
            onResult(mAdapter!!.getSelectedImages())
        }
    }

    protected fun startSingleCrop(originalPath: String?, mimeType: String?) {
        if (DoubleUtils.isFastDoubleClick()) {
            return
        }
        if (TextUtils.isEmpty(originalPath)) {
            ToastUtils.s(this, getString(R.string.picture_not_crop_data))
            return
        }
        val isHttp: Boolean = PictureMimeType.isHasHttp(originalPath)
        val suffix = mimeType!!.replace("image/", ".")
        val file: File = File(PictureFileUtils.getDiskCacheDir(context),
                if (TextUtils.isEmpty(config.renameCropFileName)) DateUtils.getCreateFileName("IMG_CROP_").toString() + suffix else config.renameCropFileName)
        val uri = if (isHttp || SdkVersionUtils.checkedAndroid_Q()) Uri.parse(originalPath) else Uri.fromFile(File(originalPath))
        val destination = Uri.fromFile(file)
        val maxBitmapSize: Int = BitmapLoadUtils.calculateMaxBitmapSize(context)
        BitmapLoadUtils.decodeBitmapInBackground(context, uri, destination, maxBitmapSize, maxBitmapSize,
                object : BitmapLoadCallback() {
                    fun onBitmapLoaded(bitmap: Bitmap, exifInfo: ExifInfo, imageInputPath: String, imageOutputPath: String?) {
                        BitmapCropSquareTask(bitmap, imageOutputPath, this@PictureSelectorInstagramStyleActivity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }

                    fun onFailure(bitmapWorkerException: Exception) {
                        ToastUtils.s(context, bitmapWorkerException.message)
                    }
                })
    }

    /**
     * 拍照后处理结果
     *
     * @param data
     */
    private fun requestCamera(data: Intent?) {
        // on take photo success
        var mimeType: String? = null
        var duration: Long = 0
        val isAndroidQ: Boolean = SdkVersionUtils.checkedAndroid_Q()
        if (config.chooseMode === PictureMimeType.ofAudio()) {
            // 音频处理规则
            config.cameraPath = getAudioPath(data)
            if (TextUtils.isEmpty(config.cameraPath)) {
                return
            }
            mimeType = PictureMimeType.MIME_TYPE_AUDIO
            duration = MediaUtils.extractDuration(context, isAndroidQ, config.cameraPath)
        }
        if (TextUtils.isEmpty(config.cameraPath) || File(config.cameraPath) == null) {
            return
        }
        var size: Long = 0
        var newSize = IntArray(2)
        if (!isAndroidQ) {
            if (config.isFallbackVersion3) {
                PictureMediaScannerConnection(context, config.cameraPath)
            } else {
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(config.cameraPath))))
            }
        }
        val media = LocalMedia()
        if (config.chooseMode !== PictureMimeType.ofAudio()) {
            // 图片视频处理规则
            if (PictureMimeType.isContent(config.cameraPath)) {
                val path: String = PictureFileUtils.getPath(getApplicationContext(), Uri.parse(config.cameraPath))
                if (!TextUtils.isEmpty(path)) {
                    val file = File(path)
                    size = file.length()
                    mimeType = PictureMimeType.getMimeType(file)
                }
                if (PictureMimeType.isHasImage(mimeType)) {
                    newSize = MediaUtils.getImageSizeForUrlToAndroidQ(this, config.cameraPath)
                } else {
                    newSize = MediaUtils.getVideoSizeForUri(this, Uri.parse(config.cameraPath))
                    duration = MediaUtils.extractDuration(context, true, config.cameraPath)
                }
                val lastIndexOf: Int = config.cameraPath.lastIndexOf("/") + 1
                media.setId(if (lastIndexOf > 0) ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) else -1)
                media.setRealPath(path)
                if (config.isUseCustomCamera) {
                    // 自定义拍照时已经在应用沙盒内生成了文件
                    if (data != null) {
                        val mediaPath: String = data.getStringExtra(PictureConfig.EXTRA_MEDIA_PATH)
                        media.setAndroidQToPath(mediaPath)
                    }
                }
            } else {
                val file: File = File(config.cameraPath)
                mimeType = PictureMimeType.getMimeType(file)
                size = file.length()
                if (PictureMimeType.isHasImage(mimeType)) {
                    val degree: Int = PictureFileUtils.readPictureDegree(this, config.cameraPath)
                    BitmapUtils.rotateImage(degree, config.cameraPath)
                    newSize = MediaUtils.getImageSizeForUrl(config.cameraPath)
                } else {
                    newSize = MediaUtils.getVideoSizeForUrl(config.cameraPath)
                    duration = MediaUtils.extractDuration(context, false, config.cameraPath)
                }
                // 拍照产生一个临时id
                media.setId(System.currentTimeMillis())
            }
        }
        media.setDuration(duration)
        media.setWidth(newSize[0])
        media.setHeight(newSize[1])
        media.setPath(config.cameraPath)
        media.setMimeType(mimeType)
        media.setSize(size)
        media.setChooseModel(config.chooseMode)
        if (mAdapter != null) {
            var isPreview = true
            images!!.add(0, media)
            if (checkVideoLegitimacy(media)) {
                if (config.selectionMode === PictureConfig.SINGLE) {
                    // 单选模式下直接返回模式
                    if (config.isSingleDirectReturn) {
                        val selectedImages: MutableList<LocalMedia> = mAdapter.getSelectedImages()
                        if (selectedImages.size > 0) {
                            selectedImages.clear()
                        }
                        selectedImages.add(media)
                        mAdapter.bindSelectImages(selectedImages)
                        singleDirectReturnCameraHandleResult(mimeType)
                    } else {
                        val selectedImages: MutableList<LocalMedia> = mAdapter.getSelectedImages()
                        mimeType = if (selectedImages.size > 0) selectedImages[0].getMimeType() else ""
                        val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, media.getMimeType())
                        // 类型相同或还没有选中才加进选中集合中
                        if (mimeTypeSame || selectedImages.size == 0) {
                            // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                            singleRadioMediaImage()
                            selectedImages.add(media)
                            mAdapter.bindSelectImages(selectedImages)
                        }
                    }
                } else {
                    // 多选模式
                    val selectedImages: List<LocalMedia> = mAdapter.getSelectedImages()
                    val count = selectedImages.size
                    mimeType = if (count > 0) selectedImages[0].getMimeType() else ""
                    val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, media.getMimeType())
                    if (config.isWithVideoImage) {
                        // 混选模式
                        var videoSize = 0
                        var imageSize = 0
                        for (i in 0 until count) {
                            val m: LocalMedia = selectedImages[i]
                            if (PictureMimeType.isHasVideo(m.getMimeType())) {
                                videoSize++
                            } else {
                                imageSize++
                            }
                        }
                        if (PictureMimeType.isHasVideo(media.getMimeType()) && config.maxVideoSelectNum > 0) {
                            // 视频还可选
                            if (videoSize < config.maxVideoSelectNum) {
                                selectedImages.add(0, media)
                                mAdapter.bindSelectImages(selectedImages)
                            } else {
                                isPreview = false
                                ToastUtils.s(context, StringUtils.getMsg(context, media.getMimeType(),
                                        config.maxVideoSelectNum))
                            }
                        } else {
                            // 图片还可选
                            if (imageSize < config.maxSelectNum) {
                                selectedImages.add(0, media)
                                mAdapter.bindSelectImages(selectedImages)
                            } else {
                                isPreview = false
                                ToastUtils.s(context, StringUtils.getMsg(context, media.getMimeType(),
                                        config.maxSelectNum))
                            }
                        }
                    } else {
                        if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) {
                            // 类型相同或还没有选中才加进选中集合中
                            if (count < config.maxVideoSelectNum) {
                                if (mimeTypeSame || count == 0) {
                                    if (selectedImages.size < config.maxVideoSelectNum) {
                                        selectedImages.add(0, media)
                                        mAdapter.bindSelectImages(selectedImages)
                                    }
                                }
                            } else {
                                isPreview = false
                                ToastUtils.s(context, StringUtils.getMsg(context, mimeType,
                                        config.maxVideoSelectNum))
                            }
                        } else {
                            // 没有到最大选择量 才做默认选中刚拍好的
                            if (count < config.maxSelectNum) {
                                // 类型相同或还没有选中才加进选中集合中
                                if (mimeTypeSame || count == 0) {
                                    selectedImages.add(0, media)
                                    mAdapter.bindSelectImages(selectedImages)
                                }
                            } else {
                                isPreview = false
                                ToastUtils.s(context, StringUtils.getMsg(context, mimeType,
                                        config.maxSelectNum))
                            }
                        }
                    }
                }
            }
            val localMediaFolder: LocalMediaFolder = foldersList[mFolderPosition]
            if (localMediaFolder.isCameraFolder() || "Camera" == localMediaFolder.getName()) {
                mAdapter.notifyItemInserted(if (config.isCamera) 1 else 0)
                mAdapter.notifyItemRangeChanged(if (config.isCamera) 1 else 0, images!!.size)
                if (images!!.size == 1 || config.selectionMode !== PictureConfig.SINGLE && isPreview) {
                    startPreview(images, 0)
                } else {
                    setPreviewPosition(mPreviewPosition + 1)
                }
            }
            // 解决部分手机拍照完Intent.ACTION_MEDIA_SCANNER_SCAN_FILE，不及时刷新问题手动添加
            manualSaveFolder(media)
            // 这里主要解决极个别手机拍照会在DCIM目录重复生成一张照片问题
            if (!isAndroidQ && PictureMimeType.isHasImage(media.getMimeType())) {
                val lastImageId: Int = getLastImageId(media.getMimeType())
                if (lastImageId != -1) {
                    removeMedia(lastImageId)
                }
            }
            mTvEmpty.setVisibility(if (images!!.size > 0) View.INVISIBLE else View.VISIBLE)
            mInstagramGallery!!.setViewVisibility(if (images!!.size > 0) View.VISIBLE else View.INVISIBLE)
            val enabled = images!!.size > 0 || config.returnEmpty
            mTvPictureRight.setEnabled(enabled)
            mTvPictureRight.setTextColor(if (enabled) config.style.pictureRightDefaultTextColor else ContextCompat.getColor(context, R.color.picture_color_9B9B9D))
        }
    }

    /**
     * 验证视频的合法性
     *
     * @param media
     * @return
     */
    private fun checkVideoLegitimacy(media: LocalMedia): Boolean {
        var isEnterNext = true
        if (PictureMimeType.isHasVideo(media.getMimeType())) {
            // 判断视频是否符合条件
            if (config.videoMinSecond > 0 && config.videoMaxSecond > 0) {
                // 用户设置了最小和最大视频时长，判断视频是否在区间之内
                if (media.getDuration() < config.videoMinSecond || media.getDuration() > config.videoMaxSecond) {
                    isEnterNext = false
                    ToastUtils.s(context, getString(R.string.picture_choose_limit_seconds, config.videoMinSecond / 1000, config.videoMaxSecond / 1000))
                }
            } else if (config.videoMinSecond > 0 && config.videoMaxSecond <= 0) {
                // 用户只设置了最小时长视频限制
                if (media.getDuration() < config.videoMinSecond) {
                    isEnterNext = false
                    ToastUtils.s(context, getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                }
            } else if (config.videoMinSecond <= 0 && config.videoMaxSecond > 0) {
                // 用户只设置了最大时长视频限制
                if (media.getDuration() > config.videoMaxSecond) {
                    isEnterNext = false
                    ToastUtils.s(context, getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                }
            }
        }
        return isEnterNext
    }

    /**
     * 单张图片裁剪
     *
     * @param data
     */
    private fun singleCropHandleResult(data: Intent?) {
        if (data == null) {
            return
        }
        val result: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
        val resultUri: Uri = UCrop.getOutput(data)
        val cutPath = resultUri.path
        if (mAdapter != null) {
            val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
            if (list != null) {
                mAdapter.bindSelectImages(list)
                mAdapter.notifyDataSetChanged()
            }
            // 取单张裁剪已选中图片的path作为原图
            val mediaList: List<LocalMedia> = mAdapter.getSelectedImages()
            var media: LocalMedia? = if (mediaList != null && mediaList.size > 0) mediaList[0] else null
            if (media != null) {
                config.originalPath = media.getPath()
                media.setCutPath(cutPath)
                media.setChooseModel(config.chooseMode)
                if (TextUtils.isEmpty(cutPath)) {
                    if (SdkVersionUtils.checkedAndroid_Q()
                            && PictureMimeType.isContent(media.getPath())) {
                        val path: String = PictureFileUtils.getPath(this, Uri.parse(media.getPath()))
                        media.setSize(File(path).length())
                    } else {
                        media.setSize(File(media.getPath()).length())
                    }
                    media.setCut(false)
                } else {
                    media.setSize(File(cutPath).length())
                    media.setAndroidQToPath(cutPath)
                    media.setCut(true)
                }
                result.add(media)
                handlerResult(result)
            } else {
                // 预览界面选中图片并裁剪回调的
                media = if (list != null && list.size > 0) list[0] else null
                config.originalPath = media.getPath()
                media.setCutPath(cutPath)
                media.setChooseModel(config.chooseMode)
                media.setSize(File(if (TextUtils.isEmpty(cutPath)) media.getPath() else cutPath).length())
                if (TextUtils.isEmpty(cutPath)) {
                    if (SdkVersionUtils.checkedAndroid_Q()
                            && PictureMimeType.isContent(media.getPath())) {
                        val path: String = PictureFileUtils.getPath(this, Uri.parse(media.getPath()))
                        media.setSize(File(path).length())
                    } else {
                        media.setSize(File(media.getPath()).length())
                    }
                    media.setCut(false)
                } else {
                    media.setSize(File(cutPath).length())
                    media.setAndroidQToPath(cutPath)
                    media.setCut(true)
                }
                result.add(media)
                handlerResult(result)
            }
        }
    }

    /**
     * 多张图片裁剪
     *
     * @param data
     */
    protected fun multiCropHandleResult(data: Intent?) {
        if (data == null) {
            return
        }
        val mCuts: List<CutInfo> = UCrop.getMultipleOutput(data)
        if (mCuts == null || mCuts.size == 0) {
            return
        }
        val size = mCuts.size
        val isAndroidQ: Boolean = SdkVersionUtils.checkedAndroid_Q()
        val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
        if (list != null) {
            mAdapter!!.bindSelectImages(list)
            mAdapter.notifyDataSetChanged()
        }
        val oldSize = if (mAdapter != null) mAdapter.getSelectedImages()!!.size() else 0
        if (oldSize == size) {
            val result: List<LocalMedia> = mAdapter!!.getSelectedImages()
            for (i in 0 until size) {
                val c: CutInfo = mCuts[i]
                val media: LocalMedia = result[i]
                media.setCut(!TextUtils.isEmpty(c.getCutPath()))
                media.setPath(c.getPath())
                media.setMimeType(c.getMimeType())
                media.setCutPath(c.getCutPath())
                media.setWidth(c.getImageWidth())
                media.setHeight(c.getImageHeight())
                media.setAndroidQToPath(if (isAndroidQ) c.getCutPath() else media.getAndroidQToPath())
                media.setSize(if (!TextUtils.isEmpty(c.getCutPath())) File(c.getCutPath()).length() else media.getSize())
            }
            handlerResult(result)
        } else {
            // 容错处理
            val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
            for (i in 0 until size) {
                val c: CutInfo = mCuts[i]
                val media = LocalMedia()
                media.setId(c.getId())
                media.setCut(!TextUtils.isEmpty(c.getCutPath()))
                media.setPath(c.getPath())
                media.setCutPath(c.getCutPath())
                media.setMimeType(c.getMimeType())
                media.setWidth(c.getImageWidth())
                media.setHeight(c.getImageHeight())
                media.setDuration(c.getDuration())
                media.setChooseModel(config.chooseMode)
                media.setAndroidQToPath(if (isAndroidQ) c.getCutPath() else null)
                if (!TextUtils.isEmpty(c.getCutPath())) {
                    media.setSize(File(c.getCutPath()).length())
                } else {
                    if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(c.getPath())) {
                        val path: String = PictureFileUtils.getPath(this, Uri.parse(c.getPath()))
                        media.setSize(File(path).length())
                    } else {
                        media.setSize(File(c.getPath()).length())
                    }
                }
                result.add(media)
            }
            handlerResult(result)
        }
    }

    /**
     * 单选图片
     */
    private fun singleRadioMediaImage() {
        val selectImages: MutableList<LocalMedia> = mAdapter!!.getSelectedImages()
        if (selectImages != null
                && selectImages.size > 0) {
            val media: LocalMedia = selectImages[0]
            val position: Int = media.getPosition()
            selectImages.clear()
            mAdapter.notifyItemChanged(position)
        }
    }

    /**
     * 手动添加拍照后的相片到图片列表，并设为选中
     *
     * @param media
     */
    private fun manualSaveFolder(media: LocalMedia) {
        try {
            createNewFolder(foldersList)
            val folder: LocalMediaFolder = getImageFolder(media.getPath(), media.getRealPath(), foldersList)
            val cameraFolder: LocalMediaFolder? = if (foldersList.size > 0) foldersList[0] else null
            if (cameraFolder != null && folder != null) {
                media.setParentFolderName(folder.getName())
                // 相机胶卷
                cameraFolder.setFirstImagePath(media.getPath())
                cameraFolder.setData(images)
                cameraFolder.setImageNum(cameraFolder.getImageNum() + 1)
                // 拍照相册
                val num: Int = folder.getImageNum() + 1
                folder.setImageNum(num)
                folder.getData().add(0, media)
                folder.setFirstImagePath(config.cameraPath)
                folderWindow.bindFolder(foldersList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新一下相册目录
     *
     * @param imageFolders
     */
    private fun updateMediaFolder(imageFolders: List<LocalMediaFolder>, media: LocalMedia) {
        val imageFile: File = File(if (PictureMimeType.isContent(media.getPath())) Objects.requireNonNull(PictureFileUtils.getPath(context, Uri.parse(media.getPath()))) else media.getPath())
        val folderFile = imageFile.parentFile
        val size = imageFolders.size
        for (i in 0 until size) {
            val folder: LocalMediaFolder = imageFolders[i]
            // 同一个文件夹下，返回自己，否则创建新文件夹
            val name: String = folder.getName()
            if (TextUtils.isEmpty(name)) {
                continue
            }
            if (name == folderFile.name) {
                folder.setFirstImagePath(config.cameraPath)
                folder.setImageNum(folder.getImageNum() + 1)
                folder.setCheckedNum(1)
                folder.getData().add(0, media)
                break
            }
        }
    }

    override fun onBackPressed() {
        if (mInstagramViewPager!!.getSelectedPosition() === 2 && (mList!![1] as PagePhoto).isInLongPress()) {
            return
        }
        super.onBackPressed()
        if (config != null && PictureSelectionConfig.listener != null) {
            PictureSelectionConfig.listener.onCancel()
        }
        closeActivity()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null && mHandler != null) {
            mHandler.removeCallbacks(mRunnable)
            mHandler.removeCallbacks(mBindRunnable)
            mediaPlayer.release()
            mediaPlayer = null
        }
        if (mPreviewContainer != null) {
            mPreviewContainer.onDestroy()
        }
        if (mInstagramViewPager != null) {
            mInstagramViewPager.onDestroy()
        }
    }

    fun onItemClick(v: View?, position: Int) {
        when (position) {
            PhotoItemSelectedDialog.IMAGE_CAMERA ->                 // 拍照
                if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                    PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(context, config, PictureConfig.TYPE_IMAGE)
                } else {
                    startOpenCamera()
                }
            PhotoItemSelectedDialog.VIDEO_CAMERA ->                 // 录视频
                if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                    PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(context, config, PictureConfig.TYPE_VIDEO)
                } else {
                    startOpenCameraVideo()
                }
            else -> {
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE ->                 // 存储权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readLocalMedia()
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
                }
            PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE ->                 // 相机权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto()
                } else {
                    (mList!![1] as PagePhoto).setEmptyViewVisibility(View.VISIBLE)
                }
            PictureConfig.APPLY_CAMERA_STORAGE_PERMISSIONS_CODE ->                 // 拍照前重新获取存储权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCamera()
                    if (mInstagramViewPager != null) {
                        if (mInstagramViewPager.getSelectedPosition() === 2) {
                            takeAudioPermissions()
                        }
                    }
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
                }
            PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE ->                 // 录音权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takeAudioPermissions()
                } else {
                }
        }
    }

    protected override fun showPermissionsDialog(isCamera: Boolean, errorMsg: String?) {
        if (isFinishing()) {
            return
        }
        val dialog = PictureCustomDialog(context, R.layout.picture_wind_base_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val btn_cancel: Button = dialog.findViewById(R.id.btn_cancel)
        val btn_commit: Button = dialog.findViewById(R.id.btn_commit)
        btn_commit.setText(getString(R.string.picture_go_setting))
        val tv_title: TextView = dialog.findViewById(R.id.tvTitle)
        val tv_content: TextView = dialog.findViewById(R.id.tv_content)
        tv_title.setText(getString(R.string.picture_prompt))
        tv_content.setText(errorMsg)
        btn_cancel.setOnClickListener { v: View? ->
            if (!isFinishing()) {
                dialog.dismiss()
            }
            if (!isCamera) {
                closeActivity()
            }
        }
        btn_commit.setOnClickListener { v: View? ->
            if (!isFinishing()) {
                dialog.dismiss()
            }
            PermissionChecker.launchAppDetailsSettings(context)
            isEnterSetting = true
        }
        dialog.show()
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION = "RECORD_AUDIO_PERMISSION"
    }
}