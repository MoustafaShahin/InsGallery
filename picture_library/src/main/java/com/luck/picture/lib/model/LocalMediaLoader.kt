package com.luck.picture.lib.model

import android.content.Context
import android.net.Uri
import android.util.Log
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.util.*

/**
 * @author：luck
 * @data：2016/12/31 19:12
 * @describe: Local media database query class
 */
@Deprecated("")
class LocalMediaLoader(context: Context, config: PictureSelectionConfig) {
    private val mContext: Context
    private val isAndroidQ: Boolean
    private val config: PictureSelectionConfig

    /**
     * Query the local gallery data
     *
     * @return
     */
    fun loadAllMedia(): List<LocalMediaFolder>? {
        val data = mContext.contentResolver.query(QUERY_URI, PROJECTION, getSelection(), getSelectionArgs(), ORDER_BY)
        try {
            if (data != null) {
                val imageFolders: MutableList<LocalMediaFolder> = ArrayList<LocalMediaFolder>()
                val allImageFolder = LocalMediaFolder()
                val latelyImages: MutableList<LocalMedia> = ArrayList<LocalMedia>()
                val count = data.count
                if (count > 0) {
                    data.moveToFirst()
                    do {
                        val id = data.getLong(data.getColumnIndexOrThrow(PROJECTION[0]))
                        val absolutePath = data.getString(data.getColumnIndexOrThrow(PROJECTION[1]))
                        val url = if (isAndroidQ) getRealPathAndroid_Q(id) else absolutePath
                        var mimeType = data.getString(data.getColumnIndexOrThrow(PROJECTION[2]))
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
                        val width = data.getInt(data.getColumnIndexOrThrow(PROJECTION[3]))
                        val height = data.getInt(data.getColumnIndexOrThrow(PROJECTION[4]))
                        val duration = data.getLong(data.getColumnIndexOrThrow(PROJECTION[5]))
                        val size = data.getLong(data.getColumnIndexOrThrow(PROJECTION[6]))
                        val folderName = data.getString(data.getColumnIndexOrThrow(PROJECTION[7]))
                        val fileName = data.getString(data.getColumnIndexOrThrow(PROJECTION[8]))
                        val bucketId = data.getLong(data.getColumnIndexOrThrow(PROJECTION[9]))
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
                        val image = LocalMedia(id, url, absolutePath, fileName, folderName, duration, config.chooseMode, mimeType, width, height, size, bucketId)
                        val folder: LocalMediaFolder = getImageFolder(url, folderName, imageFolders)
                        folder.setBucketId(image.getBucketId())
                        val images: MutableList<LocalMedia> = folder.getData()
                        images.add(image)
                        folder.setImageNum(folder.getImageNum() + 1)
                        folder.setBucketId(image.getBucketId())
                        latelyImages.add(image)
                        val imageNum: Int = allImageFolder.getImageNum()
                        allImageFolder.setImageNum(imageNum + 1)
                    } while (data.moveToNext())
                    if (latelyImages.size > 0) {
                        sortFolder(imageFolders)
                        imageFolders.add(0, allImageFolder)
                        allImageFolder.setFirstImagePath(latelyImages[0].getPath())
                        val title = if (config.chooseMode == PictureMimeType.ofAudio()) mContext.getString(R.string.picture_all_audio) else mContext.getString(R.string.picture_camera_roll)
                        allImageFolder.setName(title)
                        allImageFolder.setBucketId(-1)
                        allImageFolder.setOfAllType(config.chooseMode)
                        allImageFolder.setCameraFolder(true)
                        allImageFolder.setData(latelyImages)
                    }
                }
                return imageFolders
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
        return null
    }

    private fun getSelection(): String? {
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL ->                 // Get all, not including audio
                return getSelectionArgsForAllMediaCondition(getDurationCondition(0, 0), config.isGif)
            PictureConfig.TYPE_IMAGE -> {
                if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Gets the image of the specified type
                    return SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "'"
                }
                return if (config.isGif) SELECTION else SELECTION_NOT_GIF
            }
            PictureConfig.TYPE_VIDEO -> {
                // Access to video
                return if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Gets the image of the specified type
                    SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "'"
                } else getSelectionArgsForSingleMediaCondition()
            }
            PictureConfig.TYPE_AUDIO -> {
                // Access to the audio
                return if (!TextUtils.isEmpty(config.specifiedFormat)) {
                    // Gets the image of the specified type
                    SELECTION_SPECIFIED_FORMAT + "='" + config.specifiedFormat + "'"
                } else getSelectionArgsForSingleMediaCondition(getDurationCondition(0, AUDIO_DURATION.toLong()))
            }
        }
        return null
    }

    private fun getSelectionArgs(): Array<String>? {
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL -> return SELECTION_ALL_ARGS
            PictureConfig.TYPE_IMAGE ->                 // Get Image
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
            PictureConfig.TYPE_VIDEO ->                 // Get Video
                return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            PictureConfig.TYPE_AUDIO -> return getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO)
        }
        return null
    }

    /**
     * Sort by the number of files
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
     * Android Q
     *
     * @param id
     * @return
     */
    private fun getRealPathAndroid_Q(id: Long): String {
        return QUERY_URI.buildUpon().appendPath(ValueOf.toString(id)).build().toString()
    }

    /**
     * Create folder
     *
     * @param path
     * @param imageFolders
     * @param folderName
     * @return
     */
    private fun getImageFolder(path: String, folderName: String, imageFolders: MutableList<LocalMediaFolder>): LocalMediaFolder {
        return if (!config.isFallbackVersion) {
            for (folder in imageFolders) {
                // Under the same folder, return yourself, otherwise create a new folder
                val name: String = folder.getName()
                if (TextUtils.isEmpty(name)) {
                    continue
                }
                if (name == folderName) {
                    return folder
                }
            }
            val newFolder = LocalMediaFolder()
            newFolder.setName(folderName)
            newFolder.setFirstImagePath(path)
            imageFolders.add(newFolder)
            newFolder
        } else {
            // Fault-tolerant processing
            val imageFile = File(path)
            val folderFile = imageFile.parentFile
            for (folder in imageFolders) {
                // Under the same folder, return yourself, otherwise create a new folder
                val name: String = folder.getName()
                if (TextUtils.isEmpty(name)) {
                    continue
                }
                if (folderFile != null && name == folderFile.name) {
                    return folder
                }
            }
            val newFolder = LocalMediaFolder()
            newFolder.setName(if (folderFile != null) folderFile.name else "")
            newFolder.setFirstImagePath(path)
            imageFolders.add(newFolder)
            newFolder
        }
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
        private val TAG = LocalMediaLoader::class.java.simpleName
        private val QUERY_URI: Uri = MediaStore.Files.getContentUri("external")
        private val ORDER_BY: String = MediaStore.Files.FileColumns._ID + " DESC"
        private const val NOT_GIF = "!='image/gif'"

        /**
         * Filter out recordings that are less than 500 milliseconds long
         */
        private const val AUDIO_DURATION = 500

        /**
         * unit
         */
        private const val FILE_SIZE_UNIT = 1024 * 1024L

        /**
         * Media file database field
         */
        private val PROJECTION = arrayOf<String>(
                MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.MediaColumns.DURATION,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.BUCKET_ID)

        /**
         * Image
         */
        private val SELECTION: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0")
        private val SELECTION_NOT_GIF: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                + " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)

        /**
         * Queries for images with the specified suffix
         */
        private val SELECTION_SPECIFIED_FORMAT: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                + " AND " + MediaStore.MediaColumns.MIME_TYPE)

        /**
         * Query criteria (audio and video)
         *
         * @param time_condition
         * @return
         */
        private fun getSelectionArgsForSingleMediaCondition(time_condition: String): String {
            return (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + time_condition)
        }

        /**
         * Query (video)
         *
         * @return
         */
        private fun getSelectionArgsForSingleMediaCondition(): String {
            return (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0")
        }

        /**
         * Query conditions in all modes
         *
         * @param time_condition
         * @param isGif
         * @return
         */
        private fun getSelectionArgsForAllMediaCondition(time_condition: String, isGif: Boolean): String {
            return ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + (if (isGif) "" else " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                    + " OR "
                    + (MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + time_condition) + ")"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0")
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
    }

    init {
        mContext = context.applicationContext
        isAndroidQ = SdkVersionUtils.checkedAndroid_Q()
        this.config = config
    }
}