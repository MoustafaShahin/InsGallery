package com.luck.picture.lib.entity

import com.luck.picture.lib.config.PictureConfig

/**
 * @author：luck
 * @date：2017-5-24 16:21
 * @describe：Media Entity
 */
class LocalMedia : Parcelable {
    /**
     * file to ID
     */
    private var id: Long = 0

    /**
     * original path
     */
    private var path: String? = null
    private var coverPath: String? = null

    /**
     * The real path，But you can't get access from AndroidQ
     *
     *
     * It could be empty
     *
     *
     */
    private var realPath: String? = null

    /**
     * # Check the original button to get the return value
     * original path
     */
    private var originalPath: String? = null

    /**
     * compress path
     */
    private var compressPath: String? = null

    /**
     * cut path
     */
    private var cutPath: String? = null

    /**
     * Note: this field is only returned in Android Q version
     *
     *
     * Android Q image or video path
     */
    private var androidQToPath: String? = null

    /**
     * video duration
     */
    private var duration: Long = 0

    /**
     * If the selected
     * # Internal use
     */
    private var isChecked = false

    /**
     * If the cut
     */
    private var isCut = false

    /**
     * media position of list
     */
    var position = 0

    /**
     * The media number of qq choose styles
     */
    private var num = 0

    /**
     * The media resource type
     */
    private var mimeType: String? = null

    /**
     * Gallery selection mode
     */
    private var chooseModel = 0

    /**
     * If the compressed
     */
    private var compressed = false

    /**
     * image or video width
     *
     *
     * # If zero occurs, the developer needs to handle it extra
     */
    private var width = 0

    /**
     * image or video height
     *
     *
     * # If zero occurs, the developer needs to handle it extra
     */
    private var height = 0

    /**
     * file size
     */
    private var size: Long = 0

    /**
     * Whether the original image is displayed
     */
    private var isOriginal = false

    /**
     * file name
     */
    private var fileName: String? = null

    /**
     * Parent  Folder Name
     */
    private var parentFolderName: String? = null

    /**
     * orientation info
     * # For internal use only
     */
    private var orientation = -1

    /**
     * loadLongImageStatus
     * # For internal use only
     */
    var loadLongImageStatus: Int = PictureConfig.NORMAL

    /**
     * isLongImage
     * # For internal use only
     */
    var isLongImage = false

    /**
     * bucketId
     */
    private var bucketId: Long = -1

    /**
     * isMaxSelectEnabledMask
     * # For internal use only
     */
    private var isMaxSelectEnabledMask = false

    constructor() {}
    constructor(path: String?, duration: Long, chooseModel: Int, mimeType: String?) {
        this.path = path
        this.duration = duration
        this.chooseModel = chooseModel
        this.mimeType = mimeType
    }

    constructor(id: Long, path: String?, fileName: String?, parentFolderName: String?, duration: Long, chooseModel: Int,
                mimeType: String?, width: Int, height: Int, size: Long) {
        this.id = id
        this.path = path
        this.fileName = fileName
        this.parentFolderName = parentFolderName
        this.duration = duration
        this.chooseModel = chooseModel
        this.mimeType = mimeType
        this.width = width
        this.height = height
        this.size = size
    }

    constructor(id: Long, path: String?, absolutePath: String?, fileName: String?, parentFolderName: String?, duration: Long, chooseModel: Int,
                mimeType: String?, width: Int, height: Int, size: Long, bucketId: Long) {
        this.id = id
        this.path = path
        realPath = absolutePath
        this.fileName = fileName
        this.parentFolderName = parentFolderName
        this.duration = duration
        this.chooseModel = chooseModel
        this.mimeType = mimeType
        this.width = width
        this.height = height
        this.size = size
        this.bucketId = bucketId
    }

    constructor(path: String?, duration: Long,
                isChecked: Boolean, position: Int, num: Int, chooseModel: Int) {
        this.path = path
        this.duration = duration
        this.isChecked = isChecked
        this.position = position
        this.num = num
        this.chooseModel = chooseModel
    }

    fun getPath(): String? {
        return path
    }

    fun setPath(path: String?) {
        this.path = path
    }

    fun getCoverPath(): String? {
        return coverPath
    }

    fun setCoverPath(coverPath: String?) {
        this.coverPath = coverPath
    }

    fun getCompressPath(): String? {
        return compressPath
    }

    fun setCompressPath(compressPath: String?) {
        this.compressPath = compressPath
    }

    fun getCutPath(): String? {
        return cutPath
    }

    fun setCutPath(cutPath: String?) {
        this.cutPath = cutPath
    }

    fun getAndroidQToPath(): String? {
        return androidQToPath
    }

    fun setAndroidQToPath(androidQToPath: String?) {
        this.androidQToPath = androidQToPath
    }

    fun getDuration(): Long {
        return duration
    }

    fun setDuration(duration: Long) {
        this.duration = duration
    }

    fun getRealPath(): String? {
        return realPath
    }

    fun setRealPath(realPath: String?) {
        this.realPath = realPath
    }

    fun isChecked(): Boolean {
        return isChecked
    }

    fun setChecked(checked: Boolean) {
        isChecked = checked
    }

    fun isCut(): Boolean {
        return isCut
    }

    fun setCut(cut: Boolean) {
        isCut = cut
    }

    fun getPosition(): Int {
        return position
    }

    fun setPosition(position: Int) {
        this.position = position
    }

    fun getNum(): Int {
        return num
    }

    fun setNum(num: Int) {
        this.num = num
    }

    fun getMimeType(): String {
        return if (TextUtils.isEmpty(mimeType)) "image/jpeg" else mimeType!!
    }

    fun setMimeType(mimeType: String?) {
        this.mimeType = mimeType
    }

    fun isCompressed(): Boolean {
        return compressed
    }

    fun setCompressed(compressed: Boolean) {
        this.compressed = compressed
    }

    fun getWidth(): Int {
        return width
    }

    fun setWidth(width: Int) {
        this.width = width
    }

    fun getHeight(): Int {
        return height
    }

    fun setHeight(height: Int) {
        this.height = height
    }

    fun getChooseModel(): Int {
        return chooseModel
    }

    fun setChooseModel(chooseModel: Int) {
        this.chooseModel = chooseModel
    }

    fun getSize(): Long {
        return size
    }

    fun setSize(size: Long) {
        this.size = size
    }

    fun isOriginal(): Boolean {
        return isOriginal
    }

    fun setOriginal(original: Boolean) {
        isOriginal = original
    }

    fun getOriginalPath(): String? {
        return originalPath
    }

    fun setOriginalPath(originalPath: String?) {
        this.originalPath = originalPath
    }

    fun getFileName(): String? {
        return fileName
    }

    fun setFileName(fileName: String?) {
        this.fileName = fileName
    }

    fun getId(): Long {
        return id
    }

    fun setId(id: Long) {
        this.id = id
    }

    fun getParentFolderName(): String? {
        return parentFolderName
    }

    fun setParentFolderName(parentFolderName: String?) {
        this.parentFolderName = parentFolderName
    }

    fun getOrientation(): Int {
        return orientation
    }

    fun setOrientation(orientation: Int) {
        this.orientation = orientation
    }

    fun getBucketId(): Long {
        return bucketId
    }

    fun setBucketId(bucketId: Long) {
        this.bucketId = bucketId
    }

    fun isMaxSelectEnabledMask(): Boolean {
        return isMaxSelectEnabledMask
    }

    fun setMaxSelectEnabledMask(maxSelectEnabledMask: Boolean) {
        isMaxSelectEnabledMask = maxSelectEnabledMask
    }

    override fun toString(): String {
        return "LocalMedia{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", cutPath='" + cutPath + '\'' +
                ", androidQToPath='" + androidQToPath + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", size=" + size +
                '}'
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(path)
        dest.writeString(coverPath)
        dest.writeString(realPath)
        dest.writeString(originalPath)
        dest.writeString(compressPath)
        dest.writeString(cutPath)
        dest.writeString(androidQToPath)
        dest.writeLong(duration)
        dest.writeByte(if (isChecked) 1.toByte() else 0.toByte())
        dest.writeByte(if (isCut) 1.toByte() else 0.toByte())
        dest.writeInt(position)
        dest.writeInt(num)
        dest.writeString(mimeType)
        dest.writeInt(chooseModel)
        dest.writeByte(if (compressed) 1.toByte() else 0.toByte())
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeLong(size)
        dest.writeByte(if (isOriginal) 1.toByte() else 0.toByte())
        dest.writeString(fileName)
        dest.writeString(parentFolderName)
        dest.writeInt(orientation)
        dest.writeInt(loadLongImageStatus)
        dest.writeByte(if (isLongImage) 1.toByte() else 0.toByte())
        dest.writeLong(bucketId)
        dest.writeByte(if (isMaxSelectEnabledMask) 1.toByte() else 0.toByte())
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readLong()
        path = `in`.readString()
        coverPath = `in`.readString()
        realPath = `in`.readString()
        originalPath = `in`.readString()
        compressPath = `in`.readString()
        cutPath = `in`.readString()
        androidQToPath = `in`.readString()
        duration = `in`.readLong()
        isChecked = `in`.readByte().toInt() != 0
        isCut = `in`.readByte().toInt() != 0
        position = `in`.readInt()
        num = `in`.readInt()
        mimeType = `in`.readString()
        chooseModel = `in`.readInt()
        compressed = `in`.readByte().toInt() != 0
        width = `in`.readInt()
        height = `in`.readInt()
        size = `in`.readLong()
        isOriginal = `in`.readByte().toInt() != 0
        fileName = `in`.readString()
        parentFolderName = `in`.readString()
        orientation = `in`.readInt()
        loadLongImageStatus = `in`.readInt()
        isLongImage = `in`.readByte().toInt() != 0
        bucketId = `in`.readLong()
        isMaxSelectEnabledMask = `in`.readByte().toInt() != 0
    }

    companion object {
        val CREATOR: Parcelable.Creator<LocalMedia> = object : Parcelable.Creator<LocalMedia?> {
            override fun createFromParcel(source: Parcel): LocalMedia {
                return LocalMedia(source)
            }

            override fun newArray(size: Int): Array<LocalMedia> {
                return arrayOfNulls(size)
            }
        }
    }
}