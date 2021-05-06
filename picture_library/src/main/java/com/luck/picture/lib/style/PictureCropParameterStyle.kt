package com.luck.picture.lib.style

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt

/**
 * @author：luck
 * @date：2019-11-22 17:24
 * @describe：裁剪动态样式参数设置
 */
class PictureCropParameterStyle : Parcelable {
    /**
     * 是否改变状态栏字体颜色 黑白切换
     */
    var isChangeStatusBarFontColor = false

    /**
     * 裁剪页标题背景颜色
     */
    @ColorInt
    var cropTitleBarBackgroundColor = 0

    /**
     * 裁剪页状态栏颜色
     */
    @ColorInt
    var cropStatusBarColorPrimaryDark = 0

    /**
     * 裁剪页标题栏字体颜色
     */
    @ColorInt
    var cropTitleColor = 0

    /**
     * # SDK Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP有效
     * 裁剪导航条颜色
     */
    @ColorInt
    var cropNavBarColor = 0

    constructor() : super() {}
    constructor(cropTitleBarBackgroundColor: Int,
                cropStatusBarColorPrimaryDark: Int,
                cropTitleColor: Int,
                isChangeStatusBarFontColor: Boolean) {
        this.cropTitleBarBackgroundColor = cropTitleBarBackgroundColor
        this.cropStatusBarColorPrimaryDark = cropStatusBarColorPrimaryDark
        this.cropTitleColor = cropTitleColor
        this.isChangeStatusBarFontColor = isChangeStatusBarFontColor
    }

    constructor(cropTitleBarBackgroundColor: Int,
                cropStatusBarColorPrimaryDark: Int,
                cropNavBarColor: Int,
                cropTitleColor: Int,
                isChangeStatusBarFontColor: Boolean) {
        this.cropTitleBarBackgroundColor = cropTitleBarBackgroundColor
        this.cropNavBarColor = cropNavBarColor
        this.cropStatusBarColorPrimaryDark = cropStatusBarColorPrimaryDark
        this.cropTitleColor = cropTitleColor
        this.isChangeStatusBarFontColor = isChangeStatusBarFontColor
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (isChangeStatusBarFontColor) 1.toByte() else 0.toByte())
        dest.writeInt(cropTitleBarBackgroundColor)
        dest.writeInt(cropStatusBarColorPrimaryDark)
        dest.writeInt(cropTitleColor)
        dest.writeInt(cropNavBarColor)
    }

    protected constructor(`in`: Parcel) {
        isChangeStatusBarFontColor = `in`.readByte().toInt() != 0
        cropTitleBarBackgroundColor = `in`.readInt()
        cropStatusBarColorPrimaryDark = `in`.readInt()
        cropTitleColor = `in`.readInt()
        cropNavBarColor = `in`.readInt()
    }

    companion object {
        val CREATOR: Parcelable.Creator<PictureCropParameterStyle> = object : Parcelable.Creator<PictureCropParameterStyle?> {
            override fun createFromParcel(source: Parcel): PictureCropParameterStyle? {
                return PictureCropParameterStyle(source)
            }

            override fun newArray(size: Int): Array<PictureCropParameterStyle?> {
                return arrayOfNulls(size)
            }
        }
    }
}