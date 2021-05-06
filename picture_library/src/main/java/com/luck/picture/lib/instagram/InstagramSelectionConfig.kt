package com.luck.picture.lib.instagram

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import com.luck.picture.lib.config.PictureSelectionConfig

/**
 * ================================================
 * Created by JessYan on 2020/5/20 15:05
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramSelectionConfig : Parcelable {
    private var currentTheme: Int = InsGallery.THEME_STYLE_DEFAULT
    private var cropVideoEnabled = true
    private var coverEnabled = true
    fun setCurrentTheme(currentTheme: Int): InstagramSelectionConfig {
        this.currentTheme = currentTheme
        return this
    }

    fun getCurrentTheme(): Int {
        return currentTheme
    }

    fun isCropVideo(): Boolean {
        return cropVideoEnabled
    }

    fun setCropVideoEnabled(enableCropVideo: Boolean): InstagramSelectionConfig {
        cropVideoEnabled = enableCropVideo
        return this
    }

    fun haveCover(): Boolean {
        return coverEnabled
    }

    fun setCoverEnabled(coverEnabled: Boolean): InstagramSelectionConfig {
        this.coverEnabled = coverEnabled
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(currentTheme)
        dest.writeByte(if (cropVideoEnabled) 1.toByte() else 0.toByte())
        dest.writeByte(if (coverEnabled) 1.toByte() else 0.toByte())
    }

    private constructor() {}
    private constructor(`in`: Parcel) {
        currentTheme = `in`.readInt()
        cropVideoEnabled = `in`.readByte().toInt() != 0
        coverEnabled = `in`.readByte().toInt() != 0
    }

    companion object {
        fun createConfig(): InstagramSelectionConfig {
            return InstagramSelectionConfig()
        }

        fun convertIntent(selectionConfig: PictureSelectionConfig?, origin: Intent?) {
            if (origin == null) {
                return
            }
            if (selectionConfig != null && selectionConfig.instagramSelectionConfig != null) {
                origin.setClassName(origin.component!!.packageName, PictureSelectorInstagramStyleActivity::class.java.getName())
            }
        }

        val CREATOR: Parcelable.Creator<InstagramSelectionConfig> = object : Parcelable.Creator<InstagramSelectionConfig?> {
            override fun createFromParcel(source: Parcel): InstagramSelectionConfig? {
                return InstagramSelectionConfig(source)
            }

            override fun newArray(size: Int): Array<InstagramSelectionConfig?> {
                return arrayOfNulls(size)
            }
        }
    }
}