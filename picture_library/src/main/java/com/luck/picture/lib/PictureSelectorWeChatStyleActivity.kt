package com.luck.picture.lib

import android.view.View
import com.luck.picture.lib.config.PictureConfig

/**
 * @author：luck
 * @date：2019-11-30 13:27
 * @describe：PictureSelector WeChatStyle
 */
class PictureSelectorWeChatStyleActivity : PictureSelectorActivity() {
    private var mPictureSendView: TextView? = null
    private var rlAlbum: RelativeLayout? = null
    override val resourceId: Int
        get() = R.layout.picture_wechat_style_selector

    protected override fun initWidgets() {
        super.initWidgets()
        rlAlbum = findViewById(R.id.rlAlbum)
        mPictureSendView = findViewById(R.id.picture_send)
        mPictureSendView.setOnClickListener(this)
        mPictureSendView.setText(getString(R.string.picture_send))
        mTvPicturePreview.setTextSize(16)
        mCbOriginal.setTextSize(16)
        val isChooseMode = config.selectionMode ===
                PictureConfig.SINGLE && config.isSingleDirectReturn
        mPictureSendView.setVisibility(if (isChooseMode) View.GONE else View.VISIBLE)
        if (rlAlbum.getLayoutParams() != null
                && rlAlbum.getLayoutParams() is RelativeLayout.LayoutParams) {
            val lp: RelativeLayout.LayoutParams = rlAlbum.getLayoutParams() as RelativeLayout.LayoutParams
            if (isChooseMode) {
                lp.addRule(RelativeLayout.CENTER_HORIZONTAL)
            } else {
                lp.addRule(RelativeLayout.RIGHT_OF, R.id.pictureLeftBack)
            }
        }
    }

    override fun initPictureSelectorStyle() {
        if (config.style != null) {
            if (config.style.pictureUnCompleteBackgroundStyle !== 0) {
                mPictureSendView.setBackgroundResource(config.style.pictureUnCompleteBackgroundStyle)
            } else {
                mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_default_bg)
            }
            if (config.style.pictureBottomBgColor !== 0) {
                mBottomLayout.setBackgroundColor(config.style.pictureBottomBgColor)
            } else {
                mBottomLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.picture_color_grey))
            }
            if (config.style.pictureUnCompleteTextColor !== 0) {
                mPictureSendView.setTextColor(config.style.pictureUnCompleteTextColor)
            } else {
                if (config.style.pictureCancelTextColor !== 0) {
                    mPictureSendView.setTextColor(config.style.pictureCancelTextColor)
                } else {
                    mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_53575e))
                }
            }
            if (config.style.pictureRightTextSize !== 0) {
                mPictureSendView.setTextSize(config.style.pictureRightTextSize)
            }
            if (config.style.pictureOriginalFontColor === 0) {
                mCbOriginal.setTextColor(ContextCompat
                        .getColor(this, R.color.picture_color_white))
            }
            if (config.isOriginalControl) {
                if (config.style.pictureOriginalControlStyle === 0) {
                    mCbOriginal.setButtonDrawable(ContextCompat
                            .getDrawable(this, R.drawable.picture_original_wechat_checkbox))
                }
            }
            if (config.style.pictureContainerBackgroundColor !== 0) {
                container!!.setBackgroundColor(config.style.pictureContainerBackgroundColor)
            }
            if (config.style.pictureWeChatTitleBackgroundStyle !== 0) {
                rlAlbum.setBackgroundResource(config.style.pictureWeChatTitleBackgroundStyle)
            } else {
                rlAlbum.setBackgroundResource(R.drawable.picture_album_bg)
            }
            if (!TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                mPictureSendView.setText(config.style.pictureUnCompleteText)
            }
        } else {
            mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_default_bg)
            rlAlbum.setBackgroundResource(R.drawable.picture_album_bg)
            mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_53575e))
            val pictureBottomBgColor: Int = AttrsUtils.getTypeValueColor(getContext(), R.attr.picture_bottom_bg)
            mBottomLayout.setBackgroundColor(if (pictureBottomBgColor != 0) pictureBottomBgColor else ContextCompat.getColor(getContext(), R.color.picture_color_grey))
            mCbOriginal.setTextColor(ContextCompat
                    .getColor(this, R.color.picture_color_white))
            val drawable: Drawable = ContextCompat.getDrawable(this, R.drawable.picture_icon_wechat_down)
            mIvArrow.setImageDrawable(drawable)
            if (config.isOriginalControl) {
                mCbOriginal.setButtonDrawable(ContextCompat
                        .getDrawable(this, R.drawable.picture_original_wechat_checkbox))
            }
        }
        super.initPictureSelectorStyle()
        goneParentView()
    }

    /**
     * Hide views that are not needed by the parent container
     */
    private fun goneParentView() {
        mTvPictureRight.setVisibility(View.GONE)
        mTvPictureImgNum.setVisibility(View.GONE)
        mTvPictureOk.setVisibility(View.GONE)
    }

    protected override fun changeImageNumber(selectData: List<LocalMedia>) {
        if (mPictureSendView == null) {
            return
        }
        val size = selectData.size
        val enable = size != 0
        if (enable) {
            mPictureSendView.setEnabled(true)
            mPictureSendView.setSelected(true)
            mTvPicturePreview.setEnabled(true)
            mTvPicturePreview.setSelected(true)
            initCompleteText(selectData)
            if (config.style != null) {
                if (config.style.pictureCompleteBackgroundStyle !== 0) {
                    mPictureSendView.setBackgroundResource(config.style.pictureCompleteBackgroundStyle)
                } else {
                    mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_bg)
                }
                if (config.style.pictureCompleteTextColor !== 0) {
                    mPictureSendView.setTextColor(config.style.pictureCompleteTextColor)
                } else {
                    mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                }
                if (config.style.picturePreviewTextColor !== 0) {
                    mTvPicturePreview.setTextColor(config.style.picturePreviewTextColor)
                } else {
                    mTvPicturePreview.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                }
                if (!TextUtils.isEmpty(config.style.picturePreviewText)) {
                    mTvPicturePreview.setText(config.style.picturePreviewText)
                } else {
                    mTvPicturePreview.setText(getString(R.string.picture_preview_num, size))
                }
            } else {
                mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_bg)
                mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                mTvPicturePreview.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                mTvPicturePreview.setText(getString(R.string.picture_preview_num, size))
            }
        } else {
            mPictureSendView.setEnabled(false)
            mPictureSendView.setSelected(false)
            mTvPicturePreview.setEnabled(false)
            mTvPicturePreview.setSelected(false)
            if (config.style != null) {
                if (config.style.pictureUnCompleteBackgroundStyle !== 0) {
                    mPictureSendView.setBackgroundResource(config.style.pictureUnCompleteBackgroundStyle)
                } else {
                    mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_default_bg)
                }
                if (config.style.pictureUnCompleteTextColor !== 0) {
                    mPictureSendView.setTextColor(config.style.pictureUnCompleteTextColor)
                } else {
                    mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_53575e))
                }
                if (config.style.pictureUnPreviewTextColor !== 0) {
                    mTvPicturePreview.setTextColor(config.style.pictureUnPreviewTextColor)
                } else {
                    mTvPicturePreview.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_9b))
                }
                if (!TextUtils.isEmpty(config.style.pictureUnCompleteText)) {
                    mPictureSendView.setText(config.style.pictureUnCompleteText)
                } else {
                    mPictureSendView.setText(getString(R.string.picture_send))
                }
                if (!TextUtils.isEmpty(config.style.pictureUnPreviewText)) {
                    mTvPicturePreview.setText(config.style.pictureUnPreviewText)
                } else {
                    mTvPicturePreview.setText(getString(R.string.picture_preview))
                }
            } else {
                mPictureSendView.setBackgroundResource(R.drawable.picture_send_button_default_bg)
                mPictureSendView.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_53575e))
                mTvPicturePreview.setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_9b))
                mTvPicturePreview.setText(getString(R.string.picture_preview))
                mPictureSendView.setText(getString(R.string.picture_send))
            }
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        val id = v.id
        if (id == R.id.picture_send) {
            if (folderWindow != null
                    && folderWindow.isShowing()) {
                folderWindow.dismiss()
            } else {
                mTvPictureOk.performClick()
            }
        }
    }

    protected fun onChangeData(list: List<LocalMedia>) {
        super.onChangeData(list)
        initCompleteText(list)
    }

    protected fun initCompleteText(list: List<LocalMedia>) {
        val size = list.size
        val isNotEmptyStyle = config.style != null
        if (config.isWithVideoImage) {
            if (config.selectionMode === PictureConfig.SINGLE) {
                if (size <= 0) {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send))
                } else {
                    val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                    if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                        mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, size, 1))
                    } else {
                        mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureCompleteText)) config.style.pictureCompleteText else getString(R.string.picture_send))
                    }
                }
            } else {
                val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, size, config.maxSelectNum))
                } else {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send_num, size, config.maxSelectNum))
                }
            }
        } else {
            val mimeType: String = list[0].getMimeType()
            val maxSize: Int = if (PictureMimeType.isHasVideo(mimeType) && config.maxVideoSelectNum > 0) config.maxVideoSelectNum else config.maxSelectNum
            if (config.selectionMode === PictureConfig.SINGLE) {
                val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, size, 1))
                } else {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureCompleteText)) config.style.pictureCompleteText else getString(R.string.picture_send))
                }
            } else {
                val isCompleteReplaceNum = isNotEmptyStyle && config.style.isCompleteReplaceNum
                if (isCompleteReplaceNum && !TextUtils.isEmpty(config.style.pictureCompleteText)) {
                    mPictureSendView.setText(java.lang.String.format(config.style.pictureCompleteText, size, maxSize))
                } else {
                    mPictureSendView.setText(if (isNotEmptyStyle && !TextUtils.isEmpty(config.style.pictureUnCompleteText)) config.style.pictureUnCompleteText else getString(R.string.picture_send_num, size, maxSize))
                }
            }
        }
    }
}