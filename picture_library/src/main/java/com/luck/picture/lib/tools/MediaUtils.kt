package com.luck.picture.lib.tools

import android.content.Context
import android.database.Cursor
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.luck.picture.lib.config.PictureMimeType
import java.io.InputStream

/**
 * @author：luck
 * @date：2019-10-21 17:10
 * @describe：资源处理工具类
 */
object MediaUtils {
    /**
     * 创建一条图片地址uri,用于保存拍照后的照片
     *
     * @param context
     * @param suffixType
     * @return 图片的uri
     */
    fun createImageUri(context: Context, suffixType: String?): Uri? {
        val imageFilePath = arrayOf<Uri?>(null)
        val status = Environment.getExternalStorageState()
        val time: String = ValueOf.toString(System.currentTimeMillis())
        // ContentValues是我们希望这条记录被创建时包含的数据信息
        val values = ContentValues(3)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, DateUtils.getCreateFileName("IMG_"))
        values.put(MediaStore.Images.Media.DATE_TAKEN, time)
        values.put(MediaStore.Images.Media.MIME_TYPE, if (TextUtils.isEmpty(suffixType)) PictureMimeType.MIME_TYPE_IMAGE else suffixType)
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status == Environment.MEDIA_MOUNTED) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, PictureMimeType.DCIM)
            imageFilePath[0] = context.contentResolver
                    .insert(MediaStore.Images.Media.getContentUri("external"), values)
        } else {
            imageFilePath[0] = context.contentResolver
                    .insert(MediaStore.Images.Media.getContentUri("internal"), values)
        }
        return imageFilePath[0]
    }

    /**
     * 创建一条视频地址uri,用于保存录制的视频
     *
     * @param context
     * @param suffixType
     * @return 视频的uri
     */
    fun createVideoUri(context: Context, suffixType: String?): Uri? {
        val imageFilePath = arrayOf<Uri?>(null)
        val status = Environment.getExternalStorageState()
        val time: String = ValueOf.toString(System.currentTimeMillis())
        // ContentValues是我们希望这条记录被创建时包含的数据信息
        val values = ContentValues(3)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, DateUtils.getCreateFileName("VID_"))
        values.put(MediaStore.Video.Media.DATE_TAKEN, time)
        values.put(MediaStore.Video.Media.MIME_TYPE, if (TextUtils.isEmpty(suffixType)) PictureMimeType.MIME_TYPE_VIDEO else suffixType)
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status == Environment.MEDIA_MOUNTED) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            imageFilePath[0] = context.contentResolver
                    .insert(MediaStore.Video.Media.getContentUri("external"), values)
        } else {
            imageFilePath[0] = context.contentResolver
                    .insert(MediaStore.Video.Media.getContentUri("internal"), values)
        }
        return imageFilePath[0]
    }

    /**
     * 获取视频时长
     *
     * @param context
     * @param isAndroidQ
     * @param path
     * @return
     */
    fun extractDuration(context: Context, isAndroidQ: Boolean, path: String): Long {
        return if (isAndroidQ) getLocalDuration(context, Uri.parse(path)) else getLocalDuration(path)
    }

    /**
     * 是否是长图
     *
     * @param media
     * @return true 是 or false 不是
     */
    fun isLongImg(media: LocalMedia?): Boolean {
        if (null != media) {
            val width: Int = media.getWidth()
            val height: Int = media.getHeight()
            val newHeight = width * 3
            return height > newHeight
        }
        return false
    }

    /**
     * 是否是长图
     *
     * @param width  宽
     * @param height 高
     * @return true 是 or false 不是
     */
    fun isLongImg(width: Int, height: Int): Boolean {
        val newHeight = width * 3
        return height > newHeight
    }

    /**
     * get Local video duration
     *
     * @return
     */
    private fun getLocalDuration(context: Context, uri: Uri): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * get Local video duration
     *
     * @return
     */
    private fun getLocalDuration(path: String): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * get Local image width or height for api 29
     *
     * @return
     */
    fun getImageSizeForUrlToAndroidQ(context: Context, url: String?): IntArray {
        val size = IntArray(2)
        var query: Cursor? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                query = context.applicationContext.contentResolver
                        .query(Uri.parse(url),
                                null, null, null)
                if (query != null) {
                    query.moveToFirst()
                    size[0] = query.getInt(query.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                    size[1] = query.getInt(query.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            query?.close()
        }
        return size
    }

    /**
     * get Local video width or height
     *
     * @return
     */
    fun getVideoSizeForUrl(url: String?): IntArray {
        val size = IntArray(2)
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(url)
            size[0] = ValueOf.toInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            size[1] = ValueOf.toInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * get Local video width or height
     *
     * @return
     */
    fun getVideoSizeForUri(context: Context?, uri: Uri?): IntArray {
        val size = IntArray(2)
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            size[0] = ValueOf.toInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            size[1] = ValueOf.toInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * get Local image width or height
     *
     * @return
     */
    fun getImageSizeForUrl(url: String?): IntArray {
        val size = IntArray(2)
        try {
            val exifInterface = ExifInterface(url)
            // 获取图片的宽度
            val width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL)
            // 获取图片的高度
            val height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL)
            size[0] = width
            size[1] = height
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * get Local image width or height
     *
     * @return
     */
    fun getImageSizeForUri(context: Context, uri: Uri?): IntArray {
        val size = IntArray(2)
        var fileDescriptor: ParcelFileDescriptor? = null
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri!!, "r")
            if (fileDescriptor != null) {
                val options: BitmapFactory.Options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options)
                size[0] = options.outWidth
                size[1] = options.outHeight
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            PictureFileUtils.close(fileDescriptor)
        }
        return size
    }

    /**
     * 删除部分手机 拍照在DCIM也生成一张的问题
     *
     * @param id
     */
    fun removeMedia(context: Context, id: Int) {
        try {
            val cr: ContentResolver = context.applicationContext.contentResolver
            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection: String = MediaStore.Images.Media._ID + "=?"
            cr.delete(uri, selection, arrayOf(java.lang.Long.toString(id.toLong())))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取DCIM文件下最新一条拍照记录
     *
     * @return
     */
    fun getDCIMLastImageId(context: Context): Int {
        var data: Cursor? = null
        return try {
            //selection: 指定查询条件
            val absolutePath: String = PictureFileUtils.getDCIMCameraPath()
            val orderBy: String = MediaStore.Files.FileColumns._ID + " DESC limit 1 offset 0"
            val selection: String = MediaStore.Images.Media.DATA + " like ?"
            //定义selectionArgs：
            val selectionArgs = arrayOf("$absolutePath%")
            data = context.applicationContext.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                    selection, selectionArgs, orderBy)
            if (data != null && data.count > 0 && data.moveToFirst()) {
                val id = data.getInt(data.getColumnIndex(MediaStore.Images.Media._ID))
                val date = data.getLong(data.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                val duration: Int = DateUtils.dateDiffer(date)
                // DCIM文件下最近时间1s以内的图片，可以判定是最新生成的重复照片
                if (duration <= 1) id else -1
            } else {
                -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        } finally {
            data?.close()
        }
    }

    /**
     * 获取Camera文件下最新一条拍照记录
     *
     * @return
     */
    fun getCameraFirstBucketId(context: Context): Long {
        var data: Cursor? = null
        try {
            val absolutePath: String = PictureFileUtils.getDCIMCameraPath()
            //selection: 指定查询条件
            val selection: String = MediaStore.Files.FileColumns.DATA + " like ?"
            //定义selectionArgs：
            val selectionArgs = arrayOf("$absolutePath%")
            val orderBy: String = MediaStore.Files.FileColumns._ID + " DESC limit 1 offset 0"
            data = context.applicationContext.contentResolver.query(MediaStore.Files.getContentUri("external"), null,
                    selection, selectionArgs, orderBy)
            if (data != null && data.count > 0 && data.moveToFirst()) {
                return data.getLong(data.getColumnIndex("bucket_id"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            data?.close()
        }
        return -1
    }

    /**
     * 获取刚录取的音频文件
     *
     * @param uri
     * @return
     */
    fun getAudioFilePathFromUri(context: Context, uri: Uri?): String? {
        var path = ""
        var cursor: Cursor? = null
        try {
            cursor = context.applicationContext.contentResolver
                    .query(uri!!, null, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                val index = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
                path = cursor.getString(index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return path
    }

    /**
     * 获取旋转角度
     *
     * @param path
     * @return
     */
    fun getVideoOrientationForUrl(path: String?): Int {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            val rotation: Int = ValueOf.toInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
            when (rotation) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取旋转角度
     *
     * @param uri
     * @return
     */
    fun getVideoOrientationForUri(context: Context?, uri: Uri?): Int {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val orientation: Int = ValueOf.toInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
            when (orientation) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取旋转角度
     *
     * @param context
     * @param url
     * @return
     */
    fun getImageOrientationForUrl(context: Context, url: String?): Int {
        var exifInterface: ExifInterface? = null
        var inputStream: InputStream? = null
        return try {
            if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(url!!)) {
                inputStream = context.contentResolver.openInputStream(Uri.parse(url))
                if (inputStream != null) {
                    exifInterface = ExifInterface(inputStream)
                }
            } else {
                exifInterface = ExifInterface(url)
            }
            exifInterface?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            PictureFileUtils.close(inputStream)
        }
    }

    /**
     * 设置LocalMedia旋转信息
     *
     * @param context
     * @param media
     * @param isAndroidQChangeWH
     * @param listener
     * @return
     */
    fun setOrientationAsynchronous(context: Context, media: LocalMedia,
                                   isAndroidQChangeWH: Boolean,
                                   isAndroidQChangeVideoWH: Boolean,
                                   listener: OnCallbackListener<LocalMedia?>?) {
        if (PictureMimeType.isHasImage(media.getMimeType())) {
            if (!isAndroidQChangeWH) {
                return
            }
        }
        if (PictureMimeType.isHasVideo(media.getMimeType())) {
            if (!isAndroidQChangeVideoWH) {
                return
            }
        }
        if (media.getOrientation() !== -1) {
            if (listener != null) {
                listener.onCall(media)
            }
            return
        }
        PictureThreadUtils.executeByIo(object : SimpleTask<Int?>() {
            fun doInBackground(): Int {
                var orientation = 0
                if (PictureMimeType.isHasImage(media.getMimeType())) {
                    orientation = getImageOrientationForUrl(context, media.getPath())
                } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    orientation = if (PictureMimeType.isContent(media.getPath())) {
                        getVideoOrientationForUri(context, Uri.parse(media.getPath()))
                    } else {
                        getVideoOrientationForUrl(media.getPath())
                    }
                }
                return orientation
            }

            fun onSuccess(orientation: Int) {
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                        || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    val width: Int = media.getWidth()
                    val height: Int = media.getHeight()
                    media.setWidth(height)
                    media.setHeight(width)
                }
                media.setOrientation(orientation)
                if (listener != null) {
                    listener.onCall(media)
                }
            }
        })
    }

    /**
     * 设置LocalMedia旋转信息
     *
     * @param context
     * @param media
     * @param isAndroidQChangeWH
     * @return
     */
    fun setOrientationSynchronous(context: Context, media: LocalMedia,
                                  isAndroidQChangeWH: Boolean,
                                  isAndroidQChangeVideoWH: Boolean) {
        if (PictureMimeType.isHasImage(media.getMimeType())) {
            if (!isAndroidQChangeWH) {
                return
            }
        }
        if (PictureMimeType.isHasVideo(media.getMimeType())) {
            if (!isAndroidQChangeVideoWH) {
                return
            }
        }
        // 如果有旋转信息图片宽高则是相反
        var orientation = 0
        if (PictureMimeType.isHasImage(media.getMimeType())) {
            orientation = getImageOrientationForUrl(context, media.getPath())
        } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
            orientation = if (PictureMimeType.isContent(media.getPath())) {
                getVideoOrientationForUri(context, Uri.parse(media.getPath()))
            } else {
                getVideoOrientationForUrl(media.getPath())
            }
        }
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            val width: Int = media.getWidth()
            val height: Int = media.getHeight()
            media.setWidth(height)
            media.setHeight(width)
        }
        media.setOrientation(orientation)
    }
}