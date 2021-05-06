package com.luck.picture.lib.compress

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import com.luck.picture.lib.config.PictureMimeType
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

class Luban private constructor(builder: Builder) : Handler.Callback {
    private var mTargetDir: String?
    private val mNewFileName: String?
    private val focusAlpha: Boolean
    private val isCamera: Boolean
    private val mLeastCompressSize: Int
    private val mRenameListener: OnRenameListener?
    private val mCompressListener: OnCompressListener?
    private val mCompressionPredicate: CompressionPredicate?
    private val mStreamProviders: MutableList<InputStreamProvider>?
    private val mPaths: List<String>?
    private val mediaList: List<LocalMedia>?
    private var index = -1
    private val compressQuality: Int
    private val mHandler: Handler
    private val dataCount: Int

    /**
     * Returns a file with a cache image name in the private cache directory.
     *
     * @param context A context.
     */
    private fun getImageCacheFile(context: Context, provider: InputStreamProvider, suffix: String): File {
        if (TextUtils.isEmpty(mTargetDir)) {
            val imageCacheDir = getImageCacheDir(context)
            if (imageCacheDir != null) {
                mTargetDir = imageCacheDir.absolutePath
            }
        }
        var cacheBuilder = ""
        try {
            val media: LocalMedia = provider.getMedia()
            val encryptionValue: String = StringUtils.getEncryptionValue(media.getPath(), media.getWidth(), media.getHeight())
            cacheBuilder = if (!TextUtils.isEmpty(encryptionValue) && !media.isCut()) {
                mTargetDir + "/" +
                        "IMG_CMP_" +
                        encryptionValue +
                        if (TextUtils.isEmpty(suffix)) ".jpg" else suffix
            } else {
                mTargetDir +
                        "/" +
                        DateUtils.getCreateFileName("IMG_CMP_") +
                        if (TextUtils.isEmpty(suffix)) ".jpg" else suffix
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return File(cacheBuilder)
    }

    private fun getImageCustomFile(context: Context, filename: String?): File {
        if (TextUtils.isEmpty(mTargetDir)) {
            mTargetDir = getImageCacheDir(context)!!.absolutePath
        }
        val cacheBuilder = "$mTargetDir/$filename"
        return File(cacheBuilder)
    }

    /**
     * start asynchronous compress thread
     */
    private fun launch(context: Context) {
        if (mStreamProviders == null || mPaths == null || mStreamProviders.size == 0 && mCompressListener != null) {
            mCompressListener!!.onError(NullPointerException("image file cannot be null"))
        }
        val iterator: MutableIterator<InputStreamProvider> = mStreamProviders!!.iterator()
        // 当前压缩下标
        index = -1
        while (iterator.hasNext()) {
            val path: InputStreamProvider = iterator.next()
            AsyncTask.SERIAL_EXECUTOR.execute(Runnable {
                try {
                    index++
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START))
                    val newPath: String
                    newPath = if (path.open() != null) {
                        if (path.getMedia().isCompressed()
                                && !TextUtils.isEmpty(path.getMedia().getCompressPath())) {
                            // 压缩过的图片不重复压缩  注意:如果是开启了裁剪 就算压缩过也要重新压缩
                            val exists = !path.getMedia().isCut() && File(path.getMedia().getCompressPath()).exists()
                            val result = if (exists) File(path.getMedia().getCompressPath()) else compress(context, path)!!
                            result.absolutePath
                        } else {
                            val result = if (PictureMimeType.isHasVideo(path.getMedia().getMimeType())) File(path.getPath()) else compress(context, path)!!
                            result.absolutePath
                        }
                    } else {
                        // error
                        path.getPath()
                    }
                    if (mediaList != null && mediaList.size > 0) {
                        val media: LocalMedia = mediaList[index]
                        val isHasHttp: Boolean = PictureMimeType.isHasHttp(newPath)
                        val isHasVideo: Boolean = PictureMimeType.isHasVideo(media.getMimeType())
                        media.setCompressed(!isHasHttp && !isHasVideo)
                        media.setCompressPath(if (isHasHttp || isHasVideo) null else newPath)
                        media.setAndroidQToPath(if (SdkVersionUtils.checkedAndroid_Q()) media.getCompressPath() else null)
                        val isLast = index == mediaList.size - 1
                        if (isLast) {
                            mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, mediaList))
                        }
                    } else {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, IOException()))
                    }
                } catch (e: IOException) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, e))
                }
            })
            iterator.remove()
        }
    }

    /**
     * start compress and return the file
     */
    @Throws(IOException::class)
    private operator fun get(input: InputStreamProvider, context: Context): File {
        return try {
            Engine(input, getImageCacheFile(context, input, Checker.SINGLE.extSuffix(input)), focusAlpha, compressQuality).compress()
        } finally {
            input.close()
        }
    }

    @Throws(IOException::class)
    private operator fun get(context: Context): List<File> {
        val results: MutableList<File> = ArrayList()
        val iterator: MutableIterator<InputStreamProvider> = mStreamProviders!!.iterator()
        while (iterator.hasNext()) {
            val provider: InputStreamProvider = iterator.next()
            val inputStream: InputStream = provider.open()
            if (inputStream != null) {
                if (provider.getMedia().isCompressed()
                        && !TextUtils.isEmpty(provider.getMedia().getCompressPath())) {
                    // 压缩过的图片不重复压缩  注意:如果是开启了裁剪 就算压缩过也要重新压缩
                    val exists = !provider.getMedia().isCut() && File(provider.getMedia().getCompressPath()).exists()
                    val oldFile = if (exists) File(provider.getMedia().getCompressPath()) else compress(context, provider)!!
                    results.add(oldFile)
                } else {
                    val hasVideo: Boolean = PictureMimeType.isHasVideo(provider.getMedia().getMimeType())
                    results.add((if (hasVideo) File(provider.getMedia().getPath()) else compress(context, provider))!!)
                }
            } else {
                // error
                results.add(File(provider.getMedia().getPath()))
            }
            iterator.remove()
        }
        return results
    }

    @Throws(IOException::class)
    private fun compress(context: Context, path: InputStreamProvider): File? {
        return try {
            compressRealLocalMedia(context, path)
        } finally {
            path.close()
        }
    }

    @Throws(IOException::class)
    private fun compressReal(context: Context, path: InputStreamProvider): File {
        val result: File
        val suffix: String = Checker.SINGLE.extSuffix(if (path.getMedia() != null) path.getMedia().getMimeType() else "")
        var outFile = getImageCacheFile(context, path, if (TextUtils.isEmpty(suffix)) Checker.SINGLE.extSuffix(path) else suffix)
        if (mRenameListener != null) {
            val filename: String = mRenameListener.rename(path.getPath())
            outFile = getImageCustomFile(context, filename)
        }
        if (mCompressionPredicate != null) {
            if (mCompressionPredicate.apply(path.getPath())
                    && Checker.SINGLE.needCompress(mLeastCompressSize, path.getPath())) {
                result = Engine(path, outFile, focusAlpha, compressQuality).compress()
            } else {
                result = File(path.getPath())
            }
        } else {
            if (Checker.SINGLE.extSuffix(path).startsWith(".gif")) {
                // GIF without compression
                result = File(path.getPath())
            } else {
                result = if (Checker.SINGLE.needCompress(mLeastCompressSize, path.getPath())) Engine(path, outFile, focusAlpha, compressQuality).compress() else File(path.getPath())
            }
        }
        return result
    }

    @Throws(IOException::class)
    private fun compressRealLocalMedia(context: Context, path: InputStreamProvider): File? {
        var result: File? = null
        val media: LocalMedia = path.getMedia()
                ?: throw NullPointerException("Luban Compress LocalMedia Can't be empty")
        val newPath: String = if (media.isCut() && !TextUtils.isEmpty(media.getCutPath())) media.getCutPath() else media.getRealPath()
        val suffix: String = Checker.SINGLE.extSuffix(media.getMimeType())
        var outFile = getImageCacheFile(context, path, if (TextUtils.isEmpty(suffix)) Checker.SINGLE.extSuffix(path) else suffix)
        var filename: String? = ""
        if (!TextUtils.isEmpty(mNewFileName)) {
            filename = if (isCamera || dataCount == 1) mNewFileName else StringUtils.rename(mNewFileName)
            outFile = getImageCustomFile(context, filename)
        }
        // 如果文件存在直接返回不处理
        if (outFile.exists()) {
            return outFile
        }
        if (mCompressionPredicate != null) {
            if (Checker.SINGLE.extSuffix(path).startsWith(".gif")) {
                // GIF without compression
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    if (media.isCut() && !TextUtils.isEmpty(media.getCutPath())) {
                        result = File(media.getCutPath())
                    } else {
                        val androidQToPath: String = AndroidQTransformUtils.copyPathToAndroidQ(context, path.getPath(),
                                media.getWidth(), media.getHeight(), media.getMimeType(), filename)
                        if (!TextUtils.isEmpty(androidQToPath)) {
                            result = File(androidQToPath)
                        }
                    }
                } else {
                    result = File(newPath)
                }
            } else {
                val isCompress: Boolean = Checker.SINGLE.needCompressToLocalMedia(mLeastCompressSize, newPath)
                if (mCompressionPredicate.apply(newPath) && isCompress) {
                    // 压缩
                    result = Engine(path, outFile, focusAlpha, compressQuality).compress()
                } else {
                    if (isCompress) {
                        // 压缩
                        result = Engine(path, outFile, focusAlpha, compressQuality).compress()
                    }
                }
            }
        } else {
            if (Checker.SINGLE.extSuffix(path).startsWith(".gif")) {
                // GIF without compression
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    val newFilePath: String = if (media.isCut()) media.getCutPath() else AndroidQTransformUtils.copyPathToAndroidQ(context,
                            path.getPath(), media.getWidth(), media.getHeight(), media.getMimeType(), filename)
                    if (!TextUtils.isEmpty(newFilePath)) {
                        result = File(newFilePath)
                    }
                } else {
                    result = File(newPath)
                }
            } else {
                val isCompress: Boolean = Checker.SINGLE.needCompressToLocalMedia(mLeastCompressSize, newPath)
                if (isCompress) {
                    // 压缩
                    result = Engine(path, outFile, focusAlpha, compressQuality).compress()
                }
            }
        }
        return result
    }

    override fun handleMessage(msg: Message): Boolean {
        if (mCompressListener == null) return false
        when (msg.what) {
            MSG_COMPRESS_START -> mCompressListener.onStart()
            MSG_COMPRESS_SUCCESS -> mCompressListener.onSuccess(msg.obj as List<LocalMedia?>)
            MSG_COMPRESS_ERROR -> mCompressListener.onError(msg.obj as Throwable)
        }
        return false
    }

    class Builder internal constructor(private val context: Context) {
        var mTargetDir: String? = null
        var mNewFileName: String? = null
        var focusAlpha = false
        var isCamera = false
        var compressQuality = 0
        var mLeastCompressSize = 100
        var mRenameListener: OnRenameListener? = null
        var mCompressListener: OnCompressListener? = null
        var mCompressionPredicate: CompressionPredicate? = null
        val mStreamProviders: MutableList<InputStreamProvider>
        val mPaths: List<String>
        var mediaList: List<LocalMedia>
        var dataCount = 0
        private val isAndroidQ: Boolean
        private fun build(): Luban {
            return Luban(this)
        }

        fun load(inputStreamProvider: InputStreamProvider): Builder {
            mStreamProviders.add(inputStreamProvider)
            return this
        }

        /**
         * 扩展符合PictureSelector的压缩策略
         *
         * @param list LocalMedia集合
         * @param <T>
         * @return
        </T> */
        fun <T> loadMediaData(list: List<LocalMedia>): Builder {
            mediaList = list
            dataCount = list.size
            for (src in list) {
                load(src)
            }
            return this
        }

        /**
         * 扩展符合PictureSelector的压缩策略
         *
         * @param media LocalMedia对象
         * @param <T>
         * @return
        </T> */
        private fun load(media: LocalMedia): Builder {
            mStreamProviders.add(object : InputStreamAdapter() {
                @Throws(IOException::class)
                override fun openInternal(): InputStream? {
                    return if (PictureMimeType.isContent(media.getPath()) && !media.isCut()) {
                        if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
                            FileInputStream(media.getAndroidQToPath())
                        } else context.contentResolver.openInputStream(Uri.parse(media.getPath()))
                    } else {
                        if (PictureMimeType.isHasHttp(media.getPath())) null else FileInputStream(if (media.isCut()) media.getCutPath() else media.getPath())
                    }
                }

                override val path: String
                    get() = if (media.isCut()) {
                        media.getCutPath()
                    } else {
                        if (TextUtils.isEmpty(media.getAndroidQToPath())) media.getPath() else media.getAndroidQToPath()
                    }
                override val media: LocalMedia
                    get() = media
            })
            return this
        }

        fun load(uri: Uri): Builder {
            mStreamProviders.add(object : InputStreamAdapter() {
                @Throws(IOException::class)
                override fun openInternal(): InputStream? {
                    return context.contentResolver.openInputStream(uri)
                }

                override val path: String
                    get() = uri.path!!
                override val media: LocalMedia?
                    get() = null
            })
            return this
        }

        fun load(file: File): Builder {
            mStreamProviders.add(object : InputStreamAdapter() {
                @Throws(IOException::class)
                override fun openInternal(): InputStream {
                    return FileInputStream(file)
                }

                override val path: String
                    get() = file.absolutePath
                override val media: LocalMedia?
                    get() = null
            })
            return this
        }

        fun load(string: String): Builder {
            mStreamProviders.add(object : InputStreamAdapter() {
                @Throws(IOException::class)
                override fun openInternal(): InputStream {
                    return FileInputStream(string)
                }

                override val path: String
                    get() = string
                override val media: LocalMedia?
                    get() = null
            })
            return this
        }

        fun <T> load(list: List<T>): Builder {
            for (src in list) {
                if (src is String) {
                    load(src as String)
                } else if (src is File) {
                    load(src as File)
                } else if (src is Uri) {
                    load(src as Uri)
                } else {
                    throw IllegalArgumentException("Incoming data type exception, it must be String, File, Uri or Bitmap")
                }
            }
            return this
        }

        fun putGear(gear: Int): Builder {
            return this
        }

        @Deprecated("")
        fun setRenameListener(listener: OnRenameListener?): Builder {
            mRenameListener = listener
            return this
        }

        fun setCompressListener(listener: OnCompressListener?): Builder {
            mCompressListener = listener
            return this
        }

        fun setTargetDir(targetDir: String?): Builder {
            mTargetDir = targetDir
            return this
        }

        fun setNewCompressFileName(newFileName: String?): Builder {
            mNewFileName = newFileName
            return this
        }

        fun isCamera(isCamera: Boolean): Builder {
            this.isCamera = isCamera
            return this
        }

        /**
         * Do I need to keep the image's alpha channel
         *
         * @param focusAlpha
         *
         * true - to keep alpha channel, the compress speed will be slow.
         *
         *  false - don't keep alpha channel, it might have a black background.
         */
        fun setFocusAlpha(focusAlpha: Boolean): Builder {
            this.focusAlpha = focusAlpha
            return this
        }

        /**
         * Image compressed output quality
         *
         * @param compressQuality The quality is better than
         */
        fun setCompressQuality(compressQuality: Int): Builder {
            this.compressQuality = compressQuality
            return this
        }

        /**
         * do not compress when the origin image file size less than one value
         *
         * @param size the value of file size, unit KB, default 100K
         */
        fun ignoreBy(size: Int): Builder {
            mLeastCompressSize = size
            return this
        }

        /**
         * do compress image when return value was true, otherwise, do not compress the image file
         *
         * @param compressionPredicate A predicate callback that returns true or false for the given input path should be compressed.
         */
        fun filter(compressionPredicate: CompressionPredicate?): Builder {
            mCompressionPredicate = compressionPredicate
            return this
        }

        /**
         * begin compress image with asynchronous
         */
        fun launch() {
            build().launch(context)
        }

        @Throws(IOException::class)
        operator fun get(path: String): File {
            return build()[object : InputStreamAdapter() {
                @Throws(IOException::class)
                override fun openInternal(): InputStream {
                    return FileInputStream(path)
                }

                override val path: String
                    get() = path
                override val media: LocalMedia?
                    get() = null
            }, context]
        }

        /**
         * begin compress image with synchronize
         *
         * @return the thumb image file list
         */
        @Throws(IOException::class)
        fun get(): List<File> {
            return build()[context]
        }

        init {
            mPaths = ArrayList()
            mediaList = ArrayList<LocalMedia>()
            mStreamProviders = ArrayList<InputStreamProvider>()
            isAndroidQ = SdkVersionUtils.checkedAndroid_Q()
        }
    }

    companion object {
        private const val TAG = "Luban"
        private const val MSG_COMPRESS_SUCCESS = 0
        private const val MSG_COMPRESS_START = 1
        private const val MSG_COMPRESS_ERROR = 2
        fun with(context: Context): Builder {
            return Builder(context)
        }

        /**
         * Returns a directory with the given name in the private cache directory of the application to
         * use to store retrieved media and thumbnails.
         *
         * @param context A context.
         * @see .getImageCacheDir
         */
        private fun getImageCacheDir(context: Context): File? {
            val cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (cacheDir != null) {
                return if (!cacheDir.mkdirs() && (!cacheDir.exists() || !cacheDir.isDirectory)) {
                    // File wasn't able to create a directory, or the result exists but not a directory
                    null
                } else cacheDir
            }
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "default disk cache dir is null")
            }
            return null
        }
    }

    init {
        mPaths = builder.mPaths
        mediaList = builder.mediaList
        dataCount = builder.dataCount
        mTargetDir = builder.mTargetDir
        mNewFileName = builder.mNewFileName
        mRenameListener = builder.mRenameListener
        mStreamProviders = builder.mStreamProviders
        mCompressListener = builder.mCompressListener
        mLeastCompressSize = builder.mLeastCompressSize
        mCompressionPredicate = builder.mCompressionPredicate
        compressQuality = builder.compressQuality
        focusAlpha = builder.focusAlpha
        isCamera = builder.isCamera
        mHandler = Handler(Looper.getMainLooper(), this)
    }
}