package com.luck.picture.lib.tools

import android.content.Context
import android.database.Cursor
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.luck.picture.lib.config.PictureConfig
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

/**
 * @author：luck
 * @date：2017-5-30 19:30
 * @describe：PictureFileUtils
 */
object PictureFileUtils {
    const val POSTFIX = ".jpg"
    const val POST_VIDEO = ".mp4"
    const val POST_AUDIO = ".mp3"

    /**
     * @param context
     * @param type
     * @param format
     * @param outCameraDirectory
     * @return
     */
    fun createCameraFile(context: Context, type: Int, fileName: String, format: String, outCameraDirectory: String): File? {
        return createMediaFile(context, type, fileName, format, outCameraDirectory)
    }

    /**
     * 创建文件
     *
     * @param context
     * @param type
     * @param fileName
     * @param format
     * @param outCameraDirectory
     * @return
     */
    private fun createMediaFile(context: Context, chooseMode: Int, fileName: String, format: String, outCameraDirectory: String): File? {
        return createOutFile(context, chooseMode, fileName, format, outCameraDirectory)
    }

    private fun createOutFile(context: Context, chooseMode: Int, fileName: String, format: String, outCameraDirectory: String): File? {
        var folderDir: File? = null
        if (TextUtils.isEmpty(outCameraDirectory)) {
            // 外部没有自定义拍照存储路径使用默认
            val state = Environment.getExternalStorageState()
            val rootDir = if (state == Environment.MEDIA_MOUNTED) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) else getRootDirFile(context, chooseMode)
            if (rootDir != null) {
                if (!rootDir.exists()) {
                    rootDir.mkdirs()
                }
                folderDir = File(rootDir.absolutePath + File.separator + PictureMimeType.CAMERA + File.separator)
                if (!folderDir.exists() && folderDir.mkdirs()) {
                }
            }
        } else {
            // 自定义存储路径
            folderDir = File(outCameraDirectory)
            if (!folderDir.exists()) {
                folderDir.mkdirs()
            }
        }
        if (folderDir == null) {
            throw NullPointerException("The media output path cannot be null")
        }
        val isOutFileNameEmpty: Boolean = TextUtils.isEmpty(fileName)
        return when (chooseMode) {
            PictureConfig.TYPE_VIDEO -> {
                val newFileVideoName = if (isOutFileNameEmpty) DateUtils.getCreateFileName("VID_").toString() + POST_VIDEO else fileName
                File(folderDir, newFileVideoName)
            }
            PictureConfig.TYPE_AUDIO -> {
                val newFileAudioName = if (isOutFileNameEmpty) DateUtils.getCreateFileName("AUD_").toString() + POST_AUDIO else fileName
                File(folderDir, newFileAudioName)
            }
            else -> {
                val suffix = if (TextUtils.isEmpty(format)) POSTFIX else format
                val newFileImageName = if (isOutFileNameEmpty) DateUtils.getCreateFileName("IMG_").toString() + suffix else fileName
                File(folderDir, newFileImageName)
            }
        }
    }

    /**
     * 文件根目录
     *
     * @param context
     * @param type
     * @return
     */
    private fun getRootDirFile(context: Context, type: Int): File? {
        return when (type) {
            PictureConfig.TYPE_VIDEO -> context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            PictureConfig.TYPE_AUDIO -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            else -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
    }

    /**
     * TAG for log messages.
     */
    const val TAG = "PictureFileUtils"

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                      selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
                column
        )
        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs,
                    null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } catch (ex: IllegalArgumentException) {
            Log.i(TAG, String.format(Locale.getDefault(), "getDataColumn: _data - [%s]", ex.message))
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br></br>
     * <br></br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    fun getPath(ctx: Context, uri: Uri): String? {
        val context = ctx.applicationContext
        val isKitKat: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return if (SdkVersionUtils.checkedAndroid_Q()) {
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + split[1]
                    } else {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id: String = DocumentsContract.getDocumentId(uri)
                val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                        split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) {
                uri.lastPathSegment
            } else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Copies one file into the other with the given paths.
     * In the event that the paths are the same, trying to copy one file to the other
     * will cause both files to become null.
     * Simply skipping this step if the paths are identical.
     */
    @Throws(IOException::class)
    fun copyFile(pathFrom: String, pathTo: String) {
        if (pathFrom.equals(pathTo, ignoreCase = true)) {
            return
        }
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(File(pathFrom)).channel
            outputChannel = FileOutputStream(File(pathTo)).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            inputChannel.close()
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }
    }

    /**
     * 拷贝文件
     *
     * @param outFile
     * @return
     */
    fun bufferCopy(inBuffer: BufferedSource?, outFile: File): Boolean {
        var outBuffer: BufferedSink? = null
        try {
            outBuffer = outFile.sink().buffer()
            outBuffer.writeAll(inBuffer)
            outBuffer.flush()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close(inBuffer)
            close(outBuffer)
        }
        return false
    }

    /**
     * 拷贝文件
     *
     * @param outputStream
     * @return
     */
    fun bufferCopy(inBuffer: BufferedSource?, outputStream: OutputStream): Boolean {
        var outBuffer: BufferedSink? = null
        try {
            outBuffer = outputStream.sink().buffer()
            outBuffer.writeAll(inBuffer)
            outBuffer.flush()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close(inBuffer)
            close(outBuffer)
        }
        return false
    }

    /**
     * 拷贝文件
     *
     * @param inFile
     * @param outPutStream
     * @return
     */
    fun bufferCopy(inFile: File, outPutStream: OutputStream): Boolean {
        var inBuffer: BufferedSource? = null
        var outBuffer: BufferedSink? = null
        try {
            inBuffer = inFile.source().buffer()
            outBuffer = outPutStream.sink().buffer()
            outBuffer.writeAll(inBuffer)
            outBuffer.flush()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close(inBuffer)
            close(outPutStream)
            close(outBuffer)
        }
        return false
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    fun readPictureDegree(context: Context, path: String?): Int {
        var degree = 0
        try {
            val exifInterface: ExifInterface
            if (SdkVersionUtils.checkedAndroid_Q()) {
                val parcelFileDescriptor: ParcelFileDescriptor? = context.contentResolver
                        .openFileDescriptor(Uri.parse(path), "r")
                exifInterface = ExifInterface(parcelFileDescriptor.getFileDescriptor())
            } else {
                exifInterface = ExifInterface(path)
            }
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return degree
    }

    /**
     * getDCIMCameraPath
     *
     * @return
     */
    fun getDCIMCameraPath(): String {
        val absolutePath: String
        absolutePath = try {
            "%" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/Camera"
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
        return absolutePath
    }

    /**
     * set empty PictureSelector Cache
     *
     * @param mContext
     * @param type     image or video ...
     */
    fun deleteCacheDirFile(mContext: Context, type: Int) {
        val cutDir = mContext.getExternalFilesDir(if (type == PictureMimeType.ofImage()) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES)
        if (cutDir != null) {
            val files = cutDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * set empty PictureSelector Cache
     *
     * @param context
     * @param type    image、video、audio ...
     */
    fun deleteAllCacheDirFile(context: Context) {
        val dirPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (dirPictures != null) {
            val files = dirPictures.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        }
        val dirMovies = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (dirMovies != null) {
            val files = dirMovies.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        }
        val dirMusic = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (dirMusic != null) {
            val files = dirMusic.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * @param ctx
     * @return
     */
    fun getDiskCacheDir(ctx: Context): String {
        val filesDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return ""
        return filesDir.path
    }

    /**
     * @param ctx
     * @return
     */
    fun getVideoDiskCacheDir(ctx: Context): String {
        val filesDir = ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return ""
        return filesDir.path
    }

    /**
     * @param ctx
     * @return
     */
    fun getAudioDiskCacheDir(ctx: Context): String {
        val filesDir = ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return ""
        return filesDir.path
    }

    /**
     * 生成uri
     *
     * @param context
     * @param cameraFile
     * @return
     */
    fun parUri(context: Context, cameraFile: File?): Uri {
        val imageUri: Uri
        val authority = context.packageName + ".provider"
        imageUri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            //通过FileProvider创建一个content类型的Uri
            FileProvider.getUriForFile(context, authority, cameraFile)
        } else {
            Uri.fromFile(cameraFile)
        }
        return imageUri
    }

    /**
     * 获取图片后缀
     *
     * @param input
     * @return
     */
    fun extSuffix(input: InputStream?): String {
        return try {
            val options: BitmapFactory.Options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(input, null, options)
            options.outMimeType.replace("image/", ".")
        } catch (e: Exception) {
            PictureMimeType.JPEG
        }
    }

    /**
     * 根据类型创建文件名
     *
     * @param context
     * @param md5
     * @param mineType
     * @param customFileName
     * @return
     */
    fun createFilePath(context: Context, md5: String, mineType: String?, customFileName: String?): String {
        val suffix: String = PictureMimeType.getLastImgSuffix(mineType)
        return if (PictureMimeType.isHasVideo(mineType)) {
            // 视频
            val filesDir = getVideoDiskCacheDir(context) + File.separator
            if (!TextUtils.isEmpty(md5)) {
                val fileName = if (TextUtils.isEmpty(customFileName)) "VID_" + md5.toUpperCase() + suffix else customFileName!!
                filesDir + fileName
            } else {
                val fileName = if (TextUtils.isEmpty(customFileName)) DateUtils.getCreateFileName("VID_").toString() + suffix else customFileName!!
                filesDir + fileName
            }
        } else if (PictureMimeType.isHasAudio(mineType)) {
            // 音频
            val filesDir = getAudioDiskCacheDir(context) + File.separator
            if (!TextUtils.isEmpty(md5)) {
                val fileName = if (TextUtils.isEmpty(customFileName)) "AUD_" + md5.toUpperCase() + suffix else customFileName!!
                filesDir + fileName
            } else {
                val fileName = if (TextUtils.isEmpty(customFileName)) DateUtils.getCreateFileName("AUD_").toString() + suffix else customFileName!!
                filesDir + fileName
            }
        } else {
            // 图片
            val filesDir = getDiskCacheDir(context) + File.separator
            if (!TextUtils.isEmpty(md5)) {
                val fileName = if (TextUtils.isEmpty(customFileName)) "IMG_" + md5.toUpperCase() + suffix else customFileName!!
                filesDir + fileName
            } else {
                val fileName = if (TextUtils.isEmpty(customFileName)) DateUtils.getCreateFileName("IMG_").toString() + suffix else customFileName!!
                filesDir + fileName
            }
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param path
     * @return
     */
    fun isFileExists(path: String?): Boolean {
        return if (!TextUtils.isEmpty(path) && !File(path).exists()) {
            false
        } else true
    }

    fun close(c: Closeable?) {
        // java.lang.IncompatibleClassChangeError: interface not implemented
        if (c != null && c is Closeable) {
            try {
                c.close()
            } catch (e: Exception) {
                // silence
            }
        }
    }
}