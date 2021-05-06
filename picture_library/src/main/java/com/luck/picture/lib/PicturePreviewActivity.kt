package com.luck.picture.lib

import android.net.Uri
import android.os.Handler
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.adapter.PictureSimpleFragmentAdapter
import java.util.*

/**
 * @author：luck
 * @data：2016/1/29 下午21:50
 * @描述:图片预览
 */
class PicturePreviewActivity : PictureBaseActivity(), View.OnClickListener, PictureSimpleFragmentAdapter.OnCallBackActivity {
    protected var pictureLeftBack: ImageView? = null
    protected var tvMediaNum: TextView? = null
    protected var tvTitle: TextView? = null
    protected var mTvPictureOk: TextView? = null
    protected var viewPager: PreviewViewPager? = null
    protected var position = 0
    protected var isBottomPreview = false
    private var totalNumber = 0
    protected var selectData: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
    protected var adapter: PictureSimpleFragmentAdapter? = null
    protected var animation: Animation? = null
    protected var check: TextView? = null
    protected var btnCheck: View? = null
    protected var refresh = false
    protected var index = 0
    protected var screenWidth = 0
    protected override var mHandler: Handler? = null
    protected var selectBarLayout: RelativeLayout? = null
    protected var mCbOriginal: CheckBox? = null
    protected var titleViewBg: View? = null
    protected var isShowCamera = false
    protected var currentDirectory: String? = null

    /**
     * 是否已完成选择
     */
    protected var isCompleteOrSelected = false

    /**
     * 是否改变已选的数据
     */
    protected var isChangeSelectedData = false

    /**
     * 分页码
     */
    private override var mPage = 0
    override val resourceId: Int
        get() = R.layout.picture_preview

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // 防止内存不足时activity被回收，导致图片未选中
            selectData = PictureSelector.obtainSelectorList(savedInstanceState)
            isCompleteOrSelected = savedInstanceState.getBoolean(PictureConfig.EXTRA_COMPLETE_SELECTED, false)
            isChangeSelectedData = savedInstanceState.getBoolean(PictureConfig.EXTRA_CHANGE_SELECTED_DATA, false)
            onImageChecked(position)
            onSelectNumChange(false)
        }
    }

    protected override fun initWidgets() {
        super.initWidgets()
        mHandler = Handler()
        titleViewBg = findViewById(R.id.titleViewBg)
        screenWidth = ScreenUtils.getScreenWidth(this)
        animation = AnimationUtils.loadAnimation(this, R.anim.picture_anim_modal_in)
        pictureLeftBack = findViewById(R.id.pictureLeftBack)
        viewPager = findViewById(R.id.preview_pager)
        btnCheck = findViewById(R.id.btnCheck)
        check = findViewById(R.id.check)
        pictureLeftBack!!.setOnClickListener(this)
        mTvPictureOk = findViewById(R.id.tv_ok)
        mCbOriginal = findViewById(R.id.cb_original)
        tvMediaNum = findViewById(R.id.tvMediaNum)
        selectBarLayout = findViewById(R.id.select_bar_layout)
        mTvPictureOk.setOnClickListener(this)
        tvMediaNum.setOnClickListener(this)
        tvTitle = findViewById(R.id.picture_title)
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0)
        if (numComplete) {
            initCompleteText(0)
        }
        tvMediaNum.setSelected(config.checkNumMode)
        btnCheck!!.setOnClickListener(this)
        selectData = getIntent().getParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST)
        isBottomPreview = getIntent().getBooleanExtra(PictureConfig.EXTRA_BOTTOM_PREVIEW, false)
        isShowCamera = getIntent().getBooleanExtra(PictureConfig.EXTRA_SHOW_CAMERA, config.isCamera)
        // 当前目录
        currentDirectory = getIntent().getStringExtra(PictureConfig.EXTRA_IS_CURRENT_DIRECTORY)
        val data: List<LocalMedia>
        if (isBottomPreview) {
            // 底部预览模式
            data = getIntent().getParcelableArrayListExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST)
            initViewPageAdapterData(data)
        } else {
            data = ImagesObservable.getInstance().readPreviewMediaData()
            val isEmpty = data.size == 0
            totalNumber = getIntent().getIntExtra(PictureConfig.EXTRA_DATA_COUNT, 0)
            if (config.isPageStrategy) {
                // 分页模式
                if (isEmpty) {
                    // 这种情况有可能是单例被回收了导致readPreviewMediaData();返回的数据为0，那就从第一页开始加载吧
                    setNewTitle()
                } else {
                    mPage = getIntent().getIntExtra(PictureConfig.EXTRA_PAGE, 0)
                }
                initViewPageAdapterData(data)
                loadData()
                setTitle()
            } else {
                // 普通模式
                initViewPageAdapterData(data)
                if (isEmpty) {
                    // 这种情况有可能是单例被回收了导致readPreviewMediaData();返回的数据为0，暂时自动切换成分页模式去获取数据
                    config.isPageStrategy = true
                    setNewTitle()
                    loadData()
                }
            }
        }
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                isPreviewEggs(config.previewEggs, position, positionOffsetPixels)
            }

            override fun onPageSelected(i: Int) {
                position = i
                setTitle()
                val media: LocalMedia = adapter.getItem(position) ?: return
                index = media.getPosition()
                if (!config.previewEggs) {
                    if (config.checkNumMode) {
                        check.setText(ValueOf.toString(media.getNum()))
                        notifyCheckChanged(media)
                    }
                    onImageChecked(position)
                }
                if (config.isOriginalControl) {
                    val isHasVideo: Boolean = PictureMimeType.isHasVideo(media.getMimeType())
                    mCbOriginal.setVisibility(if (isHasVideo) View.GONE else View.VISIBLE)
                    mCbOriginal.setChecked(config.isCheckOriginalImage)
                }
                onPageSelectedChange(media)
                if (config.isPageStrategy && !isBottomPreview) {
                    if (isHasMore) {
                        // 滑到adapter.getSize() - PictureConfig.MIN_PAGE_SIZE时或最后一条时预加载
                        if (position == adapter.getSize() - 1 - PictureConfig.MIN_PAGE_SIZE || position == adapter.getSize() - 1) {
                            loadMoreData()
                        }
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        // 原图
        if (config.isOriginalControl) {
            val isCheckOriginal: Boolean = getIntent()
                    .getBooleanExtra(PictureConfig.EXTRA_CHANGE_ORIGINAL, config.isCheckOriginalImage)
            mCbOriginal.setVisibility(View.VISIBLE)
            config.isCheckOriginalImage = isCheckOriginal
            mCbOriginal.setChecked(config.isCheckOriginalImage)
            mCbOriginal.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> config.isCheckOriginalImage = isChecked })
        }
    }

    /**
     * 从本地获取数据
     */
    private fun loadData() {
        val bucketId: Long = getIntent().getLongExtra(PictureConfig.EXTRA_BUCKET_ID, -1)
        mPage++
        LocalMediaPageLoader.getInstance(getContext(), config).loadPageMediaData(bucketId, mPage, config.pageSize,
                OnQueryDataResultListener<LocalMedia> { result, currentPage, isHasMore ->
                    if (!isFinishing()) {
                        isHasMore = isHasMore
                        if (isHasMore) {
                            val size: Int = result.size()
                            if (size > 0 && adapter != null) {
                                adapter.getData().addAll(result)
                                adapter.notifyDataSetChanged()
                            } else {
                                // 这种情况就是开启过滤损坏文件刚好导致某一页全是损坏的虽然result为0，但还要请求下一页数据
                                loadMoreData()
                            }
                        }
                    }
                } as OnQueryDataResultListener<LocalMedia?>?)
    }

    /**
     * 加载更多
     */
    private fun loadMoreData() {
        val bucketId: Long = getIntent().getLongExtra(PictureConfig.EXTRA_BUCKET_ID, -1)
        mPage++
        LocalMediaPageLoader.getInstance(getContext(), config).loadPageMediaData(bucketId, mPage, config.pageSize,
                OnQueryDataResultListener<LocalMedia> { result, currentPage, isHasMore ->
                    if (!isFinishing()) {
                        isHasMore = isHasMore
                        if (isHasMore) {
                            val size: Int = result.size()
                            if (size > 0 && adapter != null) {
                                adapter.getData().addAll(result)
                                adapter.notifyDataSetChanged()
                            } else {
                                // 这种情况就是开启过滤损坏文件刚好导致某一页全是损坏的虽然result为0，但还要请求下一页数据
                                loadMoreData()
                            }
                        }
                    }
                } as OnQueryDataResultListener<LocalMedia?>?)
    }

    protected override fun initCompleteText(startCount: Int) {
        val isNotEmptyStyle = config.style != null
        if (config.selectionMode === PictureConfig.SINGLE) {
            if (startCount <= 0) {
                // 未选择任何图片
                mTvPictureOk.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_please_select))
            } else {
                // 已选择
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
                // 未选择任何图片
                mTvPictureOk.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_done_front_num,
                        startCount, config.maxSelectNum))
            } else {
                // 已选择
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
     * ViewPage滑动数据变化回调
     *
     * @param media
     */
    protected fun onPageSelectedChange(media: LocalMedia?) {}

    /**
     * 动态设置相册主题
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
                pictureLeftBack!!.setImageResource(config.style.pictureLeftBackIcon)
            }
            if (config.style.picturePreviewBottomBgColor !== 0) {
                selectBarLayout.setBackgroundColor(config.style.picturePreviewBottomBgColor)
            }
            if (config.style.pictureCheckNumBgStyle !== 0) {
                tvMediaNum.setBackgroundResource(config.style.pictureCheckNumBgStyle)
            }
            if (config.style.pictureCheckedStyle !== 0) {
                check.setBackgroundResource(config.style.pictureCheckedStyle)
            }
            if (config.style.pictureUnCompleteTextColor !== 0) {
                mTvPictureOk.setTextColor(config.style.pictureUnCompleteTextColor)
            }
            if (!TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                mTvPictureOk.setText(config.style.pictureUnCompleteText)
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
        onSelectNumChange(false)
    }

    /**
     * 这里没实际意义，好处是预览图片时 滑动到屏幕一半以上可看到下一张图片是否选中了
     *
     * @param previewEggs          是否显示预览友好体验
     * @param positionOffsetPixels 滑动偏移量
     */
    private fun isPreviewEggs(previewEggs: Boolean, position: Int, positionOffsetPixels: Int) {
        if (previewEggs) {
            if (adapter.getSize() > 0) {
                val media: LocalMedia
                val num: Int
                if (positionOffsetPixels < screenWidth / 2) {
                    media = adapter.getItem(position)
                    if (media != null) {
                        check.setSelected(isSelected(media))
                        if (config.isWeChatStyle) {
                            onUpdateSelectedChange(media)
                        } else {
                            if (config.checkNumMode) {
                                num = media.getNum()
                                check.setText(ValueOf.toString(num))
                                notifyCheckChanged(media)
                                onImageChecked(position)
                            }
                        }
                    }
                } else {
                    media = adapter.getItem(position + 1)
                    if (media != null) {
                        check.setSelected(isSelected(media))
                        if (config.isWeChatStyle) {
                            onUpdateSelectedChange(media)
                        } else {
                            if (config.checkNumMode) {
                                num = media.getNum()
                                check.setText(ValueOf.toString(num))
                                notifyCheckChanged(media)
                                onImageChecked(position + 1)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化ViewPage数据
     *
     * @param list
     */
    private fun initViewPageAdapterData(list: List<LocalMedia>) {
        adapter = PictureSimpleFragmentAdapter(config, this)
        adapter.bindData(list)
        viewPager.setAdapter(adapter)
        viewPager.setCurrentItem(position)
        setTitle()
        onImageChecked(position)
        val media: LocalMedia = adapter.getItem(position)
        if (media != null) {
            index = media.getPosition()
            if (config.checkNumMode) {
                tvMediaNum.setSelected(true)
                check.setText(ValueOf.toString(media.getNum()))
                notifyCheckChanged(media)
            }
        }
    }

    /**
     * 重置标题栏和分页码
     */
    private fun setNewTitle() {
        mPage = 0
        position = 0
        setTitle()
    }

    /**
     * 设置标题
     */
    private fun setTitle() {
        if (config.isPageStrategy && !isBottomPreview) {
            tvTitle.setText(getString(R.string.picture_preview_image_num,
                    position + 1, totalNumber))
        } else {
            tvTitle.setText(getString(R.string.picture_preview_image_num,
                    position + 1, adapter.getSize()))
        }
    }

    /**
     * 选择按钮更新
     */
    private fun notifyCheckChanged(imageBean: LocalMedia?) {
        if (config.checkNumMode) {
            check.setText("")
            val size = selectData.size
            for (i in 0 until size) {
                val media: LocalMedia? = selectData[i]
                if (media.getPath().equals(imageBean.getPath())
                        || media.getId() === imageBean.getId()) {
                    imageBean.setNum(media.getNum())
                    check.setText(java.lang.String.valueOf(imageBean.getNum()))
                }
            }
        }
    }

    /**
     * 更新选择的顺序
     */
    private fun subSelectPosition() {
        run {
            var index = 0
            val len = selectData.size
            while (index < len) {
                val media: LocalMedia? = selectData[index]
                media.setNum(index + 1)
                index++
            }
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    fun onImageChecked(position: Int) {
        if (adapter.getSize() > 0) {
            val media: LocalMedia = adapter.getItem(position)
            if (media != null) {
                check.setSelected(isSelected(media))
            }
        } else {
            check.setSelected(false)
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    protected fun isSelected(image: LocalMedia): Boolean {
        val size = selectData.size
        for (i in 0 until size) {
            val media: LocalMedia? = selectData[i]
            if (media.getPath().equals(image.getPath()) || media.getId() === image.getId()) {
                return true
            }
        }
        return false
    }

    /**
     * 更新图片选择数量
     */
    protected fun onSelectNumChange(isRefresh: Boolean) {
        refresh = isRefresh
        val enable = selectData.size != 0
        if (enable) {
            mTvPictureOk.setEnabled(true)
            mTvPictureOk.setSelected(true)
            if (config.style != null) {
                if (config.style.pictureCompleteTextColor !== 0) {
                    mTvPictureOk.setTextColor(config.style.pictureCompleteTextColor)
                } else {
                    mTvPictureOk.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_fa632d))
                }
            }
            if (numComplete) {
                initCompleteText(selectData.size)
            } else {
                if (refresh) {
                    tvMediaNum.startAnimation(animation)
                }
                tvMediaNum.setVisibility(View.VISIBLE)
                tvMediaNum.setText(selectData.size.toString())
                if (config.style != null && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mTvPictureOk.setText(config.style.pictureCompleteText)
                } else {
                    mTvPictureOk.setText(getString(R.string.picture_completed))
                }
            }
        } else {
            mTvPictureOk.setEnabled(false)
            mTvPictureOk.setSelected(false)
            if (config.style != null) {
                if (config.style.pictureUnCompleteTextColor !== 0) {
                    mTvPictureOk.setTextColor(config.style.pictureUnCompleteTextColor)
                } else {
                    mTvPictureOk.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_9b))
                }
            }
            if (numComplete) {
                initCompleteText(0)
            } else {
                tvMediaNum.setVisibility(View.INVISIBLE)
                if (config.style != null && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                    mTvPictureOk.setText(config.style.pictureUnCompleteText)
                } else {
                    mTvPictureOk.setText(getString(R.string.picture_please_select))
                }
            }
        }
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.pictureLeftBack) {
            onBackPressed()
        } else if (id == R.id.tv_ok || id == R.id.tvMediaNum) {
            onComplete()
        } else if (id == R.id.btnCheck) {
            onCheckedComplete()
        }
    }

    protected fun onCheckedComplete() {
        if (adapter.getSize() > 0) {
            val image: LocalMedia = adapter.getItem(viewPager.getCurrentItem())
            val mimeType = if (selectData.size > 0) selectData[0].getMimeType() else ""
            val currentSize = selectData.size
            if (config.isWithVideoImage) {
                // 混选模式
                var videoSize = 0
                for (i in 0 until currentSize) {
                    val media: LocalMedia? = selectData[i]
                    if (PictureMimeType.isHasVideo(media.getMimeType())) {
                        videoSize++
                    }
                }
                if (image != null && PictureMimeType.isHasVideo(image.getMimeType())) {
                    if (config.maxVideoSelectNum <= 0) {
                        // 如果视频可选数量是0
                        showPromptDialog(getString(R.string.picture_rule))
                        return
                    }
                    if (selectData.size >= config.maxSelectNum && !check.isSelected()) {
                        showPromptDialog(getString(R.string.picture_message_max_num, config.maxSelectNum))
                        return
                    }
                    if (videoSize >= config.maxVideoSelectNum && !check.isSelected()) {
                        // 如果选择的是视频
                        showPromptDialog(StringUtils.getMsg(getContext(), image.getMimeType(), config.maxVideoSelectNum))
                        return
                    }
                    if (!check.isSelected() && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                        // 视频小于最低指定的长度
                        showPromptDialog(getContext().getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                        return
                    }
                    if (!check.isSelected() && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                        // 视频时长超过了指定的长度
                        showPromptDialog(getContext().getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                        return
                    }
                }
                if (image != null && PictureMimeType.isHasImage(image.getMimeType())) {
                    if (selectData.size >= config.maxSelectNum && !check.isSelected()) {
                        showPromptDialog(getString(R.string.picture_message_max_num, config.maxSelectNum))
                        return
                    }
                }
            } else {
                // 非混选模式
                if (!TextUtils.isEmpty(mimeType)) {
                    val mimeTypeSame: Boolean = PictureMimeType.isMimeTypeSame(mimeType, image.getMimeType())
                    if (!mimeTypeSame) {
                        showPromptDialog(getString(R.string.picture_rule))
                        return
                    }
                }
                if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) {
                    if (currentSize >= config.maxVideoSelectNum && !check.isSelected()) {
                        // 如果先选择的是视频
                        showPromptDialog(StringUtils.getMsg(getContext(), mimeType, config.maxVideoSelectNum))
                        return
                    }
                    if (!check.isSelected() && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                        // 视频小于最低指定的长度
                        showPromptDialog(getContext().getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                        return
                    }
                    if (!check.isSelected() && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                        // 视频时长超过了指定的长度
                        showPromptDialog(getContext().getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                        return
                    }
                } else {
                    if (currentSize >= config.maxSelectNum && !check.isSelected()) {
                        showPromptDialog(StringUtils.getMsg(getContext(), mimeType, config.maxSelectNum))
                        return
                    }
                    if (PictureMimeType.isHasVideo(image.getMimeType())) {
                        if (!check.isSelected() && config.videoMinSecond > 0 && image.getDuration() < config.videoMinSecond) {
                            // 视频小于最低指定的长度
                            showPromptDialog(getContext().getString(R.string.picture_choose_min_seconds, config.videoMinSecond / 1000))
                            return
                        }
                        if (!check.isSelected() && config.videoMaxSecond > 0 && image.getDuration() > config.videoMaxSecond) {
                            // 视频时长超过了指定的长度
                            showPromptDialog(getContext().getString(R.string.picture_choose_max_seconds, config.videoMaxSecond / 1000))
                            return
                        }
                    }
                }
            }
            // 刷新图片列表中图片状态
            val isChecked: Boolean
            if (!check.isSelected()) {
                isChecked = true
                check.setSelected(true)
                check.startAnimation(animation)
            } else {
                isChecked = false
                check.setSelected(false)
            }
            isChangeSelectedData = true
            if (isChecked) {
                VoiceUtils.getInstance().play()
                // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                if (config.selectionMode === PictureConfig.SINGLE) {
                    selectData.clear()
                }

                // 如果宽高为0，重新获取宽高
                if (image.getWidth() === 0 || image.getHeight() === 0) {
                    var width = 0
                    var height = 0
                    image.setOrientation(-1)
                    if (PictureMimeType.isContent(image.getPath())) {
                        if (PictureMimeType.isHasVideo(image.getMimeType())) {
                            val size: IntArray = MediaUtils.getVideoSizeForUri(getContext(), Uri.parse(image.getPath()))
                            width = size[0]
                            height = size[1]
                        } else if (PictureMimeType.isHasImage(image.getMimeType())) {
                            val size: IntArray = MediaUtils.getImageSizeForUri(getContext(), Uri.parse(image.getPath()))
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

                // 如果有旋转信息图片宽高则是相反
                MediaUtils.setOrientationAsynchronous(getContext(), image, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH, null)
                selectData.add(image)
                onSelectedChange(true, image)
                image.setNum(selectData.size)
                if (config.checkNumMode) {
                    check.setText(java.lang.String.valueOf(image.getNum()))
                }
            } else {
                val size = selectData.size
                for (i in 0 until size) {
                    val media: LocalMedia? = selectData[i]
                    if (media.getPath().equals(image.getPath())
                            || media.getId() === image.getId()) {
                        selectData.remove(media)
                        onSelectedChange(false, image)
                        subSelectPosition()
                        notifyCheckChanged(media)
                        break
                    }
                }
            }
            onSelectNumChange(true)
        }
    }

    /**
     * 选中或是移除
     *
     * @param isAddRemove
     * @param media
     */
    protected fun onSelectedChange(isAddRemove: Boolean, media: LocalMedia?) {}

    /**
     * 更新选中或是移除状态
     *
     * @param media
     */
    protected fun onUpdateSelectedChange(media: LocalMedia?) {}
    protected fun onComplete() {
        // 如果设置了图片最小选择数量，则判断是否满足条件
        val size = selectData.size
        val image: LocalMedia? = if (selectData.size > 0) selectData[0] else null
        val mimeType = if (image != null) image.getMimeType() else ""
        if (config.isWithVideoImage) {
            // 混选模式
            var videoSize = 0
            var imageSize = 0
            val currentSize = selectData.size
            for (i in 0 until currentSize) {
                val media: LocalMedia? = selectData[i]
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
            // 单选模式(同类型)
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
        isCompleteOrSelected = true
        isChangeSelectedData = true
        if (config.isCheckOriginalImage) {
            onBackPressed()
            return
        }
        if (config.chooseMode === PictureMimeType.ofAll() && config.isWithVideoImage) {
            bothMimeTypeWith(mimeType, image)
        } else {
            separateMimeTypeWith(mimeType, image)
        }
    }

    /**
     * 两者不同类型的处理方式
     *
     * @param mimeType
     * @param image
     */
    private fun bothMimeTypeWith(mimeType: String, image: LocalMedia?) {
        if (config.enableCrop) {
            isCompleteOrSelected = false
            val isHasImage: Boolean = PictureMimeType.isHasImage(mimeType)
            if (config.selectionMode === PictureConfig.SINGLE && isHasImage) {
                config.originalPath = image.getPath()
                startCrop(config.originalPath, image.getMimeType())
            } else {
                // 是图片和选择压缩并且是多张，调用批量压缩
                val cuts: ArrayList<CutInfo> = ArrayList<CutInfo>()
                val count = selectData.size
                var imageNum = 0
                for (i in 0 until count) {
                    val media: LocalMedia? = selectData[i]
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
                    cutInfo.setAndroidQToPath(media.getAndroidQToPath())
                    cutInfo.setId(media.getId())
                    cutInfo.setDuration(media.getDuration())
                    cutInfo.setRealPath(media.getRealPath())
                    cuts.add(cutInfo)
                }
                if (imageNum <= 0) {
                    // 全是视频
                    isCompleteOrSelected = true
                    onBackPressed()
                } else {
                    // 图片和视频共存
                    startCrop(cuts)
                }
            }
        } else {
            onBackPressed()
        }
    }

    /**
     * 同一类型的图片或视频处理逻辑
     *
     * @param mimeType
     * @param image
     */
    private fun separateMimeTypeWith(mimeType: String, image: LocalMedia?) {
        if (config.enableCrop && PictureMimeType.isHasImage(mimeType)) {
            isCompleteOrSelected = false
            if (config.selectionMode === PictureConfig.SINGLE) {
                config.originalPath = image.getPath()
                startCrop(config.originalPath, image.getMimeType())
            } else {
                // 是图片和选择压缩并且是多张，调用批量压缩
                val cuts: ArrayList<CutInfo> = ArrayList<CutInfo>()
                val count = selectData.size
                for (i in 0 until count) {
                    val media: LocalMedia? = selectData[i]
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
                    cutInfo.setAndroidQToPath(media.getAndroidQToPath())
                    cutInfo.setId(media.getId())
                    cutInfo.setDuration(media.getDuration())
                    cutInfo.setRealPath(media.getRealPath())
                    cuts.add(cutInfo)
                }
                startCrop(cuts)
            }
        } else {
            onBackPressed()
        }
    }

    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_MULTI_CROP -> {
                    // 裁剪数据
                    val list: List<CutInfo?> = UCrop.getMultipleOutput(data)
                    data.putParcelableArrayListExtra(UCrop.Options.EXTRA_OUTPUT_URI_LIST,
                            list as ArrayList<out Parcelable?>)
                    // 已选数量
                    data.putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST,
                            selectData as ArrayList<out Parcelable?>)
                    setResult(RESULT_OK, data)
                    finish()
                }
                UCrop.REQUEST_CROP -> {
                    if (data != null) {
                        data.putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST,
                                selectData as ArrayList<out Parcelable?>)
                        setResult(RESULT_OK, data)
                    }
                    finish()
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val throwable = data.getSerializableExtra(UCrop.EXTRA_ERROR) as Throwable
            ToastUtils.s(getContext(), throwable.message)
        }
    }

    fun onBackPressed() {
        updateResult()
        if (config.windowAnimationStyle != null
                && config.windowAnimationStyle.activityPreviewExitAnimation !== 0) {
            finish()
            overridePendingTransition(0, if (config.windowAnimationStyle != null
                    && config.windowAnimationStyle.activityPreviewExitAnimation !== 0) config.windowAnimationStyle.activityPreviewExitAnimation else R.anim.picture_anim_exit)
        } else {
            closeActivity()
        }
    }

    /**
     * 更新选中数据
     */
    private fun updateResult() {
        val intent = Intent()
        if (isChangeSelectedData) {
            intent.putExtra(PictureConfig.EXTRA_COMPLETE_SELECTED, isCompleteOrSelected)
            intent.putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST,
                    selectData as ArrayList<out Parcelable?>)
        }
        // 把是否原图标识返回，主要用于开启了开发者选项不保留活动或内存不足时 原图选中状态没有全局同步问题
        if (config.isOriginalControl) {
            intent.putExtra(PictureConfig.EXTRA_CHANGE_ORIGINAL, config.isCheckOriginalImage)
        }
        setResult(RESULT_CANCELED, intent)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(PictureConfig.EXTRA_COMPLETE_SELECTED, isCompleteOrSelected)
        outState.putBoolean(PictureConfig.EXTRA_CHANGE_SELECTED_DATA, isChangeSelectedData)
        PictureSelector.saveSelectorList(outState, selectData)
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (!isOnSaveInstanceState) {
            ImagesObservable.getInstance().clearPreviewMediaData()
        }
        if (mHandler != null) {
            mHandler!!.removeCallbacksAndMessages(null)
            mHandler = null
        }
        if (animation != null) {
            animation.cancel()
            animation = null
        }
        if (adapter != null) {
            adapter.clear()
        }
    }

    fun onActivityBackPressed() {
        onBackPressed()
    }

    companion object {
        private val TAG = PicturePreviewActivity::class.java.simpleName
    }
}