package com.luck.picture.lib.style

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.AnimRes

/**
 * @author：luck
 * @date：2019-11-25 18:17
 * @describe：PictureSelector Activity动画管理Style
 */
class PictureWindowAnimationStyle : Parcelable {
    /**
     * 相册启动动画
     */
    @AnimRes
    var activityEnterAnimation = 0

    /**
     * 相册退出动画
     */
    @AnimRes
    var activityExitAnimation = 0

    /**
     * 预览界面启动动画
     */
    @AnimRes
    var activityPreviewEnterAnimation = 0

    /**
     * 预览界面退出动画
     */
    @AnimRes
    var activityPreviewExitAnimation = 0

    /**
     * 裁剪界面启动动画
     */
    @AnimRes
    var activityCropEnterAnimation = 0

    /**
     * 裁剪界面退出动画
     */
    @AnimRes
    var activityCropExitAnimation = 0

    constructor() : super() {}
    constructor(@AnimRes activityEnterAnimation: Int,
                @AnimRes activityExitAnimation: Int) : super() {
        this.activityEnterAnimation = activityEnterAnimation
        this.activityExitAnimation = activityExitAnimation
    }

    constructor(@AnimRes activityEnterAnimation: Int,
                @AnimRes activityExitAnimation: Int,
                @AnimRes activityPreviewEnterAnimation: Int,
                @AnimRes activityPreviewExitAnimation: Int) : super() {
        this.activityEnterAnimation = activityEnterAnimation
        this.activityExitAnimation = activityExitAnimation
        this.activityPreviewEnterAnimation = activityPreviewEnterAnimation
        this.activityPreviewExitAnimation = activityPreviewExitAnimation
    }

    /**
     * 全局所有动画样式
     *
     * @param enterAnimation
     * @param exitAnimation
     */
    fun ofAllAnimation(enterAnimation: Int, exitAnimation: Int) {
        activityEnterAnimation = enterAnimation
        activityExitAnimation = exitAnimation
        activityPreviewEnterAnimation = enterAnimation
        activityPreviewExitAnimation = exitAnimation
        activityCropEnterAnimation = enterAnimation
        activityCropExitAnimation = exitAnimation
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(activityEnterAnimation)
        dest.writeInt(activityExitAnimation)
        dest.writeInt(activityPreviewEnterAnimation)
        dest.writeInt(activityPreviewExitAnimation)
        dest.writeInt(activityCropEnterAnimation)
        dest.writeInt(activityCropExitAnimation)
    }

    protected constructor(`in`: Parcel) {
        activityEnterAnimation = `in`.readInt()
        activityExitAnimation = `in`.readInt()
        activityPreviewEnterAnimation = `in`.readInt()
        activityPreviewExitAnimation = `in`.readInt()
        activityCropEnterAnimation = `in`.readInt()
        activityCropExitAnimation = `in`.readInt()
    }

    companion object {
        val CREATOR: Parcelable.Creator<PictureWindowAnimationStyle> = object : Parcelable.Creator<PictureWindowAnimationStyle?> {
            override fun createFromParcel(source: Parcel): PictureWindowAnimationStyle? {
                return PictureWindowAnimationStyle(source)
            }

            override fun newArray(size: Int): Array<PictureWindowAnimationStyle?> {
                return arrayOfNulls(size)
            }
        }
    }
}