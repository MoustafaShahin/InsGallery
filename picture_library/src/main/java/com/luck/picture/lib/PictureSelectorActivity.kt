package com.luck.picture.lib

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.luck.picture.lib.adapter.PictureImageGridAdapter
import java.io.File
import java.util.*

/**
 * @author：luck
 * @data：2018/1/27 19:12
 * @describe: PictureSelectorActivity
 */
class PictureSelectorActivity : PictureBaseActivity(), View.OnClickListener, OnAlbumItemClickListener, OnPhotoSelectChangedListener<LocalMedia?>, OnItemClickListener, OnRecyclerViewPreloadMoreListener {
    protected var mIvPictureLeftBack: ImageView? = null
    protected var mIvArrow: ImageView? = null
    protected var titleViewBg: View? = null
    protected var mTvPictureTitle: TextView? = null
    protected var mTvPictureRight: TextView? = null
    protected var mTvPictureOk: TextView? = null
    protected var mTvEmpty: TextView? = null
    protected var mTvPictureImgNum: TextView? = null
    protected var mTvPicturePreview: TextView? = null
    protected var mTvPlayPause: TextView? = null
    protected var mTvStop: TextView? = null
    protected var mTvQuit: TextView? = null
    protected var mTvMusicStatus: TextView? = null
    protected var mTvMusicTotal: TextView? = null
    protected var mTvMusicTime: TextView? = null
    protected var mRecyclerView: RecyclerPreloadView? = null
    protected var mBottomLayout: RelativeLayout? = null
    protected var mAdapter: PictureImageGridAdapter? = null
    protected var folderWindow: FolderPopWindow? = null
    protected var animation: Animation? = null
    protected var isStartAnimation = false
    protected var mediaPlayer: MediaPlayer? = null
    protected var musicSeekBar: SeekBar? = null
    protected var isPlayAudio = false
    protected var audioDialog: PictureCustomDialog? = null
    protected var mCbOriginal: CheckBox? = null
    protected var oldCurrentListSize = 0
    protected var isEnterSetting = false
    private var intervalClickTime: Long = 0
    private var allFolderSize = 0
    private var mOpenCameraCount = 0
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            allFolderSize = savedInstanceState.getInt(PictureConfig.EXTRA_ALL_FOLDER_SIZE)
            oldCurrentListSize = savedInstanceState.getInt(PictureConfig.EXTRA_OLD_CURRENT_LIST_SIZE, 0)
            selectionMedias = PictureSelector.obtainSelectorList(savedInstanceState)
            if (mAdapter != null) {
                isStartAnimation = true
                mAdapter.bindSelectData(selectionMedias)
            }
        }
    }

    protected fun onResume() {
        super.onResume()
        if (isEnterSetting) {
            if (PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (mAdapter.isDataEmpty()) {
                    readLocalMedia()
                }
            } else {
                showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
            }
            isEnterSetting = false
        }
        if (config.isOriginalControl) {
            if (mCbOriginal != null) {
                mCbOriginal.setChecked(config.isCheckOriginalImage)
            }
        }
    }

    override val resourceId: Int
        get() = R.layout.picture_selector

    protected override fun initWidgets() {
        super.initWidgets()
        container = findViewById(R.id.container)
        titleViewBg = findViewById(R.id.titleViewBg)
        mIvPictureLeftBack = findViewById(R.id.pictureLeftBack)
        mTvPictureTitle = findViewById(R.id.picture_title)
        mTvPictureRight = findViewById(R.id.picture_right)
        mTvPictureOk = findViewById(R.id.picture_tv_ok)
        mCbOriginal = findViewById(R.id.cb_original)
        mIvArrow = findViewById(R.id.ivArrow)
        mTvPicturePreview = findViewById(R.id.picture_id_preview)
        mTvPictureImgNum = findViewById(R.id.picture_tvMediaNum)
        mRecyclerView = findViewById(R.id.picture_recycler)
        mBottomLayout = findViewById(R.id.rl_bottom)
        mTvEmpty = findViewById(R.id.tv_empty)
        isNumComplete(numComplete)
        if (!numComplete) {
            animation = AnimationUtils.loadAnimation(this, R.anim.picture_anim_modal_in)
        }
        mTvPicturePreview.setOnClickListener(this)
        if (config.isAutomaticTitleRecyclerTop) {
            titleViewBg!!.setOnClickListener(this)
        }
        mTvPicturePreview.setVisibility(if (config.chooseMode !== PictureMimeType.ofAudio() && config.enablePreview) View.VISIBLE else View.GONE)
        mBottomLayout.setVisibility(if (config.selectionMode === PictureConfig.SINGLE
                && config.isSingleDirectReturn) View.GONE else View.VISIBLE)
        mIvPictureLeftBack!!.setOnClickListener(this)
        mTvPictureRight.setOnClickListener(this)
        mTvPictureOk.setOnClickListener(this)
        mTvPictureImgNum.setOnClickListener(this)
        mTvPictureTitle.setOnClickListener(this)
        mIvArrow!!.setOnClickListener(this)
        val title: String = if (config.chooseMode === PictureMimeType.ofAudio()) getString(R.string.picture_all_audio) else getString(R.string.picture_camera_roll)
        mTvPictureTitle.setText(title)
        mTvPictureTitle.setTag(R.id.view_tag, -1)
        folderWindow = FolderPopWindow(this, config)
        folderWindow.setArrowImageView(mIvArrow)
        folderWindow.setOnAlbumItemClickListener(this)
        mRecyclerView.addItemDecoration(GridSpacingItemDecoration(config.imageSpanCount,
                ScreenUtils.dip2px(this, 2), false))
        mRecyclerView.setLayoutManager(GridLayoutManager(getContext(), config.imageSpanCount))
        if (!config.isPageStrategy) {
            mRecyclerView.setHasFixedSize(true)
        } else {
            mRecyclerView.setReachBottomRow(RecyclerPreloadView.BOTTOM_PRELOAD)
            mRecyclerView.setOnRecyclerViewPreloadListener(this@PictureSelectorActivity)
        }
        val itemAnimator: ItemAnimator = mRecyclerView.getItemAnimator()
        if (itemAnimator != null) {
            (itemAnimator as SimpleItemAnimator).setSupportsChangeAnimations(false)
            mRecyclerView.setItemAnimator(null)
        }
        loadAllMediaData()
        mTvEmpty.setText(if (config.chooseMode === PictureMimeType.ofAudio()) getString(R.string.picture_audio_empty) else getString(R.string.picture_empty))
        StringUtils.tempTextFont(mTvEmpty, config.chooseMode)
        mAdapter = PictureImageGridAdapter(getContext(), config)
        mAdapter.setOnPhotoSelectChangedListener(this)
        when (config.animationMode) {
            AnimationType.ALPHA_IN_ANIMATION -> mRecyclerView.setAdapter(AlphaInAnimationAdapter(mAdapter))
            AnimationType.SLIDE_IN_BOTTOM_ANIMATION -> mRecyclerView.setAdapter(SlideInBottomAnimationAdapter(mAdapter))
            else -> mRecyclerView.setAdapter(mAdapter)
        }
        if (config.isOriginalControl) {
            mCbOriginal.setVisibility(View.VISIBLE)
            mCbOriginal.setChecked(config.isCheckOriginalImage)
            mCbOriginal.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> config.isCheckOriginalImage = isChecked })
        }
    }

    fun onRecyclerViewPreloadMore() {
        loadMoreData()
    }

    /**
     * getPageLimit
     * # If the user clicks to take a photo and returns, the Limit should be adjusted dynamically
     *
     * @return
     */
    private val pageLimit: Int
        private get() {
            val bucketId: Int = ValueOf.toInt(mTvPictureTitle.getTag(R.id.view_tag))
            if (bucketId == -1) {
                val limit: Int = if (mOpenCameraCount > 0) config.pageSize - mOpenCameraCount else config.pageSize
                mOpenCameraCount = 0
                return limit
            }
            return config.pageSize
        }

    /**
     * load more data
     */
    private fun loadMoreData() {
        if (mAdapter != null) {
            if (isHasMore) {
                mPage++
                val bucketId: Long = ValueOf.toLong(mTvPictureTitle.getTag(R.id.view_tag))
                LocalMediaPageLoader.getInstance(getContext(), config).loadPageMediaData(bucketId, mPage, pageLimit,
                        OnQueryDataResultListener<LocalMedia> { result, currentPage, isHasMore ->
                            if (!isFinishing()) {
                                isHasMore = isHasMore
                                if (isHasMore) {
                                    hideDataNull()
                                    val size: Int = result.size()
                                    if (size > 0) {
                                        val positionStart: Int = mAdapter.getSize()
                                        mAdapter.getData().addAll(result)
                                        val itemCount: Int = mAdapter.getItemCount()
                                        mAdapter.notifyItemRangeChanged(positionStart, itemCount)
                                    } else {
                                        onRecyclerViewPreloadMore()
                                    }
                                    if (size < PictureConfig.MIN_PAGE_SIZE) {
                                        mRecyclerView.onScrolled(mRecyclerView.getScrollX(), mRecyclerView.getScrollY())
                                    }
                                } else {
                                    val isEmpty: Boolean = mAdapter.isDataEmpty()
                                    if (isEmpty) {
                                        showDataNull(if (bucketId == -1L) getString(R.string.picture_empty) else getString(R.string.picture_data_null), R.drawable.picture_icon_no_data)
                                    }
                                }
                            }
                        } as OnQueryDataResultListener<LocalMedia?>?)
            }
        }
    }

    /**
     * load All Data
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
            if (config.style.pictureUnPreviewTextColor !== 0) {
                mTvPicturePreview.setTextColor(config.style.pictureUnPreviewTextColor)
            }
            if (config.style.picturePreviewTextSize !== 0) {
                mTvPicturePreview.setTextSize(config.style.picturePreviewTextSize)
            }
            if (config.style.pictureCheckNumBgStyle !== 0) {
                mTvPictureImgNum.setBackgroundResource(config.style.pictureCheckNumBgStyle)
            }
            if (config.style.pictureUnCompleteTextColor !== 0) {
                mTvPictureOk.setTextColor(config.style.pictureUnCompleteTextColor)
            }
            if (config.style.pictureCompleteTextSize !== 0) {
                mTvPictureOk.setTextSize(config.style.pictureCompleteTextSize)
            }
            if (config.style.pictureBottomBgColor !== 0) {
                mBottomLayout.setBackgroundColor(config.style.pictureBottomBgColor)
            }
            if (config.style.pictureContainerBackgroundColor !== 0) {
                container!!.setBackgroundColor(config.style.pictureContainerBackgroundColor)
            }
            if (!TextUtils.isEmpty(config.style.pictureRightDefaultText)) {
                mTvPictureRight.setText(config.style.pictureRightDefaultText)
            }
            if (!TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                mTvPictureOk.setText(config.style.pictureUnCompleteText)
            }
            if (!TextUtils.isEmpty(config.style.pictureUnPreviewText)) {
                mTvPicturePreview.setText(config.style.pictureUnPreviewText)
            }
        } else {
            if (config.downResId !== 0) {
                val drawable: Drawable = ContextCompat.getDrawable(this, config.downResId)
                mIvArrow!!.setImageDrawable(drawable)
            }
            val pictureBottomBgColor: Int = AttrsUtils.getTypeValueColor(getContext(), R.attr.picture_bottom_bg)
            if (pictureBottomBgColor != 0) {
                mBottomLayout.setBackgroundColor(pictureBottomBgColor)
            }
        }
        titleViewBg!!.setBackgroundColor(colorPrimary)
        if (config.isOriginalControl) {
            if (config.style != null) {
                if (config.style.pictureOriginalControlStyle !== 0) {
                    mCbOriginal.setButtonDrawable(config.style.pictureOriginalControlStyle)
                } else {
                    mCbOriginal.setButtonDrawable(ContextCompat.getDrawable(this, R.drawable.picture_original_checkbox))
                }
                if (config.style.pictureOriginalFontColor !== 0) {
                    mCbOriginal.setTextColor(config.style.pictureOriginalFontColor)
                } else {
                    mCbOriginal.setTextColor(ContextCompat.getColor(this, R.color.picture_color_53575e))
                }
                if (config.style.pictureOriginalTextSize !== 0) {
                    mCbOriginal.setTextSize(config.style.pictureOriginalTextSize)
                }
            } else {
                mCbOriginal.setButtonDrawable(ContextCompat.getDrawable(this, R.drawable.picture_original_checkbox))
                mCbOriginal.setTextColor(ContextCompat.getColor(this, R.color.picture_color_53575e))
            }
        }
        mAdapter.bindSelectData(selectionMedias)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mAdapter != null) {
            // Save the number of pictures or videos in the current list
            outState.putInt(PictureConfig.EXTRA_OLD_CURRENT_LIST_SIZE, mAdapter.getSize())
            // Save the number of Camera film and Camera folder files
            val size: Int = folderWindow.getFolderData().size()
            if (size > 0) {
                outState.putInt(PictureConfig.EXTRA_ALL_FOLDER_SIZE, folderWindow.getFolder(0).getImageNum())
            }
            if (mAdapter.getSelectedData() != null) {
                val selectedImages: List<LocalMedia?> = mAdapter.getSelectedData()
                PictureSelector.saveSelectorList(outState, selectedImages)
            }
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
     * init Text
     */
    protected override fun initCompleteText(startCount: Int) {
        val isNotEmptyStyle = config.style != null
        if (config.selectionMode === PictureConfig.SINGLE) {
            if (startCount <= 0) {
                mTvPictureOk.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_please_select))
            } else {
                val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mTvPictureOk.setText(java.lang.String.format(config.style.pictureCompleteText, startCount, 1))
                } else {
                    mTvPictureOk.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureCompleteText)) config.style.pictureCompleteText else getString(R.string.picture_done))
                }
            }
        } else {
            val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
            if (startCount <= 0) {
                mTvPictureOk.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_done_front_num,
                        startCount, config.maxSelectNum))
            } else {
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mTvPictureOk.setText(java.lang.String.format(config.style.pictureCompleteText, startCount, config.maxSelectNum))
                } else {
                    mTvPictureOk.setText(getString(R.string.picture_done_front_num,
                            startCount, config.maxSelectNum))
                }
            }
        }
    }

    /**
     * get LocalMedia s
     */
    protected fun readLocalMedia() {
        showPleaseDialog()
        if (config.isPageStrategy) {
            LocalMediaPageLoader.getInstance(getContext(), config).loadAllMedia(
                    OnQueryDataResultListener<LocalMediaFolder> { data, currentPage, isHasMore ->
                        if (!isFinishing()) {
                            isHasMore = true
                            initPageModel(data)
                            synchronousCover()
                        }
                    } as OnQueryDataResultListener<LocalMediaFolder?>?)
        } else {
            PictureThreadUtils.executeByIo(object : SimpleTask<List<LocalMediaFolder?>?>() {
                fun doInBackground(): List<LocalMediaFolder> {
                    return LocalMediaLoader(getContext(), config).loadAllMedia()
                }

                fun onSuccess(folders: List<LocalMediaFolder>?) {
                    initStandardModel(folders)
                }
            })
        }
    }

    /**
     * Page Model
     *
     * @param folders
     */
    private fun initPageModel(folders: List<LocalMediaFolder>?) {
        if (folders != null) {
            folderWindow.bindFolder(folders)
            mPage = 1
            val folder: LocalMediaFolder = folderWindow.getFolder(0)
            mTvPictureTitle.setTag(R.id.view_count_tag, if (folder != null) folder.getImageNum() else 0)
            mTvPictureTitle.setTag(R.id.view_index_tag, 0)
            val bucketId = if (folder != null) folder.getBucketId() else -1.toLong()
            mRecyclerView.setEnabledLoadMore(true)
            LocalMediaPageLoader.getInstance(getContext(), config).loadPageMediaData(bucketId, mPage,
                    label@ OnQueryDataResultListener<LocalMedia> { data, currentPage, isHasMore ->
                        if (!isFinishing()) {
                            dismissDialog()
                            if (mAdapter != null) {
                                isHasMore = true
                                // IsHasMore being true means that there's still data, but data being 0 might be a filter that's turned on and that doesn't happen to fit on the whole page
                                if (isHasMore && data.size() === 0) {
                                    onRecyclerViewPreloadMore()
                                    return@label
                                }
                                val currentSize: Int = mAdapter.getSize()
                                val resultSize: Int = data.size()
                                oldCurrentListSize = oldCurrentListSize + currentSize
                                if (resultSize >= currentSize) {
                                    // This situation is mainly caused by the use of camera memory, the Activity is recycled
                                    if (currentSize > 0 && currentSize < resultSize && oldCurrentListSize != resultSize) {
                                        if (isLocalMediaSame(data.get(0))) {
                                            mAdapter.bindData(data)
                                        } else {
                                            mAdapter.getData().addAll(data)
                                        }
                                    } else {
                                        mAdapter.bindData(data)
                                    }
                                }
                                val isEmpty: Boolean = mAdapter.isDataEmpty()
                                if (isEmpty) {
                                    showDataNull(getString(R.string.picture_empty), R.drawable.picture_icon_no_data)
                                } else {
                                    hideDataNull()
                                }
                            }
                        }
                    } as OnQueryDataResultListener<LocalMedia?>?)
        } else {
            showDataNull(getString(R.string.picture_data_exception), R.drawable.picture_icon_data_error)
            dismissDialog()
        }
    }

    /**
     * ofAll Page Model Synchronous cover
     */
    private fun synchronousCover() {
        if (config.chooseMode === PictureMimeType.ofAll()) {
            PictureThreadUtils.executeByIo(object : SimpleTask<Boolean?>() {
                fun doInBackground(): Boolean {
                    val size: Int = folderWindow.getFolderData().size()
                    for (i in 0 until size) {
                        val mediaFolder: LocalMediaFolder = folderWindow.getFolder(i) ?: continue
                        val firstCover: String = LocalMediaPageLoader
                                .getInstance(getContext(), config).getFirstCover(mediaFolder.getBucketId())
                        mediaFolder.setFirstImagePath(firstCover)
                    }
                    return true
                }

                fun onSuccess(result: Boolean?) {
                    // TODO Synchronous Success
                }
            })
        }
    }

    /**
     * Standard Model
     *
     * @param folders
     */
    private fun initStandardModel(folders: List<LocalMediaFolder>?) {
        if (folders != null) {
            if (folders.size > 0) {
                folderWindow.bindFolder(folders)
                val folder: LocalMediaFolder = folders[0]
                folder.setChecked(true)
                mTvPictureTitle.setTag(R.id.view_count_tag, folder.getImageNum())
                val result: List<LocalMedia> = folder.getData()
                if (mAdapter != null) {
                    val currentSize: Int = mAdapter.getSize()
                    val resultSize = result.size
                    oldCurrentListSize = oldCurrentListSize + currentSize
                    if (resultSize >= currentSize) {
                        // This situation is mainly caused by the use of camera memory, the Activity is recycled
                        if (currentSize > 0 && currentSize < resultSize && oldCurrentListSize != resultSize) {
                            mAdapter.getData().addAll(result)
                            val media: LocalMedia = mAdapter.getData().get(0)
                            folder.setFirstImagePath(media.getPath())
                            folder.getData().add(0, media)
                            folder.setCheckedNum(1)
                            folder.setImageNum(folder.getImageNum() + 1)
                            updateMediaFolder(folderWindow.getFolderData(), media)
                        } else {
                            mAdapter.bindData(result)
                        }
                    }
                    val isEmpty: Boolean = mAdapter.isDataEmpty()
                    if (isEmpty) {
                        showDataNull(getString(R.string.picture_empty), R.drawable.picture_icon_no_data)
                    } else {
                        hideDataNull()
                    }
                }
            } else {
                showDataNull(getString(R.string.picture_empty), R.drawable.picture_icon_no_data)
            }
        } else {
            showDataNull(getString(R.string.picture_data_exception), R.drawable.picture_icon_data_error)
        }
        dismissDialog()
    }

    /**
     * isSame
     *
     * @param newMedia
     * @return
     */
    private fun isLocalMediaSame(newMedia: LocalMedia?): Boolean {
        val oldMedia: LocalMedia = mAdapter.getItem(0)
        if (oldMedia == null || newMedia == null) {
            return false
        }
        if (oldMedia.getPath().equals(newMedia.getPath())) {
            return true
        }
        // if Content:// type,determines whether the suffix id is consistent, mainly to solve the following two types of problems
        // content://media/external/images/media/5844
        // content://media/external/file/5844
        if (PictureMimeType.isContent(newMedia.getPath())
                && PictureMimeType.isContent(oldMedia.getPath())) {
            if (!TextUtils.isEmpty(newMedia.getPath()) && !TextUtils.isEmpty(oldMedia.getPath())) {
                val newId: String = newMedia.getPath().substring(newMedia.getPath().lastIndexOf("/") + 1)
                val oldId: String = oldMedia.getPath().substring(oldMedia.getPath().lastIndexOf("/") + 1)
                if (newId == oldId) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Open Camera
     */
    fun startCamera() {
        if (!DoubleUtils.isFastDoubleClick()) {
            if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                if (config.chooseMode === PictureConfig.TYPE_ALL) {
                    val selectedDialog: PhotoItemSelectedDialog = PhotoItemSelectedDialog.newInstance()
                    selectedDialog.setOnItemClickListener(this)
                    selectedDialog.show(getSupportFragmentManager(), "PhotoItemSelectedDialog")
                } else {
                    PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, config.chooseMode)
                    config.cameraMimeType = config.chooseMode
                }
                return
            }
            if (config.isUseCustomCamera) {
                startCustomCamera()
                return
            }
            when (config.chooseMode) {
                PictureConfig.TYPE_ALL -> {
                    val selectedDialog: PhotoItemSelectedDialog = PhotoItemSelectedDialog.newInstance()
                    selectedDialog.setOnItemClickListener(this)
                    selectedDialog.show(getSupportFragmentManager(), "PhotoItemSelectedDialog")
                }
                PictureConfig.TYPE_IMAGE -> startOpenCamera()
                PictureConfig.TYPE_VIDEO -> startOpenCameraVideo()
                PictureConfig.TYPE_AUDIO -> startOpenCameraAudio()
                else -> {
                }
            }
        }
    }

    /**
     * Open Custom Camera
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
        if (id == R.id.pictureLeftBack || id == R.id.picture_right) {
            if (folderWindow != null && folderWindow.isShowing()) {
                folderWindow.dismiss()
            } else {
                onBackPressed()
            }
            return
        }
        if (id == R.id.picture_title || id == R.id.ivArrow) {
            if (folderWindow.isShowing()) {
                folderWindow.dismiss()
            } else {
                if (!folderWindow.isEmpty()) {
                    folderWindow.showAsDropDown(titleViewBg)
                    if (!config.isSingleDirectReturn) {
                        val selectedImages: List<LocalMedia> = mAdapter.getSelectedData()
                        folderWindow.updateFolderCheckStatus(selectedImages)
                    }
                }
            }
            return
        }
        if (id == R.id.picture_id_preview) {
            onPreview()
            return
        }
        if (id == R.id.picture_tv_ok || id == R.id.picture_tvMediaNum) {
            onComplete()
            return
        }
        if (id == R.id.titleViewBg) {
            if (config.isAutomaticTitleRecyclerTop) {
                val intervalTime = 500
                if (SystemClock.uptimeMillis() - intervalClickTime < intervalTime) {
                    if (mAdapter.getItemCount() > 0) {
                        mRecyclerView.scrollToPosition(0)
                    }
                } else {
                    intervalClickTime = SystemClock.uptimeMillis()
                }
            }
        }
    }

    /**
     * Preview
     */
    private fun onPreview() {
        val selectedImages: List<LocalMedia?> = mAdapter.getSelectedData()
        val medias: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
        val size = selectedImages.size
        for (i in 0 until size) {
            val media: LocalMedia? = selectedImages[i]
            medias.add(media)
        }
        val bundle = Bundle()
        bundle.putParcelableArrayList(PictureConfig.EXTRA_PREVIEW_SELECT_LIST, medias as ArrayList<out Parcelable?>)
        bundle.putParcelableArrayList(PictureConfig.EXTRA_SELECT_LIST, selectedImages as ArrayList<out Parcelable?>)
        bundle.putBoolean(PictureConfig.EXTRA_BOTTOM_PREVIEW, true)
        bundle.putBoolean(PictureConfig.EXTRA_CHANGE_ORIGINAL, config.isCheckOriginalImage)
        bundle.putBoolean(PictureConfig.EXTRA_SHOW_CAMERA, mAdapter.isShowCamera())
        bundle.putString(PictureConfig.EXTRA_IS_CURRENT_DIRECTORY, mTvPictureTitle.getText().toString())
        JumpUtils.startPicturePreviewActivity(getContext(), config.isWeChatStyle, bundle,
                if (config.selectionMode === PictureConfig.SINGLE) UCrop.REQUEST_CROP else UCrop.REQUEST_MULTI_CROP)
        overridePendingTransition(if (config.windowAnimationStyle != null
                && config.windowAnimationStyle.activityPreviewEnterAnimation !== 0) config.windowAnimationStyle.activityPreviewEnterAnimation else R.anim.picture_anim_enter,
                R.anim.picture_anim_fade_in)
    }

    /**
     * Complete
     */
    private fun onComplete() {
        val result: List<LocalMedia?> = mAdapter.getSelectedData()
        val size = result.size
        val image: LocalMedia? = if (result.size > 0) result[0] else null
        val mimeType = if (image != null) image.getMimeType() else ""
        val isHasImage: Boolean = PictureMimeType.isHasImage(mimeType)
        if (config.isWithVideoImage) {
            var videoSize = 0
            var imageSize = 0
            for (i in 0 until size) {
                val media: LocalMedia? = result[i]
                if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    videoSize++
                } else {
                    imageSize++
                }
            }
            if (config.selectionMode === PictureConfig.MULTIPLE) {
                if (config.minSelectNum > 0) {
                    if (imageSize < config.minSelectNum) {
                        showPromptDialog(getString(R.string.picture_min_img_num, config.minSelectNum))
                        return
                    }
                }
                if (config.minVideoSelectNum > 0) {
                    if (videoSize < config.minVideoSelectNum) {
                        showPromptDialog(getString(R.string.picture_min_video_num, config.minVideoSelectNum))
                        return
                    }
                }
            }
        } else {
            if (config.selectionMode === PictureConfig.MULTIPLE) {
                if (PictureMimeType.isHasImage(mimeType) && config.minSelectNum > 0 && size < config.minSelectNum) {
                    val str: String = getString(R.string.picture_min_img_num, config.minSelectNum)
                    showPromptDialog(str)
                    return
                }
                if (PictureMimeType.isHasVideo(mimeType) && config.minVideoSelectNum > 0 && size < config.minVideoSelectNum) {
                    val str: String = getString(R.string.picture_min_video_num, config.minVideoSelectNum)
                    showPromptDialog(str)
                    return
                }
            }
        }
        if (config.returnEmpty && size == 0) {
            if (config.selectionMode === PictureConfig.MULTIPLE) {
                if (config.minSelectNum > 0 && size < config.minSelectNum) {
                    val str: String = getString(R.string.picture_min_img_num, config.minSelectNum)
                    showPromptDialog(str)
                    return
                }
                if (config.minVideoSelectNum > 0 && size < config.minVideoSelectNum) {
                    val str: String = getString(R.string.picture_min_video_num, config.minVideoSelectNum)
                    showPromptDialog(str)
                    return
                }
            }
            if (PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onResult(result)
            } else {
                val intent: Intent = PictureSelector.putIntentResult(result)
                setResult(RESULT_OK, intent)
            }
            closeActivity()
            return
        }
        if (config.isCheckOriginalImage) {
            onResult(result)
            return
        }
        if (config.chooseMode === PictureMimeType.ofAll() && config.isWithVideoImage) {
            bothMimeTypeWith(isHasImage, result)
        } else {
            separateMimeTypeWith(isHasImage, result)
        }
    }

    /**
     * They are different types of processing
     *
     * @param isHasImage
     * @param images
     */
    private fun bothMimeTypeWith(isHasImage: Boolean, images: List<LocalMedia?>) {
        val image: LocalMedia = (if (images.size > 0) images[0] else null) ?: return
        if (config.enableCrop) {
            if (config.selectionMode === PictureConfig.SINGLE && isHasImage) {
                config.originalPath = image.getPath()
                startCrop(config.originalPath, image.getMimeType())
            } else {
                val cuts: ArrayList<CutInfo> = ArrayList<CutInfo>()
                val count = images.size
                var imageNum = 0
                for (i in 0 until count) {
                    val media: LocalMedia? = images[i]
                    if (media == null
                            || TextUtils.isEmpty(media.getPath())) {
                        continue
                    }
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
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
                    onResult(images)
                } else {
                    startCrop(cuts)
                }
            }
        } else if (config.isCompress) {
            val size = images.size
            var imageNum = 0
            for (i in 0 until size) {
                val media: LocalMedia? = images[i]
                if (PictureMimeType.isHasImage(media.getMimeType())) {
                    imageNum++
                    break
                }
            }
            if (imageNum <= 0) {
                onResult(images)
            } else {
                compressImage(images)
            }
        } else {
            onResult(images)
        }
    }

    /**
     * Same type of image or video processing logic
     *
     * @param isHasImage
     * @param images
     */
    private fun separateMimeTypeWith(isHasImage: Boolean, images: List<LocalMedia?>) {
        val image: LocalMedia = (if (images.size > 0) images[0] else null) ?: return
        if (config.enableCrop && isHasImage) {
            if (config.selectionMode === PictureConfig.SINGLE) {
                config.originalPath = image.getPath()
                startCrop(config.originalPath, image.getMimeType())
            } else {
                val cuts: ArrayList<CutInfo> = ArrayList<CutInfo>()
                val count = images.size
                for (i in 0 until count) {
                    val media: LocalMedia? = images[i]
                    if (media == null
                            || TextUtils.isEmpty(media.getPath())) {
                        continue
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
                startCrop(cuts)
            }
        } else if (config.isCompress
                && isHasImage) {
            compressImage(images)
        } else {
            onResult(images)
        }
    }

    /**
     * Play Audio
     *
     * @param path
     */
    private fun AudioDialog(path: String) {
        if (!isFinishing()) {
            audioDialog = PictureCustomDialog(getContext(), R.layout.picture_audio_dialog)
            if (audioDialog.getWindow() != null) {
                audioDialog.getWindow().setWindowAnimations(R.style.Picture_Theme_Dialog_AudioStyle)
            }
            mTvMusicStatus = audioDialog.findViewById(R.id.tv_musicStatus)
            mTvMusicTime = audioDialog.findViewById(R.id.tv_musicTime)
            musicSeekBar = audioDialog.findViewById(R.id.musicSeekBar)
            mTvMusicTotal = audioDialog.findViewById(R.id.tv_musicTotal)
            mTvPlayPause = audioDialog.findViewById(R.id.tv_PlayPause)
            mTvStop = audioDialog.findViewById(R.id.tv_Stop)
            mTvQuit = audioDialog.findViewById(R.id.tv_Quit)
            if (mHandler != null) {
                mHandler.postDelayed({ initPlayer(path) }, 30)
            }
            mTvPlayPause.setOnClickListener(AudioOnClick(path))
            mTvStop.setOnClickListener(AudioOnClick(path))
            mTvQuit.setOnClickListener(AudioOnClick(path))
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

    /**
     * init Player
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
     * Audio Click
     */
    inner class AudioOnClick(private val path: String) : View.OnClickListener {
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
                    mHandler.postDelayed({ stop(path) }, 30)
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
     * Play Audio
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
        if (!isPlayAudio) {
            if (mHandler != null) {
                mHandler.post(mRunnable)
            }
            isPlayAudio = true
        }
    }

    /**
     * Audio Stop
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
     * Audio Pause
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

    fun onItemClick(position: Int, isCameraFolder: Boolean, bucketId: Long, folderName: String?, data: List<LocalMedia?>?) {
        val camera = config.isCamera && isCameraFolder
        mAdapter.setShowCamera(camera)
        mTvPictureTitle.setText(folderName)
        val currentBucketId: Long = ValueOf.toLong(mTvPictureTitle.getTag(R.id.view_tag))
        mTvPictureTitle.setTag(R.id.view_count_tag, if (folderWindow.getFolder(position) != null) folderWindow.getFolder(position).getImageNum() else 0)
        if (config.isPageStrategy) {
            if (currentBucketId != bucketId) {
                setLastCacheFolderData()
                val isCurrentCacheFolderData = isCurrentCacheFolderData(position)
                if (!isCurrentCacheFolderData) {
                    mPage = 1
                    showPleaseDialog()
                    LocalMediaPageLoader.getInstance(getContext(), config).loadPageMediaData(bucketId, mPage,
                            OnQueryDataResultListener<LocalMedia> { result, currentPage, isHasMore ->
                                isHasMore = isHasMore
                                if (!isFinishing()) {
                                    if (result.size() === 0) {
                                        mAdapter.clear()
                                    }
                                    mAdapter.bindData(result)
                                    mRecyclerView.onScrolled(0, 0)
                                    mRecyclerView.smoothScrollToPosition(0)
                                    dismissDialog()
                                }
                            } as OnQueryDataResultListener<LocalMedia?>?)
                }
            }
        } else {
            mAdapter.bindData(data)
            mRecyclerView.smoothScrollToPosition(0)
        }
        mTvPictureTitle.setTag(R.id.view_tag, bucketId)
        folderWindow.dismiss()
    }

    /**
     * Before switching directories, set the current directory cache
     */
    private fun setLastCacheFolderData() {
        val oldPosition: Int = ValueOf.toInt(mTvPictureTitle.getTag(R.id.view_index_tag))
        val lastFolder: LocalMediaFolder = folderWindow.getFolder(oldPosition)
        lastFolder.setData(mAdapter.getData())
        lastFolder.setCurrentDataPage(mPage)
        lastFolder.setHasMore(isHasMore)
    }

    /**
     * Does the current album have a cache
     *
     * @param position
     */
    private fun isCurrentCacheFolderData(position: Int): Boolean {
        mTvPictureTitle.setTag(R.id.view_index_tag, position)
        val currentFolder: LocalMediaFolder = folderWindow.getFolder(position)
        if (currentFolder != null && currentFolder.getData() != null && currentFolder.getData().size() > 0) {
            mAdapter.bindData(currentFolder.getData())
            mPage = currentFolder.getCurrentDataPage()
            isHasMore = currentFolder.isHasMore()
            mRecyclerView.smoothScrollToPosition(0)
            return true
        }
        return false
    }

    fun onTakePhoto() {
        // Check the permissions
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            if (PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                startCamera()
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

    fun onChange(selectData: List<LocalMedia?>) {
        changeImageNumber(selectData)
    }

    fun onPictureClick(media: LocalMedia, position: Int) {
        if (config.selectionMode === PictureConfig.SINGLE && config.isSingleDirectReturn) {
            val list: MutableList<LocalMedia> = ArrayList<LocalMedia>()
            list.add(media)
            if (config.enableCrop && PictureMimeType.isHasImage(media.getMimeType()) && !config.isCheckOriginalImage) {
                mAdapter.bindSelectData(list)
                startCrop(media.getPath(), media.getMimeType())
            } else {
                handlerResult(list)
            }
        } else {
            val data: List<LocalMedia> = mAdapter.getData()
            startPreview(data, position)
        }
    }

    /**
     * preview image and video
     *
     * @param previewImages
     * @param position
     */
    fun startPreview(previewImages: List<LocalMedia>, position: Int) {
        val media: LocalMedia = previewImages[position]
        val mimeType: String = media.getMimeType()
        val bundle = Bundle()
        val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
        if (PictureMimeType.isHasVideo(mimeType)) {
            // video
            if (config.selectionMode === PictureConfig.SINGLE && !config.enPreviewVideo) {
                result.add(media)
                onResult(result)
            } else {
                if (PictureSelectionConfig.customVideoPlayCallback != null) {
                    PictureSelectionConfig.customVideoPlayCallback.startPlayVideo(media)
                } else {
                    bundle.putParcelable(PictureConfig.EXTRA_MEDIA_KEY, media)
                    JumpUtils.startPictureVideoPlayActivity(getContext(), bundle, PictureConfig.PREVIEW_VIDEO_CODE)
                }
            }
        } else if (PictureMimeType.isHasAudio(mimeType)) {
            // audio
            if (config.selectionMode === PictureConfig.SINGLE) {
                result.add(media)
                onResult(result)
            } else {
                AudioDialog(media.getPath())
            }
        } else {
            // image
            val selectedData: List<LocalMedia?> = mAdapter.getSelectedData()
            ImagesObservable.getInstance().savePreviewMediaData(ArrayList<Any?>(previewImages))
            bundle.putParcelableArrayList(PictureConfig.EXTRA_SELECT_LIST, selectedData as ArrayList<out Parcelable?>)
            bundle.putInt(PictureConfig.EXTRA_POSITION, position)
            bundle.putBoolean(PictureConfig.EXTRA_CHANGE_ORIGINAL, config.isCheckOriginalImage)
            bundle.putBoolean(PictureConfig.EXTRA_SHOW_CAMERA, mAdapter.isShowCamera())
            bundle.putLong(PictureConfig.EXTRA_BUCKET_ID, ValueOf.toLong(mTvPictureTitle.getTag(R.id.view_tag)))
            bundle.putInt(PictureConfig.EXTRA_PAGE, mPage)
            bundle.putParcelable(PictureConfig.EXTRA_CONFIG, config)
            bundle.putInt(PictureConfig.EXTRA_DATA_COUNT, ValueOf.toInt(mTvPictureTitle.getTag(R.id.view_count_tag)))
            bundle.putString(PictureConfig.EXTRA_IS_CURRENT_DIRECTORY, mTvPictureTitle.getText().toString())
            JumpUtils.startPicturePreviewActivity(getContext(), config.isWeChatStyle, bundle,
                    if (config.selectionMode === PictureConfig.SINGLE) UCrop.REQUEST_CROP else UCrop.REQUEST_MULTI_CROP)
            overridePendingTransition(if (config.windowAnimationStyle != null
                    && config.windowAnimationStyle.activityPreviewEnterAnimation !== 0) config.windowAnimationStyle.activityPreviewEnterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
        }
    }

    /**
     * change image selector state
     *
     * @param selectData
     */
    protected fun changeImageNumber(selectData: List<LocalMedia?>) {
        val enable = selectData.size != 0
        if (enable) {
            mTvPictureOk.setEnabled(true)
            mTvPictureOk.setSelected(true)
            mTvPicturePreview.setEnabled(true)
            mTvPicturePreview.setSelected(true)
            if (config.style != null) {
                if (config.style.pictureCompleteTextColor !== 0) {
                    mTvPictureOk.setTextColor(config.style.pictureCompleteTextColor)
                }
                if (config.style.picturePreviewTextColor !== 0) {
                    mTvPicturePreview.setTextColor(config.style.picturePreviewTextColor)
                }
            }
            if (config.style != null && !TextUtils.isEmpty(config.style.picturePreviewText)) {
                mTvPicturePreview.setText(config.style.picturePreviewText)
            } else {
                mTvPicturePreview.setText(getString(R.string.picture_preview_num, selectData.size))
            }
            if (numComplete) {
                initCompleteText(selectData.size)
            } else {
                if (!isStartAnimation) {
                    mTvPictureImgNum.startAnimation(animation)
                }
                mTvPictureImgNum.setVisibility(View.VISIBLE)
                mTvPictureImgNum.setText(selectData.size.toString())
                if (config.style != null && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mTvPictureOk.setText(config.style.pictureCompleteText)
                } else {
                    mTvPictureOk.setText(getString(R.string.picture_completed))
                }
                isStartAnimation = false
            }
        } else {
            mTvPictureOk.setEnabled(config.returnEmpty)
            mTvPictureOk.setSelected(false)
            mTvPicturePreview.setEnabled(false)
            mTvPicturePreview.setSelected(false)
            if (config.style != null) {
                if (config.style.pictureUnCompleteTextColor !== 0) {
                    mTvPictureOk.setTextColor(config.style.pictureUnCompleteTextColor)
                }
                if (config.style.pictureUnPreviewTextColor !== 0) {
                    mTvPicturePreview.setTextColor(config.style.pictureUnPreviewTextColor)
                }
            }
            if (config.style != null && !TextUtils.isEmpty(config.style.pictureUnPreviewText)) {
                mTvPicturePreview.setText(config.style.pictureUnPreviewText)
            } else {
                mTvPicturePreview.setText(getString(R.string.picture_preview))
            }
            if (numComplete) {
                initCompleteText(selectData.size)
            } else {
                mTvPictureImgNum.setVisibility(View.INVISIBLE)
                if (config.style != null && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                    mTvPictureOk.setText(config.style.pictureUnCompleteText)
                } else {
                    mTvPictureOk.setText(getString(R.string.picture_please_select))
                }
            }
        }
    }

    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PictureConfig.PREVIEW_VIDEO_CODE -> if (data != null) {
                    val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
                    if (list != null && list.size > 0) {
                        onResult(list)
                    }
                }
                UCrop.REQUEST_CROP -> singleCropHandleResult(data)
                UCrop.REQUEST_MULTI_CROP -> multiCropHandleResult(data)
                PictureConfig.REQUEST_CAMERA -> dispatchHandleCamera(data)
                else -> {
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            previewCallback(data)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val throwable = data.getSerializableExtra(UCrop.EXTRA_ERROR) as Throwable
                if (throwable != null) {
                    ToastUtils.s(getContext(), throwable.message)
                }
            }
        }
    }

    /**
     * Preview interface callback processing
     *
     * @param data
     */
    private fun previewCallback(data: Intent?) {
        if (data == null) {
            return
        }
        if (config.isOriginalControl) {
            config.isCheckOriginalImage = data.getBooleanExtra(PictureConfig.EXTRA_CHANGE_ORIGINAL, config.isCheckOriginalImage)
            mCbOriginal.setChecked(config.isCheckOriginalImage)
        }
        val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
        if (mAdapter != null && list != null) {
            val isCompleteOrSelected: Boolean = data.getBooleanExtra(PictureConfig.EXTRA_COMPLETE_SELECTED, false)
            if (isCompleteOrSelected) {
                onChangeData(list)
                if (config.isWithVideoImage) {
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
                        onResult(list)
                    } else {
                        compressImage(list)
                    }
                } else {
                    // Determine if the resource is of the same type
                    val mimeType = if (list.size > 0) list[0].getMimeType() else ""
                    if (config.isCompress && PictureMimeType.isHasImage(mimeType)
                            && !config.isCheckOriginalImage) {
                        compressImage(list)
                    } else {
                        onResult(list)
                    }
                }
            } else {
                // Resources are selected on the preview page
                isStartAnimation = true
            }
            mAdapter.bindSelectData(list)
            mAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Preview the callback
     *
     * @param list
     */
    protected fun onChangeData(list: List<LocalMedia>?) {}

    /**
     * singleDirectReturn
     *
     * @param mimeType
     */
    private fun singleDirectReturnCameraHandleResult(mimeType: String) {
        val isHasImage: Boolean = PictureMimeType.isHasImage(mimeType)
        if (config.enableCrop && isHasImage) {
            config.originalPath = config.cameraPath
            startCrop(config.cameraPath, mimeType)
        } else if (config.isCompress && isHasImage) {
            val selectedImages: List<LocalMedia> = mAdapter.getSelectedData()
            compressImage(selectedImages)
        } else {
            onResult(mAdapter.getSelectedData())
        }
    }

    /**
     * Camera Handle
     *
     * @param intent
     */
    private fun dispatchHandleCamera(intent: Intent?) {
        // If PictureSelectionConfig is not empty, synchronize it
        val selectionConfig: PictureSelectionConfig? = if (intent != null) intent.getParcelableExtra(PictureConfig.EXTRA_CONFIG) else null
        if (selectionConfig != null) {
            config = selectionConfig
        }
        val isAudio = config.chooseMode === PictureMimeType.ofAudio()
        config.cameraPath = if (isAudio) getAudioPath(intent) else config.cameraPath
        if (TextUtils.isEmpty(config.cameraPath)) {
            return
        }
        showPleaseDialog()
        PictureThreadUtils.executeByIo(object : SimpleTask<LocalMedia?>() {
            fun doInBackground(): LocalMedia {
                val media = LocalMedia()
                var mimeType = if (isAudio) PictureMimeType.MIME_TYPE_AUDIO else ""
                var newSize = IntArray(2)
                var duration: Long = 0
                if (!isAudio) {
                    if (PictureMimeType.isContent(config.cameraPath)) {
                        // content: Processing rules
                        val path: String = PictureFileUtils.getPath(getContext(), Uri.parse(config.cameraPath))
                        if (!TextUtils.isEmpty(path)) {
                            val cameraFile = File(path)
                            mimeType = PictureMimeType.getMimeType(config.cameraMimeType)
                            media.setSize(cameraFile.length())
                        }
                        if (PictureMimeType.isHasImage(mimeType)) {
                            newSize = MediaUtils.getImageSizeForUrlToAndroidQ(getContext(), config.cameraPath)
                        } else if (PictureMimeType.isHasVideo(mimeType)) {
                            newSize = MediaUtils.getVideoSizeForUri(getContext(), Uri.parse(config.cameraPath))
                            duration = MediaUtils.extractDuration(getContext(), SdkVersionUtils.checkedAndroid_Q(), config.cameraPath)
                        }
                        val lastIndexOf: Int = config.cameraPath.lastIndexOf("/") + 1
                        media.setId(if (lastIndexOf > 0) ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) else -1)
                        media.setRealPath(path)
                        // Custom photo has been in the application sandbox into the file
                        val mediaPath: String? = if (intent != null) intent.getStringExtra(PictureConfig.EXTRA_MEDIA_PATH) else null
                        media.setAndroidQToPath(mediaPath)
                    } else {
                        val cameraFile: File = File(config.cameraPath)
                        mimeType = PictureMimeType.getMimeType(config.cameraMimeType)
                        media.setSize(cameraFile.length())
                        if (PictureMimeType.isHasImage(mimeType)) {
                            val degree: Int = PictureFileUtils.readPictureDegree(getContext(), config.cameraPath)
                            BitmapUtils.rotateImage(degree, config.cameraPath)
                            newSize = MediaUtils.getImageSizeForUrl(config.cameraPath)
                        } else if (PictureMimeType.isHasVideo(mimeType)) {
                            newSize = MediaUtils.getVideoSizeForUrl(config.cameraPath)
                            duration = MediaUtils.extractDuration(getContext(), SdkVersionUtils.checkedAndroid_Q(), config.cameraPath)
                        }
                        // Taking a photo generates a temporary id
                        media.setId(System.currentTimeMillis())
                    }
                    media.setPath(config.cameraPath)
                    media.setDuration(duration)
                    media.setMimeType(mimeType)
                    media.setWidth(newSize[0])
                    media.setHeight(newSize[1])
                    if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasVideo(media.getMimeType())) {
                        media.setParentFolderName(Environment.DIRECTORY_MOVIES)
                    } else {
                        media.setParentFolderName(PictureMimeType.CAMERA)
                    }
                    media.setChooseModel(config.chooseMode)
                    val bucketId: Long = MediaUtils.getCameraFirstBucketId(getContext())
                    media.setBucketId(bucketId)
                    // The width and height of the image are reversed if there is rotation information
                    MediaUtils.setOrientationSynchronous(getContext(), media, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH)
                }
                return media
            }

            fun onSuccess(result: LocalMedia) {
                dismissDialog()
                // Refresh the system library
                if (!SdkVersionUtils.checkedAndroid_Q()) {
                    if (config.isFallbackVersion3) {
                        PictureMediaScannerConnection(getContext(), config.cameraPath)
                    } else {
                        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(config.cameraPath))))
                    }
                }
                // add data Adapter
                notifyAdapterData(result)
                // Solve some phone using Camera, DCIM will produce repetitive problems
                if (!SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasImage(result.getMimeType())) {
                    val lastImageId: Int = MediaUtils.getDCIMLastImageId(getContext())
                    if (lastImageId != -1) {
                        MediaUtils.removeMedia(getContext(), lastImageId)
                    }
                }
            }
        })
    }

    /**
     * Update Adapter Data
     *
     * @param media
     */
    private fun notifyAdapterData(media: LocalMedia) {
        if (mAdapter != null) {
            val isAddSameImp = isAddSameImp(if (folderWindow.getFolder(0) != null) folderWindow.getFolder(0).getImageNum() else 0)
            if (!isAddSameImp) {
                mAdapter.getData().add(0, media)
                mOpenCameraCount++
            }
            if (checkVideoLegitimacy(media)) {
                if (config.selectionMode === PictureConfig.SINGLE) {
                    dispatchHandleSingle(media)
                } else {
                    dispatchHandleMultiple(media)
                }
            }
            mAdapter.notifyItemInserted(if (config.isCamera) 1 else 0)
            mAdapter.notifyItemRangeChanged(if (config.isCamera) 1 else 0, mAdapter.getSize())
            // Solve the problem that some mobile phones do not refresh the system library timely after using Camera
            if (config.isPageStrategy) {
                manualSaveFolderForPageModel(media)
            } else {
                manualSaveFolder(media)
            }
            mTvEmpty.setVisibility(if (mAdapter.getSize() > 0 || config.isSingleDirectReturn) View.GONE else View.VISIBLE)
            // update all count
            if (folderWindow.getFolder(0) != null) {
                mTvPictureTitle.setTag(R.id.view_count_tag, folderWindow.getFolder(0).getImageNum())
            }
            allFolderSize = 0
        }
    }

    /**
     * After using Camera, MultiSelect mode handles the logic
     *
     * @param media
     */
    private fun dispatchHandleMultiple(media: LocalMedia) {
        val selectedData: List<LocalMedia> = mAdapter.getSelectedData()
        val count = selectedData.size
        val oldMimeType = if (count > 0) selectedData[0].getMimeType() else ""
        val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(oldMimeType, media.getMimeType())
        if (config.isWithVideoImage) {
            var videoSize = 0
            for (i in 0 until count) {
                val item: LocalMedia = selectedData[i]
                if (PictureMimeType.isHasVideo(item.getMimeType())) {
                    videoSize++
                }
            }
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                if (config.maxVideoSelectNum <= 0) {
                    showPromptDialog(getString(R.string.picture_rule))
                } else {
                    if (selectedData.size >= config.maxSelectNum) {
                        showPromptDialog(getString(R.string.picture_message_max_num, config.maxSelectNum))
                    } else {
                        if (videoSize < config.maxVideoSelectNum) {
                            selectedData.add(0, media)
                            mAdapter.bindSelectData(selectedData)
                        } else {
                            showPromptDialog(StringUtils.getMsg(getContext(), media.getMimeType(),
                                    config.maxVideoSelectNum))
                        }
                    }
                }
            } else {
                if (selectedData.size < config.maxSelectNum) {
                    selectedData.add(0, media)
                    mAdapter.bindSelectData(selectedData)
                } else {
                    showPromptDialog(StringUtils.getMsg(getContext(), media.getMimeType(),
                            config.maxSelectNum))
                }
            }
        } else {
            if (PictureMimeType.isHasVideo(oldMimeType) && config.maxVideoSelectNum > 0) {
                if (count < config.maxVideoSelectNum) {
                    if (mimeTypeSame || count == 0) {
                        if (selectedData.size < config.maxVideoSelectNum) {
                            selectedData.add(0, media)
                            mAdapter.bindSelectData(selectedData)
                        }
                    }
                } else {
                    showPromptDialog(StringUtils.getMsg(getContext(), oldMimeType,
                            config.maxVideoSelectNum))
                }
            } else {
                if (count < config.maxSelectNum) {
                    if (mimeTypeSame || count == 0) {
                        selectedData.add(0, media)
                        mAdapter.bindSelectData(selectedData)
                    }
                } else {
                    showPromptDialog(StringUtils.getMsg(getContext(), oldMimeType,
                            config.maxSelectNum))
                }
            }
        }
    }

    /**
     * After using the camera, the radio mode handles the logic
     *
     * @param media
     */
    private fun dispatchHandleSingle(media: LocalMedia) {
        if (config.isSingleDirectReturn) {
            val selectedData: MutableList<LocalMedia> = mAdapter.getSelectedData()
            selectedData.add(media)
            mAdapter.bindSelectData(selectedData)
            singleDirectReturnCameraHandleResult(media.getMimeType())
        } else {
            val selectedData: MutableList<LocalMedia> = mAdapter.getSelectedData()
            val mimeType = if (selectedData.size > 0) selectedData[0].getMimeType() else ""
            val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, media.getMimeType())
            if (mimeTypeSame || selectedData.size == 0) {
                singleRadioMediaImage()
                selectedData.add(media)
                mAdapter.bindSelectData(selectedData)
            }
        }
    }

    /**
     * Verify the validity of the video
     *
     * @param media
     * @return
     */
    private fun checkVideoLegitimacy(media: LocalMedia): Boolean {
        var isEnterNext = true
        if (PictureMimeType.isHasVideo(media.getMimeType())) {
            if (config.videoMinSecond > 0 && config.videoMaxSecond > 0) {
                // The user sets the minimum and maximum video length to determine whether the video is within the interval
                if (media.getDuration() < config.videoMinSecond || media.getDuration() > config.videoMaxSecond) {
                    isEnterNext = false
                    showPromptDialog(getString(R.string.picture_choose_limit_seconds, config.videoMinSecond / 1000, config.videoMaxSecond / 1000))
                }
            } else if (config.videoMinSecond > 0) {
                // The user has only set a minimum video length limit
                if (media.getDuration() < config.videoMinSecond) {
                    isEnterNext = false
                    showPromptDialog(getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                }
            } else if (config.videoMaxSecond > 0) {
                // Only the maximum length of video is set
                if (media.getDuration() > config.videoMaxSecond) {
                    isEnterNext = false
                    showPromptDialog(getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                }
            }
        }
        return isEnterNext
    }

    /**
     * Single picture clipping callback
     *
     * @param data
     */
    private fun singleCropHandleResult(data: Intent?) {
        if (data == null) {
            return
        }
        val resultUri: Uri = UCrop.getOutput(data) ?: return
        val result: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
        val cutPath = resultUri.path
        if (mAdapter != null) {
            val list: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
            if (list != null) {
                mAdapter.bindSelectData(list)
                mAdapter.notifyDataSetChanged()
            }
            val mediaList: List<LocalMedia> = mAdapter.getSelectedData()
            var media: LocalMedia? = if (mediaList != null && mediaList.size > 0) mediaList[0] else null
            if (media != null) {
                config.originalPath = media.getPath()
                media.setCutPath(cutPath)
                media.setChooseModel(config.chooseMode)
                val isCutPathEmpty: Boolean = !TextUtils.isEmpty(cutPath)
                if (SdkVersionUtils.checkedAndroid_Q()
                        && PictureMimeType.isContent(media.getPath())) {
                    if (isCutPathEmpty) {
                        media.setSize(File(cutPath).length())
                    } else {
                        media.setSize(if (!TextUtils.isEmpty(media.getRealPath())) File(media.getRealPath()).length() else 0)
                    }
                    media.setAndroidQToPath(cutPath)
                } else {
                    media.setSize(if (isCutPathEmpty) File(cutPath).length() else 0)
                }
                media.setCut(isCutPathEmpty)
                result.add(media)
                handlerResult(result)
            } else {
                // Preview screen selects the image and crop the callback
                media = if (list != null && list.size > 0) list[0] else null
                if (media != null) {
                    config.originalPath = media.getPath()
                    media.setCutPath(cutPath)
                    media.setChooseModel(config.chooseMode)
                    val isCutPathEmpty: Boolean = !TextUtils.isEmpty(cutPath)
                    if (SdkVersionUtils.checkedAndroid_Q()
                            && PictureMimeType.isContent(media.getPath())) {
                        if (isCutPathEmpty) {
                            media.setSize(File(cutPath).length())
                        } else {
                            media.setSize(if (!TextUtils.isEmpty(media.getRealPath())) File(media.getRealPath()).length() else 0)
                        }
                        media.setAndroidQToPath(cutPath)
                    } else {
                        media.setSize(if (isCutPathEmpty) File(cutPath).length() else 0)
                    }
                    media.setCut(isCutPathEmpty)
                    result.add(media)
                    handlerResult(result)
                }
            }
        }
    }

    /**
     * Multiple picture crop
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
            mAdapter.bindSelectData(list)
            mAdapter.notifyDataSetChanged()
        }
        val oldSize = if (mAdapter != null) mAdapter.getSelectedData().size() else 0
        if (oldSize == size) {
            val result: List<LocalMedia> = mAdapter.getSelectedData()
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
            // Fault-tolerant processing
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
                media.setAndroidQToPath(if (isAndroidQ) c.getCutPath() else c.getAndroidQToPath())
                if (!TextUtils.isEmpty(c.getCutPath())) {
                    media.setSize(File(c.getCutPath()).length())
                } else {
                    if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(c.getPath())) {
                        media.setSize(if (!TextUtils.isEmpty(c.getRealPath())) File(c.getRealPath()).length() else 0)
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
     * Just make sure you pick one
     */
    private fun singleRadioMediaImage() {
        val selectData: MutableList<LocalMedia> = mAdapter.getSelectedData()
        if (selectData != null
                && selectData.size > 0) {
            val media: LocalMedia = selectData[0]
            val position: Int = media.getPosition()
            selectData.clear()
            mAdapter.notifyItemChanged(position)
        }
    }

    /**
     * Manually add the photo to the list of photos and set it to select-paging mode
     *
     * @param media
     */
    private fun manualSaveFolderForPageModel(media: LocalMedia?) {
        if (media == null) {
            return
        }
        val count: Int = folderWindow.getFolderData().size()
        val allFolder: LocalMediaFolder = if (count > 0) folderWindow.getFolderData().get(0) else LocalMediaFolder()
        if (allFolder != null) {
            val totalNum: Int = allFolder.getImageNum()
            allFolder.setFirstImagePath(media.getPath())
            allFolder.setImageNum(if (isAddSameImp(totalNum)) allFolder.getImageNum() else allFolder.getImageNum() + 1)
            // Create All folder
            if (count == 0) {
                allFolder.setName(if (config.chooseMode === PictureMimeType.ofAudio()) getString(R.string.picture_all_audio) else getString(R.string.picture_camera_roll))
                allFolder.setOfAllType(config.chooseMode)
                allFolder.setCameraFolder(true)
                allFolder.setChecked(true)
                allFolder.setBucketId(-1)
                folderWindow.getFolderData().add(0, allFolder)
                // Create Camera
                val cameraFolder = LocalMediaFolder()
                cameraFolder.setName(media.getParentFolderName())
                cameraFolder.setImageNum(if (isAddSameImp(totalNum)) cameraFolder.getImageNum() else cameraFolder.getImageNum() + 1)
                cameraFolder.setFirstImagePath(media.getPath())
                cameraFolder.setBucketId(media.getBucketId())
                folderWindow.getFolderData().add(folderWindow.getFolderData().size(), cameraFolder)
            } else {
                var isCamera = false
                val newFolder = if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasVideo(media.getMimeType())) Environment.DIRECTORY_MOVIES else PictureMimeType.CAMERA
                for (i in 0 until count) {
                    val cameraFolder: LocalMediaFolder = folderWindow.getFolderData().get(i)
                    if (cameraFolder.getName().startsWith(newFolder)) {
                        media.setBucketId(cameraFolder.getBucketId())
                        cameraFolder.setFirstImagePath(config.cameraPath)
                        cameraFolder.setImageNum(if (isAddSameImp(totalNum)) cameraFolder.getImageNum() else cameraFolder.getImageNum() + 1)
                        if (cameraFolder.getData() != null && cameraFolder.getData().size() > 0) {
                            cameraFolder.getData().add(0, media)
                        }
                        isCamera = true
                        break
                    }
                }
                if (!isCamera) {
                    // There is no Camera folder locally. Create one
                    val cameraFolder = LocalMediaFolder()
                    cameraFolder.setName(media.getParentFolderName())
                    cameraFolder.setImageNum(if (isAddSameImp(totalNum)) cameraFolder.getImageNum() else cameraFolder.getImageNum() + 1)
                    cameraFolder.setFirstImagePath(media.getPath())
                    cameraFolder.setBucketId(media.getBucketId())
                    folderWindow.getFolderData().add(cameraFolder)
                    sortFolder(folderWindow.getFolderData())
                }
            }
            folderWindow.bindFolder(folderWindow.getFolderData())
        }
    }

    /**
     * Manually add the photo to the list of photos and set it to select
     *
     * @param media
     */
    private fun manualSaveFolder(media: LocalMedia) {
        try {
            val isEmpty: Boolean = folderWindow.isEmpty()
            val totalNum = if (folderWindow.getFolder(0) != null) folderWindow.getFolder(0).getImageNum() else 0
            var allFolder: LocalMediaFolder?
            if (isEmpty) {
                // All Folder
                createNewFolder(folderWindow.getFolderData())
                allFolder = if (folderWindow.getFolderData().size() > 0) folderWindow.getFolderData().get(0) else null
                if (allFolder == null) {
                    allFolder = LocalMediaFolder()
                    folderWindow.getFolderData().add(0, allFolder)
                }
            } else {
                // All Folder
                allFolder = folderWindow.getFolderData().get(0)
            }
            allFolder.setFirstImagePath(media.getPath())
            allFolder.setData(mAdapter.getData())
            allFolder.setBucketId(-1)
            allFolder.setImageNum(if (isAddSameImp(totalNum)) allFolder.getImageNum() else allFolder.getImageNum() + 1)

            // Camera
            val cameraFolder: LocalMediaFolder = getImageFolder(media.getPath(), media.getRealPath(), folderWindow.getFolderData())
            if (cameraFolder != null) {
                cameraFolder.setImageNum(if (isAddSameImp(totalNum)) cameraFolder.getImageNum() else cameraFolder.getImageNum() + 1)
                if (!isAddSameImp(totalNum)) {
                    cameraFolder.getData().add(0, media)
                }
                cameraFolder.setBucketId(media.getBucketId())
                cameraFolder.setFirstImagePath(config.cameraPath)
            }
            folderWindow.bindFolder(folderWindow.getFolderData())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Is the quantity consistent
     *
     * @return
     */
    private fun isAddSameImp(totalNum: Int): Boolean {
        return if (totalNum == 0) {
            false
        } else allFolderSize > 0 && allFolderSize < totalNum
    }

    /**
     * Update Folder
     *
     * @param imageFolders
     */
    private fun updateMediaFolder(imageFolders: List<LocalMediaFolder>, media: LocalMedia) {
        val imageFile: File = File(media.getRealPath())
        val folderFile = imageFile.parentFile ?: return
        val size = imageFolders.size
        for (i in 0 until size) {
            val folder: LocalMediaFolder = imageFolders[i]
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

    fun onBackPressed() {
        super.onBackPressed()
        if (config != null && PictureSelectionConfig.listener != null) {
            PictureSelectionConfig.listener.onCancel()
        }
        closeActivity()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (animation != null) {
            animation.cancel()
            animation = null
        }
        if (mediaPlayer != null && mHandler != null) {
            mHandler.removeCallbacks(mRunnable)
            mediaPlayer.release()
            mediaPlayer = null
        }
    }

    fun onItemClick(view: View?, position: Int) {
        when (position) {
            PhotoItemSelectedDialog.IMAGE_CAMERA -> if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, PictureConfig.TYPE_IMAGE)
                config.cameraMimeType = PictureMimeType.ofImage()
            } else {
                startOpenCamera()
            }
            PhotoItemSelectedDialog.VIDEO_CAMERA -> if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, PictureConfig.TYPE_IMAGE)
                config.cameraMimeType = PictureMimeType.ofVideo()
            } else {
                startOpenCameraVideo()
            }
            else -> {
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE ->                 // Store Permissions
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readLocalMedia()
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
                }
            PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE ->                 // Camera Permissions
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto()
                } else {
                    showPermissionsDialog(true, getString(R.string.picture_camera))
                }
            PictureConfig.APPLY_CAMERA_STORAGE_PERMISSIONS_CODE ->                 // Using the camera, retrieve the storage permission
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
                }
            PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE ->                 // Recording Permissions
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCustomCamera()
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_audio))
                }
        }
    }

    protected override fun showPermissionsDialog(isCamera: Boolean, errorMsg: String?) {
        if (isFinishing()) {
            return
        }
        val dialog = PictureCustomDialog(getContext(), R.layout.picture_wind_base_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val btn_cancel: Button = dialog.findViewById(R.id.btn_cancel)
        val btn_commit: Button = dialog.findViewById(R.id.btn_commit)
        btn_commit.setText(getString(R.string.picture_go_setting))
        val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
        val tv_content: TextView = dialog.findViewById(R.id.tv_content)
        tvTitle.setText(getString(R.string.picture_prompt))
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
            PermissionChecker.launchAppDetailsSettings(getContext())
            isEnterSetting = true
        }
        dialog.show()
    }

    /**
     * set Data Null
     *
     * @param msg
     */
    private fun showDataNull(msg: String, topErrorResId: Int) {
        if (mTvEmpty.getVisibility() == View.GONE || mTvEmpty.getVisibility() == View.INVISIBLE) {
            mTvEmpty.setCompoundDrawablesRelativeWithIntrinsicBounds(0, topErrorResId, 0, 0)
            mTvEmpty.setText(msg)
            mTvEmpty.setVisibility(View.VISIBLE)
        }
    }

    /**
     * hidden
     */
    private fun hideDataNull() {
        if (mTvEmpty.getVisibility() == View.VISIBLE) {
            mTvEmpty.setVisibility(View.GONE)
        }
    }

    companion object {
        private val TAG = PictureSelectorActivity::class.java.simpleName
    }
}