package com.luck.picture.lib.style

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

/**
 * @author：luck
 * @date：2019-11-22 17:24
 * @describe：相册动态样式参数设置
 */
class PictureParameterStyle : Parcelable {
    /**
     * 是否改变状态栏字体颜色 黑白切换
     */
    var isChangeStatusBarFontColor = false

    /**
     * 是否开启 已完成(0/9) 模式
     */
    var isOpenCompletedNumStyle = false

    /**
     * 是否开启QQ 数字选择风格
     */
    var isOpenCheckNumStyle = false

    /**
     * 状态栏色值
     */
    @ColorInt
    var pictureStatusBarColor = 0

    /**
     * 标题栏背景色
     */
    @ColorInt
    var pictureTitleBarBackgroundColor = 0

    /**
     * 相册父容器背景色
     */
    @ColorInt
    var pictureContainerBackgroundColor = 0

    /**
     * 相册标题色值
     */
    @ColorInt
    var pictureTitleTextColor = 0

    /**
     * 相册标题字体大小
     */
    var pictureTitleTextSize = 0

    /**
     * 相册取消按钮色值
     */
    @ColorInt
    @Deprecated("")
    var pictureCancelTextColor = 0

    /**
     * 相册右侧按钮色值
     */
    @ColorInt
    var pictureRightDefaultTextColor = 0

    /**
     * 相册右侧文字字体大小
     */
    var pictureRightTextSize = 0

    /**
     * 相册右侧按钮文本
     */
    var pictureRightDefaultText: String? = null

    /**
     * 相册右侧按钮色值
     */
    @ColorInt
    var pictureRightSelectedTextColor = 0

    /**
     * 相册列表底部背景色
     */
    @ColorInt
    var pictureBottomBgColor = 0

    /**
     * 相册列表已完成按钮色值
     */
    @ColorInt
    var pictureCompleteTextColor = 0

    /**
     * 相册列表未完成按钮色值
     */
    @ColorInt
    var pictureUnCompleteTextColor = 0

    /**
     * 相册列表完成按钮字体大小
     */
    var pictureCompleteTextSize = 0

    /**
     * 相册列表不可预览文字颜色
     */
    @ColorInt
    var pictureUnPreviewTextColor = 0

    /**
     * 相册列表预览文字大小
     */
    var picturePreviewTextSize = 0

    /**
     * 相册列表未完成按钮文本
     */
    var pictureUnCompleteText: String? = null

    /**
     * 相册列表已完成按钮文本
     */
    var pictureCompleteText: String? = null

    /**
     * 相册列表预览文字颜色
     */
    @ColorInt
    var picturePreviewTextColor = 0

    /**
     * 相册列表不可预览文字
     */
    var pictureUnPreviewText: String? = null

    /**
     * 相册列表预览文字
     */
    var picturePreviewText: String? = null

    /**
     * 相册列表预览界面底部背景色
     */
    @ColorInt
    var picturePreviewBottomBgColor = 0

    /**
     * # SDK Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP有效
     * 相册导航条颜色
     */
    @ColorInt
    var pictureNavBarColor = 0

    /**
     * 原图字体颜色
     */
    @ColorInt
    var pictureOriginalFontColor = 0

    /**
     * 原图字体大小
     */
    var pictureOriginalTextSize = 0

    /**
     * 相册右侧按钮不可点击背景样式
     */
    @DrawableRes
    var pictureUnCompleteBackgroundStyle = 0

    /**
     * 相册右侧按钮可点击背景样式
     */
    @DrawableRes
    var pictureCompleteBackgroundStyle = 0

    /**
     * 相册标题右侧箭头
     */
    @DrawableRes
    var pictureTitleUpResId = 0

    /**
     * 相册标题右侧箭头
     */
    @DrawableRes
    var pictureTitleDownResId = 0

    /**
     * 相册返回图标
     */
    @DrawableRes
    var pictureLeftBackIcon = 0

    /**
     * 相册勾选CheckBox drawable样式
     */
    @DrawableRes
    var pictureCheckedStyle = 0

    /**
     * 是否使用(%1$d/%2$d)字符串
     */
    var isCompleteReplaceNum = false

    /**
     * WeChatStyle 预览右下角 勾选CheckBox drawable样式
     */
    @DrawableRes
    var pictureWeChatChooseStyle = 0

    /**
     * WeChatStyle 预览界面返回键样式
     */
    @DrawableRes
    var pictureWeChatLeftBackStyle = 0

    /**
     * WeChatStyle 相册界面标题背景样式
     */
    @DrawableRes
    var pictureWeChatTitleBackgroundStyle = 0

    /**
     * WeChatStyle 自定义预览页右下角选择文字大小
     */
    var pictureWeChatPreviewSelectedTextSize = 0

    /**
     * WeChatStyle 自定义预览页右下角选择文字文案
     */
    var pictureWeChatPreviewSelectedText: String? = null

    /**
     * 图片已选数量圆点背景色
     */
    @DrawableRes
    var pictureCheckNumBgStyle = 0

    /**
     * 相册文件夹列表选中圆点
     */
    @DrawableRes
    var pictureFolderCheckedDotStyle = 0

    /**
     * 外部预览图片删除按钮样式
     */
    @DrawableRes
    var pictureExternalPreviewDeleteStyle = 0

    /**
     * 原图勾选样式
     */
    @DrawableRes
    var pictureOriginalControlStyle = 0

    /**
     * 外部预览图片是否显示删除按钮
     */
    var pictureExternalPreviewGonePreviewDelete = false

    /**
     * 选择相册目录背景样式
     */
    @DrawableRes
    var pictureAlbumStyle = 0

    constructor() : super() {}

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (isChangeStatusBarFontColor) 1.toByte() else 0.toByte())
        dest.writeByte(if (isOpenCompletedNumStyle) 1.toByte() else 0.toByte())
        dest.writeByte(if (isOpenCheckNumStyle) 1.toByte() else 0.toByte())
        dest.writeInt(pictureStatusBarColor)
        dest.writeInt(pictureTitleBarBackgroundColor)
        dest.writeInt(pictureContainerBackgroundColor)
        dest.writeInt(pictureTitleTextColor)
        dest.writeInt(pictureTitleTextSize)
        dest.writeInt(pictureCancelTextColor)
        dest.writeInt(pictureRightDefaultTextColor)
        dest.writeInt(pictureRightTextSize)
        dest.writeString(pictureRightDefaultText)
        dest.writeInt(pictureRightSelectedTextColor)
        dest.writeInt(pictureBottomBgColor)
        dest.writeInt(pictureCompleteTextColor)
        dest.writeInt(pictureUnCompleteTextColor)
        dest.writeInt(pictureCompleteTextSize)
        dest.writeInt(pictureUnPreviewTextColor)
        dest.writeInt(picturePreviewTextSize)
        dest.writeString(pictureUnCompleteText)
        dest.writeString(pictureCompleteText)
        dest.writeInt(picturePreviewTextColor)
        dest.writeString(pictureUnPreviewText)
        dest.writeString(picturePreviewText)
        dest.writeInt(picturePreviewBottomBgColor)
        dest.writeInt(pictureNavBarColor)
        dest.writeInt(pictureOriginalFontColor)
        dest.writeInt(pictureOriginalTextSize)
        dest.writeInt(pictureUnCompleteBackgroundStyle)
        dest.writeInt(pictureCompleteBackgroundStyle)
        dest.writeInt(pictureTitleUpResId)
        dest.writeInt(pictureTitleDownResId)
        dest.writeInt(pictureLeftBackIcon)
        dest.writeInt(pictureCheckedStyle)
        dest.writeByte(if (isCompleteReplaceNum) 1.toByte() else 0.toByte())
        dest.writeInt(pictureWeChatChooseStyle)
        dest.writeInt(pictureWeChatLeftBackStyle)
        dest.writeInt(pictureWeChatTitleBackgroundStyle)
        dest.writeInt(pictureWeChatPreviewSelectedTextSize)
        dest.writeString(pictureWeChatPreviewSelectedText)
        dest.writeInt(pictureCheckNumBgStyle)
        dest.writeInt(pictureFolderCheckedDotStyle)
        dest.writeInt(pictureExternalPreviewDeleteStyle)
        dest.writeInt(pictureOriginalControlStyle)
        dest.writeByte(if (pictureExternalPreviewGonePreviewDelete) 1.toByte() else 0.toByte())
        dest.writeInt(pictureAlbumStyle)
    }

    protected constructor(`in`: Parcel) {
        isChangeStatusBarFontColor = `in`.readByte().toInt() != 0
        isOpenCompletedNumStyle = `in`.readByte().toInt() != 0
        isOpenCheckNumStyle = `in`.readByte().toInt() != 0
        pictureStatusBarColor = `in`.readInt()
        pictureTitleBarBackgroundColor = `in`.readInt()
        pictureContainerBackgroundColor = `in`.readInt()
        pictureTitleTextColor = `in`.readInt()
        pictureTitleTextSize = `in`.readInt()
        pictureCancelTextColor = `in`.readInt()
        pictureRightDefaultTextColor = `in`.readInt()
        pictureRightTextSize = `in`.readInt()
        pictureRightDefaultText = `in`.readString()
        pictureRightSelectedTextColor = `in`.readInt()
        pictureBottomBgColor = `in`.readInt()
        pictureCompleteTextColor = `in`.readInt()
        pictureUnCompleteTextColor = `in`.readInt()
        pictureCompleteTextSize = `in`.readInt()
        pictureUnPreviewTextColor = `in`.readInt()
        picturePreviewTextSize = `in`.readInt()
        pictureUnCompleteText = `in`.readString()
        pictureCompleteText = `in`.readString()
        picturePreviewTextColor = `in`.readInt()
        pictureUnPreviewText = `in`.readString()
        picturePreviewText = `in`.readString()
        picturePreviewBottomBgColor = `in`.readInt()
        pictureNavBarColor = `in`.readInt()
        pictureOriginalFontColor = `in`.readInt()
        pictureOriginalTextSize = `in`.readInt()
        pictureUnCompleteBackgroundStyle = `in`.readInt()
        pictureCompleteBackgroundStyle = `in`.readInt()
        pictureTitleUpResId = `in`.readInt()
        pictureTitleDownResId = `in`.readInt()
        pictureLeftBackIcon = `in`.readInt()
        pictureCheckedStyle = `in`.readInt()
        isCompleteReplaceNum = `in`.readByte().toInt() != 0
        pictureWeChatChooseStyle = `in`.readInt()
        pictureWeChatLeftBackStyle = `in`.readInt()
        pictureWeChatTitleBackgroundStyle = `in`.readInt()
        pictureWeChatPreviewSelectedTextSize = `in`.readInt()
        pictureWeChatPreviewSelectedText = `in`.readString()
        pictureCheckNumBgStyle = `in`.readInt()
        pictureFolderCheckedDotStyle = `in`.readInt()
        pictureExternalPreviewDeleteStyle = `in`.readInt()
        pictureOriginalControlStyle = `in`.readInt()
        pictureExternalPreviewGonePreviewDelete = `in`.readByte().toInt() != 0
        pictureAlbumStyle = `in`.readInt()
    }

    companion object {
        val CREATOR: Parcelable.Creator<PictureParameterStyle> = object : Parcelable.Creator<PictureParameterStyle?> {
            override fun createFromParcel(source: Parcel): PictureParameterStyle? {
                return PictureParameterStyle(source)
            }

            override fun newArray(size: Int): Array<PictureParameterStyle?> {
                return arrayOfNulls(size)
            }
        }
    }
}