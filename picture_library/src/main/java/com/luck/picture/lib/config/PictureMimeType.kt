package com.luck.picture.lib.config

import android.content.*
import android.text.TextUtils
import com.luck.picture.lib.R
import java.io.File

/**
 * @author：luck
 * @date：2017-5-24 17:02
 * @describe：PictureMimeType
 */
object PictureMimeType {
    fun ofAll(): Int {
        return PictureConfig.TYPE_ALL
    }

    fun ofImage(): Int {
        return PictureConfig.TYPE_IMAGE
    }

    fun ofVideo(): Int {
        return PictureConfig.TYPE_VIDEO
    }

    /**
     * # No longer maintain audio related functions,
     * but can continue to use but there will be phone compatibility issues
     *
     *
     * 不再维护音频相关功能，但可以继续使用但会有机型兼容性问题
     */
    @Deprecated("")
    fun ofAudio(): Int {
        return PictureConfig.TYPE_AUDIO
    }

    fun ofPNG(): String {
        return MIME_TYPE_PNG
    }

    fun ofJPEG(): String {
        return MIME_TYPE_JPEG
    }

    fun ofBMP(): String {
        return MIME_TYPE_BMP
    }

    fun ofGIF(): String {
        return MIME_TYPE_GIF
    }

    fun ofWEBP(): String {
        return MIME_TYPE_WEBP
    }

    fun of3GP(): String {
        return MIME_TYPE_3GP
    }

    fun ofMP4(): String {
        return MIME_TYPE_MP4
    }

    fun ofMPEG(): String {
        return MIME_TYPE_MPEG
    }

    fun ofAVI(): String {
        return MIME_TYPE_AVI
    }

    private const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_JPEG = "image/jpeg"
    private const val MIME_TYPE_JPG = "image/jpg"
    private const val MIME_TYPE_BMP = "image/bmp"
    private const val MIME_TYPE_GIF = "image/gif"
    private const val MIME_TYPE_WEBP = "image/webp"
    private const val MIME_TYPE_3GP = "video/3gp"
    private const val MIME_TYPE_MP4 = "video/mp4"
    private const val MIME_TYPE_MPEG = "video/mpeg"
    private const val MIME_TYPE_AVI = "video/avi"

    /**
     * isGif
     *
     * @param mimeType
     * @return
     */
    fun isGif(mimeType: String?): Boolean {
        return mimeType != null && (mimeType == "image/gif" || mimeType == "image/GIF")
    }

    /**
     * isVideo
     *
     * @param mimeType
     * @return
     */
    fun isHasVideo(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_VIDEO)
    }

    /**
     * isVideo
     *
     * @param url
     * @return
     */
    fun isUrlHasVideo(url: String): Boolean {
        return url.endsWith(".mp4")
    }

    /**
     * isAudio
     *
     * @param mimeType
     * @return
     */
    fun isHasAudio(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_AUDIO)
    }

    /**
     * isImage
     *
     * @param mimeType
     * @return
     */
    fun isHasImage(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_IMAGE)
    }

    /**
     * Determine if it is JPG.
     *
     * @param is image file mimeType
     */
    fun isJPEG(mimeType: String): Boolean {
        return if (TextUtils.isEmpty(mimeType)) {
            false
        } else mimeType.startsWith(MIME_TYPE_JPEG) || mimeType.startsWith(MIME_TYPE_JPG)
    }

    /**
     * Determine if it is JPG.
     *
     * @param is image file mimeType
     */
    fun isJPG(mimeType: String): Boolean {
        return if (TextUtils.isEmpty(mimeType)) {
            false
        } else mimeType.startsWith(MIME_TYPE_JPG)
    }

    /**
     * is Network image
     *
     * @param path
     * @return
     */
    fun isHasHttp(path: String): Boolean {
        return if (TextUtils.isEmpty(path)) {
            false
        } else path.startsWith("http")
                || path.startsWith("https")
                || path.startsWith("/http")
                || path.startsWith("/https")
    }

    /**
     * Determine whether the file type is an image or a video
     *
     * @param cameraMimeType
     * @return
     */
    fun getMimeType(cameraMimeType: Int): String {
        return when (cameraMimeType) {
            PictureConfig.TYPE_VIDEO -> MIME_TYPE_VIDEO
            PictureConfig.TYPE_AUDIO -> MIME_TYPE_AUDIO
            else -> MIME_TYPE_IMAGE
        }
    }

    /**
     * 判断文件类型是图片还是视频
     *
     * @param file
     * @return
     */
    fun getMimeType(file: File?): String {
        if (file != null) {
            val name = file.name
            if (name.endsWith(".mp4") || name.endsWith(".avi")
                    || name.endsWith(".3gpp") || name.endsWith(".3gp") || name.endsWith(".mov")) {
                return MIME_TYPE_VIDEO
            } else if (name.endsWith(".PNG") || name.endsWith(".png") || name.endsWith(".jpeg")
                    || name.endsWith(".gif") || name.endsWith(".GIF") || name.endsWith(".jpg")
                    || name.endsWith(".webp") || name.endsWith(".WEBP") || name.endsWith(".JPEG")
                    || name.endsWith(".bmp")) {
                return MIME_TYPE_IMAGE
            } else if (name.endsWith(".mp3") || name.endsWith(".amr")
                    || name.endsWith(".aac") || name.endsWith(".war")
                    || name.endsWith(".flac") || name.endsWith(".lamr")) {
                return MIME_TYPE_AUDIO
            }
        }
        return MIME_TYPE_IMAGE
    }

    /**
     * Determines if the file name is a picture
     *
     * @param name
     * @return
     */
    fun isSuffixOfImage(name: String): Boolean {
        return (!TextUtils.isEmpty(name) && name.endsWith(".PNG") || name.endsWith(".png") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".GIF") || name.endsWith(".jpg")
                || name.endsWith(".webp") || name.endsWith(".WEBP") || name.endsWith(".JPEG")
                || name.endsWith(".bmp"))
    }

    /**
     * Is it the same type
     *
     * @param oldMimeType
     * @param newMimeType
     * @return
     */
    fun isMimeTypeSame(oldMimeType: String, newMimeType: String): Boolean {
        return getMimeType(oldMimeType) == getMimeType(newMimeType)
    }

    /**
     * Get Image mimeType
     *
     * @param path
     * @return
     */
    fun getImageMimeType(path: String?): String {
        try {
            if (!TextUtils.isEmpty(path)) {
                val file = File(path)
                val fileName = file.name
                val last = fileName.lastIndexOf(".") + 1
                val temp = fileName.substring(last)
                return "image/$temp"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return MIME_TYPE_IMAGE
        }
        return MIME_TYPE_IMAGE
    }

    /**
     * Picture or video
     *
     * @return
     */
    fun getMimeType(mimeType: String): Int {
        if (TextUtils.isEmpty(mimeType)) {
            return PictureConfig.TYPE_IMAGE
        }
        return if (mimeType.startsWith(MIME_TYPE_PREFIX_VIDEO)) {
            PictureConfig.TYPE_VIDEO
        } else if (mimeType.startsWith(MIME_TYPE_PREFIX_AUDIO)) {
            PictureConfig.TYPE_AUDIO
        } else {
            PictureConfig.TYPE_IMAGE
        }
    }

    /**
     * Get image suffix
     *
     * @param mineType
     * @return
     */
    fun getLastImgSuffix(mineType: String): String {
        val defaultSuffix = PNG
        try {
            val index = mineType.lastIndexOf("/") + 1
            if (index > 0) {
                return "." + mineType.substring(index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return defaultSuffix
        }
        return defaultSuffix
    }

    /**
     * is content://
     *
     * @param url
     * @return
     */
    fun isContent(url: String): Boolean {
        return if (TextUtils.isEmpty(url)) {
            false
        } else url.startsWith("content://")
    }

    /**
     * Returns an error message by type
     *
     * @param context
     * @param mimeType
     * @return
     */
    fun s(context: Context, mimeType: String?): String {
        val ctx = context.applicationContext
        return if (isHasVideo(mimeType)) {
            ctx.getString(R.string.picture_video_error)
        } else if (isHasAudio(mimeType)) {
            ctx.getString(R.string.picture_audio_error)
        } else {
            ctx.getString(R.string.picture_error)
        }
    }

    const val JPEG = ".jpg"
    private const val PNG = ".png"
    const val MP4 = ".mp4"
    const val JPEG_Q = "image/jpeg"
    const val PNG_Q = "image/png"
    const val MP4_Q = "video/mp4"
    const val AVI_Q = "video/avi"
    const val DCIM = "DCIM/Camera"
    const val CAMERA = "Camera"
    const val MIME_TYPE_IMAGE = "image/jpeg"
    const val MIME_TYPE_VIDEO = "video/mp4"
    const val MIME_TYPE_AUDIO = "audio/mpeg"
    private const val MIME_TYPE_PREFIX_IMAGE = "image"
    private const val MIME_TYPE_PREFIX_VIDEO = "video"
    private const val MIME_TYPE_PREFIX_AUDIO = "audio"
}