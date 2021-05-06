package com.luck.picture.lib

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import com.luck.picture.lib.config.PictureMimeType
import java.io.InputStream

/**
 * @author：luck
 * @date：2020-04-12 13:13
 * @describe：PictureSelector对外提供的一些方法
 */
object PictureSelectorExternalUtils {
    /**
     * 获取ExifInterface
     *
     * @param context
     * @param url
     * @return
     */
    fun getExifInterface(context: Context, url: String?): ExifInterface? {
        var exifInterface: ExifInterface? = null
        var inputStream: InputStream? = null
        try {
            if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(url)) {
                inputStream = context.contentResolver.openInputStream(Uri.parse(url))
                if (inputStream != null) {
                    exifInterface = ExifInterface(inputStream)
                }
            } else {
                exifInterface = ExifInterface(url)
            }
            return exifInterface
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            PictureFileUtils.close(inputStream)
        }
        return null
    }
}