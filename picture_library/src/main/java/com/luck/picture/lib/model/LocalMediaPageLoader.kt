package com.luck.picture.lib.model

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.luck.picture.lib.config.PictureConfig
import java.util.*

/**
 * @author：luck
 * @date：2020-04-13 15:06
 * @describe：Local media database query class，Support paging
 */
class LocalMediaPageLoader(private val mContext: Context, config: PictureSelectionConfig) {
    private val config: PictureSelectionConfig

    /**
     * Get the latest cover of an album catalog
     *
     * @param bucketId
     * @return
     */
    fun getFirstCover(bucketId: Long): String? {
        var data: Cursor? = null
        try {
            val orderBy: String = MediaStore.Files.FileColumns._ID + " DESC limit 1 offset 0"
            data = mContext.contentResolver.query(QUERY_URI, arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.MediaColumns.DATA), getPageSelection(bucketId), getPageSelectionArgs(bucketId), orderBy)
            if (data != null && data.count > 0) {
                if (data.moveToFirst()) {
                    val id = data.getLong(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    return if (SdkVersionUtils.checkedAndroid_Q()) getRealPathAndroid_Q(id) else data.getString(data.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                }
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (data != null && !data.isClosed) {
                data.close()
            }
        }
        return null
    }

    /**
     * Queries for data in the specified directory
     *
     * @param bucketId
     * @param page
     * @param limit
     * @param listener
     * @return
     */
    fun loadPageMediaData(bucketId: Long, page: Int, limit: Int, listener: OnQueryDataResultListener?) {
        loadPageMediaData(bucketId, page, limit, config.pageSize, listener)
    }

    /**
     * Queries for data in the specified directory
     *
     * @param bucketId
     * @param listener
     * @return
     */
    fun loadPageMediaData(bucketId: Long, page: Int, listener: OnQueryDataResultListener?) {
        loadPageMediaData(bucketId, page, config.pageSize, config.pageSize, listener)
    }

    /**
     * Queries for data in the specified directory (page)
     *
     * @param bucketId
     * @param page
     * @param limit
     * @param pageSize
     * @return
     */
    fun loadPageMediaData(bucketId: Long, page: Int, limit: Int, pageSize: Int, listener: OnQueryDataResultListener?) {
        PictureThreadUtils.executeByIo(object : SimpleTask<MediaData?>() {
            fun doInBackground(): MediaData? {
                var data: Cursor? = null
                try {
                    val orderBy: String = if (page == -1) MediaStore.Files.FileColumns._ID + " DESC" else MediaStore.Files.FileColumns._ID + " DESC limit " + limit + " offset " + (page - 1) * pageSize
                    data = mContext.contentResolver.query(QUERY_URI, PROJECTION_PAGE, getPageSelection(bucketId), getPageSelectionArgs(bucketId), orderBy)
                    if (data != null) {
                        val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
                        if (data.count > 0) {
                            data.moveToFirst()
                            do {
                                val id = data.getLong(data.getColumnIndexOrThrow(PROJECTION_PAGE[0]))
                                val absolutePath = data.getString(data.getColumnIndexOrThrow(PROJECTION_PAGE[1]))
                                val url = if (SdkVersionUtils.checkedAndroid_Q()) getRealPathAndroid_Q(id) else absolutePath
                                if (config.isFilterInvalidFile) {
                                    if (!PictureFileUtils.isFileExists(absolutePath)) {
                                        continue
                                    }
                                }
                                var mimeType = data.getString(data.getColumnIndexOrThrow(PROJECTION_PAGE[2]))
                                mimeType = if (TextUtils.isEmpty(mimeType)) PictureMimeType.ofJPEG() else mimeType
                                // Here, it is solved that some models obtain mimeType and return the format of image / *,
                                // which makes it impossible to distinguish the specific type, such as mi 8,9,10 and other models
                                if (mimeType.endsWith("image/*")) {
                                    mimeType = if (PictureMimeType.isContent(url)) {
                                        PictureMimeType.getImageMimeType(absolutePath)
                                    } else {
                                        PictureMimeType.getImageMimeType(url)
                                    }
                                    if (!config.isGif) {
                                        val isGif: Boolean = PictureMimeType.isGif(mimeType)
                                        if (isGif) {
                                            continue
                                        }
                                    }
                                }
                                val width = data.getInt(data.getColumnIndexOrThrow(PROJECTION_PAGE[3]))
                                val height = data.getInt(data.getColumnIndexOrThrow(PROJECTION_PAGE[4]))
                                val duration = data.getLong(data.getColumnIndexOrThrow(PROJECTION_PAGE[5]))
                                val size = data.getLong(data.getColumnIndexOrThrow(PROJECTION_PAGE[6]))
                                val folderName = data.getString(data.getColumnIndexOrThrow(PROJECTION_PAGE[7]))
                                val fileName = data.getString(data.getColumnIndexOrThrow(PROJECTION_PAGE[8]))
                                val bucket_id = data.getLong(data.getColumnIndexOrThrow(PROJECTION_PAGE[9]))
                                if (config.filterFileSize > 0) {
                                    if (size > config.filterFileSize * FILE_SIZE_UNIT) {
                                        continue
                                    }
                                }
                                if (PictureMimeType.isHasVideo(mimeType)) {
                                    if (config.videoMinSecond > 0 && duration < config.videoMinSecond) {
                                        // If you set the minimum number of seconds of video to display
                                        continue
                                    }
                                    if (config.videoMaxSecond > 0 && duration > config.videoMaxSecond) {
                                        // If you set the maximum number of seconds of video to display
                                        continue
                                    }
                                    if (duration == 0L) {
                                        //If the length is 0, the corrupted video is processed and filtered out
                                        continue
                                    }
                                    if (size <= 0) {
                                        // The video size is 0 to filter out
                                        continue
                                    }
                                }
                                val image = LocalMedia(id, url, absolutePath, fileName, folderName, duration, config.chooseMode, mimeType, width, height, size, bucket_id)
                                result.add(image)
                            } while (data.moveToNext())
                        }
                        return MediaData(data.count > 0, result)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.i(TAG, "loadMedia Page Data Error: " + e.message)
                    return null
                } finally {
                    if (data != null && !data.isClosed) {
                        data.close()
                    }
                }
                return null
            }

            fun onSuccess(result: MediaData?) {
                if (listener != null && result != null) {
                    listener.onComplete(result.data, page, result.isHasNextMore)
                }
            }
        })
    }

    /**
     * Query the local gallery data
     *
     * @param listener
     */
    fun loadAllMedia(listener: OnQueryDataResultListener?) {
        PictureThreadUtils.executeByIo(object : SimpleTask<List<LocalMediaFolder?>?>() {
            fun doInBackground(): List<LocalMediaFolder>? {
                val data = mContext.contentResolver.query(QUERY_URI,
                        if (SdkVersionUtils.checkedAndroid_Q()) PROJECTION_29 else PROJECTION,
                        getSelection(), getSelectionArgs(), ORDER_BY)
                try {
                    if (data != null) {
                        val count = data.count
                        var totalCount = 0
                        val mediaFolders: MutableList<LocalMediaFolder> = ArrayList<LocalMediaFolder>()
                        if (count > 0) {
                            if (SdkVersionUtils.checkedAndroid_Q()) {
                                val countMap: MutableMap<Long, Long> = HashMap()
                                while (data.moveToNext()) {
                                    val bucketId = data.getLong(data.getColumnIndex(COLUMN_BUCKET_ID))
                                    var newCount = countMap[bucketId]
                                    if (newCount == null) {
                                        newCount = 1L
                                    } else {
                                        newCount++
                                    }
                                    countMap[bucketId] = newCount
                                }
                                if (data.moveToFirst()) {
                                    val hashSet: MutableSet<Long> = HashSet()
                                    do {
                                        val bucketId = data.getLong(data.getColumnIndex(COLUMN_BUCKET_ID))
                                        if (hashSet.contains(bucketId)) {
                                            continue
                                        }
                                        val mediaFolder = LocalMediaFolder()
                                        mediaFolder.setBucketId(bucketId)
                                        val bucketDisplayName = data.getString(
                                                data.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME))
                                        val size = countMap[bucketId]!!
                                        val id = data.getLong(data.getColumnIndex(MediaStore.Files.FileColumns._ID))
                                        mediaFolder.setName(bucketDisplayName)
                                        mediaFolder.setImageNum(ValueOf.toInt(size))
                                        mediaFolder.setFirstImagePath(getRealPathAndroid_Q(id))
                                        mediaFolders.add(mediaFolder)
                                        hashSet.add(bucketId)
                                        totalCount += size.toInt()
                                    } while (data.moveToNext())
                                }
                            } else {
                                data.moveToFirst()
                                do {
                                    val mediaFolder = LocalMediaFolder()
                                    val bucketId = data.getLong(data.getColumnIndex(COLUMN_BUCKET_ID))
                                    val bucketDisplayName = data.getString(data.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME))
                                    val size = data.getInt(data.getColumnIndex(COLUMN_COUNT))
                                    mediaFolder.setBucketId(bucketId)
                                    val url = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DATA))
                                    mediaFolder.setFirstImagePath(url)
                                    mediaFolder.setName(bucketDisplayName)
                                    mediaFolder.setImageNum(size)
                                    mediaFolders.add(mediaFolder)
                                    totalCount += size
                                } while (data.moveToNext())
                            }
                            sortFolder(mediaFolders)

                            // 相机胶卷
                            val allMediaFolder = LocalMediaFolder()
                            allMediaFolder.setImageNum(totalCount)
                            allMediaFolder.setChecked(true)
                            allMediaFolder.setBucketId(-1)
                            if (data.moveToFirst()) {
                                val firstUrl = if (SdkVersionUtils.checkedAndroid_Q()) getFirstUri(data) else getFirstUrl(data)
                                allMediaFolder.setFirstImagePath(firstUrl)
                            }
                            val bucketDisplayName = if (config.chooseMode == PictureMimeType.ofAudio()) mContext.getString(R.string.picture_all_audio) else mContext.getString(R.string.picture_camera_roll)
                            allMediaFolder.setName(bucketDisplayName)
                            allMediaFolder.setOfAllType(config.chooseMode)
                            allMediaFolder.setCameraFolder(true)
                            mediaFolders.add(0, allMediaFolder)
                            return mediaFolders
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.i(TAG, "loadAllMedia Data Error: " + e.message)
                    return null
                } finally {
                    if (data != null && !data.isClosed) {
                        data.close()
                    }
                }
                return ArrayList<LocalMediaFolder>()
            }

            fun onSuccess(result: List<LocalMediaFolder?>?) {
                if (listener != null && result != null) {
                    listener.onComplete(result, 1, false)
                }
            }
        })
    }

    private fun getPageSelection(bucketId: Long): String? {
        val durationCondition = getDurationCondition(0, 0)
        val isSpecifiedFormat: Boolean = !TextUtils.isEmpty(config.specifiedFormat)
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL -> {
                return if (bucketId == -1L) {
                    // ofAll
                    ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                            + (if (config.isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                            + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + durationCondition + ") AND " + MediaStore.MediaColumns.SIZE + ">0")
                } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                        + (if (config.isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                        + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + durationCondition + ") AND " + COLUMN_BUCKET_ID + "=? AND " + MediaStore.MediaColumns.SIZE + ">0"
                // Gets the specified album directory
            }
            PictureConfig.TYPE_IMAGE -> {
                // Gets the image of the specified type
                if (bucketId == -1L) {
                    // ofAll
                    return if (isSpecifiedFormat) {
                        ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                                + (if (config.isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF + " AND " + MediaStore.MediaColumns.MIME_TYPE + "='" + config.specifiedFormat + "'")
                                + ") AND " + MediaStore.MediaColumns.SIZE + ">0")
                    } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                            + (if (config.isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                            + ") AND " + MediaStore.MediaColumns.SIZE + ">0"
                }
                // Gets the specified album directory
                return if (isSpecifiedFormat) {
                    ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                            + (if (config.isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF + " AND " + MediaStore.MediaColumns.MIME_TYPE + "='" + config.specifiedFormat + "'")
                            + ") AND " + COLUMN_BUCKET_ID + "=? AND " + MediaStore.MediaColumns.SIZE + ">0")
                } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                        + (if (config.isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                        + ") AND " + COLUMN_BUCKET_ID + "=? AND " + MediaStore.MediaColumns.SIZE + ">0"
            }
            PictureConfig.TYPE_VIDEO, PictureConfig.TYPE_AUDIO -> {
                if (bucketId == -1L) {
                    // ofAll
                    return if (isSpecifiedFormat) {
                        "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + MediaStore.MediaColumns.MIME_TYPE + "='" + config.specifiedFormat + "'" + " AND " + durationCondition + ") AND " + MediaStore.MediaColumns.SIZE + ">0"
                    } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + durationCondition + ") AND " + MediaStore.MediaColumns.SIZE + ">0"
                }
                // Gets the specified album directory
                return if (isSpecifiedFormat) {
                    "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + MediaStore.MediaColumns.MIME_TYPE + "='" + config.specifiedFormat + "'" + " AND " + durationCondition + ") AND " + COLUMN_BUCKET_ID + "=? AND " + MediaStore.MediaColumns.SIZE + ">0"
                } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + durationCondition + ") AND " + COLUMN_BUCKET_ID + "=? AND " + MediaStore.MediaColumns.SIZE + ">0"
            }
        }
        return null
    }

    private fun getPageSelectionArgs(bucketId: Long): Array<String>? {
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL -> {
                return if (bucketId == -1L) {
                    // ofAll
                    arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                } else arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                        ValueOf.toString(bucketId)
                )
                //  Gets the specified album directory
            }
            PictureConfig.TYPE_IMAGE ->                 // Get photo
                return getSelectionArgsForPageSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, bucketId)
            PictureConfig.TYPE_VIDEO ->                 // Get video
                return getSelectionArgsForPageSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, bucketId)
            PictureConfig.TYPE_AUDIO ->                 // Get audio
                return getSelectionArgsForPageSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO, bucketId)
        }
        return null
    }

    private fun getSelection(): String? {
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL ->                 // Get all, not including audio
                return getSelectionArgsForAllMediaCondition(getDurationCondition(0, 0), config.isGif)
            PictureConfig.TYPE_IMAGE -> {
                if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // 获取指定类型的图片
                    return if (SdkVersionUtils.checkedAndroid_Q()) {
                        SELECTION_SPECIFIED_FORMAT_29 + "='" + config.specifiedFormat + "' AND " + MediaStore.MediaColumns.SIZE + ">0"
                    } else SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "') AND " + MediaStore.MediaColumns.SIZE + ">0)" + GROUP_BY_BUCKET_Id
                }
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    return if (config.isGif) SELECTION_29 else SELECTION_NOT_GIF_29
                }
                return if (config.isGif) SELECTION else SELECTION_NOT_GIF
            }
            PictureConfig.TYPE_VIDEO -> {
                // 获取视频
                return if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Gets the specified album directory
                    if (SdkVersionUtils.checkedAndroid_Q()) {
                        SELECTION_SPECIFIED_FORMAT_29 + "='" + config.specifiedFormat + "' AND " + MediaStore.MediaColumns.SIZE + ">0"
                    } else SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "') AND " + MediaStore.MediaColumns.SIZE + ">0)" + GROUP_BY_BUCKET_Id
                } else getSelectionArgsForSingleMediaCondition(getDurationCondition(0, 0))
            }
            PictureConfig.TYPE_AUDIO -> {
                // Get Audio
                return if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Gets the specified album directory
                    if (SdkVersionUtils.checkedAndroid_Q()) {
                        SELECTION_SPECIFIED_FORMAT_29 + "='" + config.specifiedFormat + "' AND " + MediaStore.MediaColumns.SIZE + ">0"
                    } else SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "') AND " + MediaStore.MediaColumns.SIZE + ">0)" + GROUP_BY_BUCKET_Id
                } else getSelectionArgsForSingleMediaCondition(getDurationCondition(0, AUDIO_DURATION.toLong()))
            }
        }
        return null
    }

    private fun getSelectionArgs(): Array<String>? {
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL -> return SELECTION_ALL_ARGS
            PictureConfig.TYPE_IMAGE ->                 // Get photo
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            PictureConfig.TYPE_VIDEO ->                 // Get video
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            PictureConfig.TYPE_AUDIO -> return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO)
        }
        return null
    }

    /**
     * Sort by number of files
     *
     * @param imageFolders
     */
    private fun sortFolder(imageFolders: List<LocalMediaFolder>) {
        Collections.sort(imageFolders, Comparator<T> { lhs: T, rhs: T ->
            if (lhs.getData() == null || rhs.getData() == null) {
                return@sort 0
            }
            val lSize: Int = lhs.getImageNum()
            val rSize: Int = rhs.getImageNum()
            Integer.compare(rSize, lSize)
        })
    }

    /**
     * Get video (maximum or minimum time)
     *
     * @param exMaxLimit
     * @param exMinLimit
     * @return
     */
    private fun getDurationCondition(exMaxLimit: Long, exMinLimit: Long): String {
        var maxS = if (config.videoMaxSecond == 0) Long.MAX_VALUE else config.videoMaxSecond
        if (exMaxLimit != 0L) {
            maxS = Math.min(maxS, exMaxLimit)
        }
        return String.format(Locale.CHINA, "%d <%s " + MediaStore.MediaColumns.DURATION + " and " + MediaStore.MediaColumns.DURATION + " <= %d",
                Math.max(exMinLimit, config.videoMinSecond),
                if (Math.max(exMinLimit, config.videoMinSecond) == 0L) "" else "=",
                maxS)
    }

    companion object {
        private val TAG = LocalMediaPageLoader::class.java.simpleName
        private val QUERY_URI: Uri = MediaStore.Files.getContentUri("external")
        private val ORDER_BY: String = MediaStore.Files.FileColumns._ID + " DESC"
        private const val NOT_GIF_UNKNOWN = "!='image/*'"
        private val NOT_GIF = "!='image/gif' AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF_UNKNOWN
        private const val GROUP_BY_BUCKET_Id = " GROUP BY (bucket_id"
        private const val COLUMN_COUNT = "count"
        private const val COLUMN_BUCKET_ID = "bucket_id"
        private const val COLUMN_BUCKET_DISPLAY_NAME = "bucket_display_name"

        /**
         * Filter out recordings that are less than 500 milliseconds long
         */
        private const val AUDIO_DURATION = 500

        /**
         * unit
         */
        private const val FILE_SIZE_UNIT = 1024 * 1024L

        /**
         * Image
         */
        private val SELECTION = ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? )"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0)" + GROUP_BY_BUCKET_Id)
        private val SELECTION_29: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=? "
                + " AND " + MediaStore.MediaColumns.SIZE + ">0")
        private val SELECTION_NOT_GIF = ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF + ") AND " + MediaStore.MediaColumns.SIZE + ">0)" + GROUP_BY_BUCKET_Id)
        private val SELECTION_NOT_GIF_29: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF + " AND " + MediaStore.MediaColumns.SIZE + ">0")

        /**
         * Queries for images with the specified suffix
         */
        private val SELECTION_SPECIFIED_FORMAT = ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.MIME_TYPE)

        /**
         * Queries for images with the specified suffix targetSdk>=29
         */
        private val SELECTION_SPECIFIED_FORMAT_29: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.MIME_TYPE)

        /**
         * Query criteria (audio and video)
         *
         * @param timeCondition
         * @return
         */
        private fun getSelectionArgsForSingleMediaCondition(timeCondition: String): String {
            return if (SdkVersionUtils.checkedAndroid_Q()) {
                (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                        + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                        + " AND " + timeCondition)
            } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + ") AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + timeCondition + ")" + GROUP_BY_BUCKET_Id
        }

        /**
         * All mode conditions
         *
         * @param timeCondition
         * @param isGif
         * @return
         */
        private fun getSelectionArgsForAllMediaCondition(timeCondition: String, isGif: Boolean): String {
            return if (SdkVersionUtils.checkedAndroid_Q()) {
                ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                        + (if (isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                        + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + timeCondition + ") AND " + MediaStore.MediaColumns.SIZE + ">0")
            } else "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + (if (isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                    + " OR " + (MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + timeCondition) + ")" + " AND " + MediaStore.MediaColumns.SIZE + ">0)" + GROUP_BY_BUCKET_Id
        }

        /**
         * Get pictures or videos
         */
        private val SELECTION_ALL_ARGS = arrayOf<String>(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())

        /**
         * Gets a file of the specified type
         *
         * @param mediaType
         * @return
         */
        private fun getSelectionArgsForSingleMediaType(mediaType: Int): Array<String> {
            return arrayOf(mediaType.toString())
        }

        /**
         * Gets a file of the specified type
         *
         * @param mediaType
         * @return
         */
        private fun getSelectionArgsForPageSingleMediaType(mediaType: Int, bucketId: Long): Array<String> {
            return if (bucketId == -1L) arrayOf(mediaType.toString()) else arrayOf(mediaType.toString(), ValueOf.toString(bucketId))
        }

        private val PROJECTION_29 = arrayOf(
                MediaStore.Files.FileColumns._ID,
                COLUMN_BUCKET_ID,
                COLUMN_BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE)
        private val PROJECTION = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.DATA,
                COLUMN_BUCKET_ID,
                COLUMN_BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                "COUNT(*) AS " + COLUMN_COUNT)

        /**
         * Media file database field
         */
        private val PROJECTION_PAGE = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.MediaColumns.DURATION,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DISPLAY_NAME,
                COLUMN_BUCKET_ID)

        /**
         * Get cover uri
         *
         * @param cursor
         * @return
         */
        private fun getFirstUri(cursor: Cursor): String {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
            return getRealPathAndroid_Q(id)
        }

        /**
         * Get cover url
         *
         * @param cursor
         * @return
         */
        private fun getFirstUrl(cursor: Cursor): String {
            return cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
        }

        /**
         * Android Q
         *
         * @param id
         * @return
         */
        private fun getRealPathAndroid_Q(id: Long): String {
            return QUERY_URI.buildUpon().appendPath(ValueOf.toString(id)).build().toString()
        }

        private var instance: LocalMediaPageLoader? = null
        fun getInstance(context: Context, config: PictureSelectionConfig): LocalMediaPageLoader? {
            if (instance == null) {
                synchronized(LocalMediaPageLoader::class.java) {
                    if (instance == null) {
                        instance = LocalMediaPageLoader(context.applicationContext, config)
                    }
                }
            }
            return instance
        }

        /**
         * set empty
         */
        fun setInstanceNull() {
            instance = null
        }
    }

    init {
        this.config = config
    }
}