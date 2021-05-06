package com.luck.picture.lib

import android.view.View
import com.luck.picture.lib.adapter.PictureWeChatPreviewGalleryAdapter

/**
 * @author：luck
 * @date：2019-11-30 17:16
 * @describe：PictureSelector WeChatStyle
 */
class PictureSelectorPreviewWeChatStyleActivity : PicturePreviewActivity() {
    private var mPictureSendView: TextView? = null
    private var mRvGallery: RecyclerView? = null
    private var tvSelected: TextView? = null
    private var bottomLine: View? = null
    private var mGalleryAdapter: PictureWeChatPreviewGalleryAdapter? = null
    override val resourceId: Int
        get() = R.layout.picture_wechat_style_preview

    private fun goneParent() {
        if (tvMediaNum.getVisibility() === View.VISIBLE) {
            tvMediaNum.setVisibility(View.GONE)
        }
        if (mTvPictureOk.getVisibility() === View.VISIBLE) {
            mTvPictureOk.setVisibility(View.GONE)
        }
        if (!TextUtils.isEmpty(check.getText())) {
            check.setText("")
        }
    }

    protected override fun initWidgets() {
        super.initWidgets()
        goneParent()
        mRvGallery = findViewById(R.id.rv_gallery)
        bottomLine = findViewById(R.id.bottomLine)
        tvSelected = findViewById(R.id.tv_selected)
        mPictureSendView = findViewById(R.id.picture_send)
        mPictureSendView.setOnClickListener(this)
        mPictureSendView.setText(getString(R.string.picture_send))
        mCbOriginal.setTextSize(16)
        mGalleryAdapter = PictureWeChatPreviewGalleryAdapter(config)
        val layoutManager = WrapContentLinearLayoutManager(getContext())
        layoutManager.setOrientation(WrapContentLinearLayoutManager.HORIZONTAL)
        mRvGallery.setLayoutManager(layoutManager)
        mRvGallery.addItemDecoration(GridSpacingItemDecoration(Int.MAX_VALUE,
                ScreenUtils.dip2px(this, 8), false))
        mRvGallery.setAdapter(mGalleryAdapter)
        mGalleryAdapter.setItemClickListener { position, media, v ->
            if (viewPager != null && media != null) {
                if (isEqualsDirectory(media.getParentFolderName(), currentDirectory)) {
                    val newPosition: Int = if (isBottomPreview) position else if (isShowCamera) media.position - 1 else media.position
                    viewPager.setCurrentItem(newPosition)
                } else {
                    // TODO The picture is not in the album directory, click invalid
                }
            }
        }
        if (isBottomPreview) {
            if (selectData != null && selectData.size() > position) {
                val size: Int = selectData.size()
                for (i in 0 until size) {
                    val media: LocalMedia = selectData.get(i)
                    media.setChecked(false)
                }
                val media: LocalMedia = selectData.get(position)
                media.setChecked(true)
            }
        } else {
            val size = if (selectData != null) selectData.size() else 0
            for (i in 0 until size) {
                val media: LocalMedia = selectData.get(i)
                if (isEqualsDirectory(media.getParentFolderName(), currentDirectory)) {
                    media.setChecked(if (isShowCamera) media.position - 1 === position else media.position === position)
                }
            }
        }
    }

    /**
     * Is it the same directory
     *
     * @param parentFolderName
     * @param currentDirectory
     * @return
     */
    private fun isEqualsDirectory(parentFolderName: String, currentDirectory: String): Boolean {
        return (isBottomPreview
                || TextUtils.isEmpty(parentFolderName)
                || TextUtils.isEmpty(currentDirectory)
                || currentDirectory == getString(R.string.picture_camera_roll) || parentFolderName == currentDirectory)
    }

    override fun initPictureSelectorStyle() {
        super.initPictureSelectorStyle()
        if (config.style != null) {
            if (config.style.pictureCompleteBackgroundStyle !== 0) {
                mPictureSendView.setBackgroundResource(config.style.pictureCompleteBackgroundStyle)
            } else {
                mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_bg)
            }
            if (config.style.pictureRightTextSize !== 0) {
                mPictureSendView.setTextSize(config.style.pictureRightTextSize)
            }
            if (!TextUtils.isEmpty(config.style.pictureWeChatPreviewSelectedText)) {
                tvSelected.setText(config.style.pictureWeChatPreviewSelectedText)
            }
            if (config.style.pictureWeChatPreviewSelectedTextSize !== 0) {
                tvSelected.setTextSize(config.style.pictureWeChatPreviewSelectedTextSize)
            }
            if (config.style.picturePreviewBottomBgColor !== 0) {
                selectBarLayout.setBackgroundColor(config.style.picturePreviewBottomBgColor)
            } else {
                selectBarLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.picture_color_half_grey))
            }
            if (config.style.pictureCompleteTextColor !== 0) {
                mPictureSendView.setTextColor(config.style.pictureCompleteTextColor)
            } else {
                if (config.style.pictureCancelTextColor !== 0) {
                    mPictureSendView.setTextColor(config.style.pictureCancelTextColor)
                } else {
                    mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                }
            }
            if (config.style.pictureOriginalFontColor === 0) {
                mCbOriginal.setTextColor(ContextCompat
                        .getColor(this, R.color.picture_color_white))
            }
            if (config.style.pictureWeChatChooseStyle !== 0) {
                check.setBackgroundResource(config.style.pictureWeChatChooseStyle)
            } else {
                check.setBackgroundResource(R.drawable.picture_wechat_select_cb)
            }
            if (config.isOriginalControl) {
                if (config.style.pictureOriginalControlStyle === 0) {
                    mCbOriginal.setButtonDrawable(ContextCompat
                            .getDrawable(this, R.drawable.picture_original_wechat_checkbox))
                }
            }
            if (config.style.pictureWeChatLeftBackStyle !== 0) {
                pictureLeftBack.setImageResource(config.style.pictureWeChatLeftBackStyle)
            } else {
                pictureLeftBack.setImageResource(R.drawable.picture_icon_back)
            }
            if (!TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                mPictureSendView.setText(config.style.pictureUnCompleteText)
            }
        } else {
            mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_bg)
            mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
            selectBarLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.picture_color_half_grey))
            check.setBackgroundResource(R.drawable.picture_wechat_select_cb)
            pictureLeftBack.setImageResource(R.drawable.picture_icon_back)
            mCbOriginal.setTextColor(ContextCompat
                    .getColor(this, R.color.picture_color_white))
            if (config.isOriginalControl) {
                mCbOriginal.setButtonDrawable(ContextCompat
                        .getDrawable(this, R.drawable.picture_original_wechat_checkbox))
            }
        }
        onSelectNumChange(false)
    }

    override fun onClick(v: View) {
        super.onClick(v)
        val id = v.id
        if (id == R.id.picture_send) {
            val enable = selectData.size() !== 0
            if (enable) {
                mTvPictureOk.performClick()
            } else {
                btnCheck.performClick()
                val isNewEnableStatus = selectData.size() !== 0
                if (isNewEnableStatus) {
                    mTvPictureOk.performClick()
                }
            }
        }
    }

    protected override fun onUpdateSelectedChange(media: LocalMedia) {
        onChangeMediaStatus(media)
    }

    protected override fun onSelectedChange(isAddRemove: Boolean, media: LocalMedia) {
        if (isAddRemove) {
            media.setChecked(true)
            if (config.selectionMode === PictureConfig.SINGLE) {
                mGalleryAdapter.addSingleMediaToData(media)
            }
        } else {
            media.setChecked(false)
            mGalleryAdapter.removeMediaToData(media)
            if (isBottomPreview) {
                if (selectData != null && selectData.size() > position) {
                    selectData.get(position).setChecked(true)
                }
                if (mGalleryAdapter.isDataEmpty()) {
                    onActivityBackPressed()
                } else {
                    val currentItem: Int = viewPager.getCurrentItem()
                    adapter.remove(currentItem)
                    adapter.removeCacheView(currentItem)
                    position = currentItem
                    tvTitle.setText(getString(R.string.picture_preview_image_num,
                            position + 1, adapter.getSize()))
                    check.setSelected(true)
                    adapter.notifyDataSetChanged()
                }
            }
        }
        val itemCount: Int = mGalleryAdapter.getItemCount()
        if (itemCount > 5) {
            mRvGallery.smoothScrollToPosition(itemCount - 1)
        }
    }

    protected override fun onPageSelectedChange(media: LocalMedia) {
        super.onPageSelectedChange(media)
        goneParent()
        if (!config.previewEggs) {
            onChangeMediaStatus(media)
        }
    }

    /**
     * onChangeMediaStatus
     *
     * @param media
     */
    private fun onChangeMediaStatus(media: LocalMedia) {
        if (mGalleryAdapter != null) {
            val itemCount: Int = mGalleryAdapter.getItemCount()
            if (itemCount > 0) {
                var isChangeData = false
                for (i in 0 until itemCount) {
                    val item: LocalMedia = mGalleryAdapter.getItem(i)
                    if (item == null || TextUtils.isEmpty(item.getPath())) {
                        continue
                    }
                    val isOldChecked: Boolean = item.isChecked()
                    val isNewChecked = item.getPath().equals(media.getPath()) || item.getId() === media.getId()
                    if (!isChangeData) {
                        isChangeData = isOldChecked && !isNewChecked || !isOldChecked && isNewChecked
                    }
                    item.setChecked(isNewChecked)
                }
                if (isChangeData) {
                    mGalleryAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    protected override fun onSelectNumChange(isRefresh: Boolean) {
        if (mPictureSendView == null) {
            return
        }
        goneParent()
        val enable = selectData.size() !== 0
        if (enable) {
            initCompleteText(selectData.size())
            if (mRvGallery.getVisibility() == View.GONE) {
                mRvGallery.animate().alpha(1f).setDuration(ALPHA_DURATION.toLong()).setInterpolator(AccelerateInterpolator())
                mRvGallery.setVisibility(View.VISIBLE)
                bottomLine!!.animate().alpha(1f).setDuration(ALPHA_DURATION.toLong()).interpolator = AccelerateInterpolator()
                bottomLine!!.visibility = View.VISIBLE
                // 重置一片内存区域 不然在其他地方添加也影响这里的数量
                mGalleryAdapter.setNewData(selectData)
            }
            if (config.style != null) {
                if (config.style.pictureCompleteTextColor !== 0) {
                    mPictureSendView.setTextColor(config.style.pictureCompleteTextColor)
                }
                if (config.style.pictureCompleteBackgroundStyle !== 0) {
                    mPictureSendView.setBackgroundResource(config.style.pictureCompleteBackgroundStyle)
                }
            } else {
                mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_bg)
            }
        } else {
            if (config.style != null && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                mPictureSendView.setText(config.style.pictureUnCompleteText)
            } else {
                mPictureSendView.setText(getString(R.string.picture_send))
            }
            mRvGallery.animate().alpha(0f).setDuration(ALPHA_DURATION.toLong()).setInterpolator(AccelerateInterpolator())
            mRvGallery.setVisibility(View.GONE)
            bottomLine!!.animate().alpha(0f).setDuration(ALPHA_DURATION.toLong()).interpolator = AccelerateInterpolator()
            bottomLine!!.visibility = View.GONE
        }
    }

    /**
     * initCompleteText
     */
    protected override fun initCompleteText(startCount: Int) {
        val isNotEmptyStyle = config.style != null
        if (config.isWithVideoImage) {
            // 混选模式
            if (config.selectionMode === PictureConfig.SINGLE) {
                if (startCount <= 0) {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send))
                } else {
                    val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                    if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                        mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, selectData.size(), 1))
                    } else {
                        mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureCompleteText)) config.style.pictureCompleteText else getString(R.string.picture_send))
                    }
                }
            } else {
                val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText,
                            selectData.size(), config.maxSelectNum))
                } else {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send_num, selectData.size(),
                            config.maxSelectNum))
                }
            }
        } else {
            val mimeType: String = selectData.get(0).getMimeType()
            val maxSize: Int = if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) config.maxVideoSelectNum else config.maxSelectNum
            if (config.selectionMode === PictureConfig.SINGLE) {
                if (startCount <= 0) {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send))
                } else {
                    val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                    if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                        mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, selectData.size(),
                                1))
                    } else {
                        mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureCompleteText)) config.style.pictureCompleteText else getString(R.string.picture_send))
                    }
                }
            } else {
                val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, selectData.size(), maxSize))
                } else {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send_num, selectData.size(), maxSize))
                }
            }
        }
    }

    companion object {
        /**
         * alpha duration
         */
        private const val ALPHA_DURATION = 300
    }
}