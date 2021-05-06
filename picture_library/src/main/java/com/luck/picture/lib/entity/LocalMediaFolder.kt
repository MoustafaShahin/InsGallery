package com.luck.picture.lib.entity

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * @author：luck
 * @date：2016-12-31 15:21
 * @describe：MediaFolder Entity
 */
class LocalMediaFolder : Parcelable {
    /**
     * bucketId
     */
    private var bucketId: Long = -1

    /**
     * Folder name
     */
    private var name: String? = null

    /**
     * Folder first path
     */
    private var firstImagePath: String? = null

    /**
     * Folder media num
     */
    private var imageNum = 0

    /**
     * If the selected num
     */
    private var checkedNum = 0

    /**
     * If the selected
     */
    private var isChecked = false

    /**
     * type
     */
    private var ofAllType = -1

    /**
     * Whether or not the camera
     */
    private var isCameraFolder = false

    /**
     * data
     */
    private var data: List<LocalMedia> = ArrayList<LocalMedia>()

    /**
     * # Internal use
     * setCurrentDataPage
     */
    private var currentDataPage = 0

    /**
     * # Internal use
     * is load more
     */
    private var isHasMore = false
    fun getBucketId(): Long {
        return bucketId
    }

    fun setBucketId(bucketId: Long) {
        this.bucketId = bucketId
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String?) {
        this.name = name
    }

    fun getFirstImagePath(): String? {
        return firstImagePath
    }

    fun setFirstImagePath(firstImagePath: String?) {
        this.firstImagePath = firstImagePath
    }

    fun getImageNum(): Int {
        return imageNum
    }

    fun setImageNum(imageNum: Int) {
        this.imageNum = imageNum
    }

    fun getCheckedNum(): Int {
        return checkedNum
    }

    fun setCheckedNum(checkedNum: Int) {
        this.checkedNum = checkedNum
    }

    fun isChecked(): Boolean {
        return isChecked
    }

    fun setChecked(checked: Boolean) {
        isChecked = checked
    }

    fun getOfAllType(): Int {
        return ofAllType
    }

    fun setOfAllType(ofAllType: Int) {
        this.ofAllType = ofAllType
    }

    fun isCameraFolder(): Boolean {
        return isCameraFolder
    }

    fun setCameraFolder(cameraFolder: Boolean) {
        isCameraFolder = cameraFolder
    }

    fun getData(): List<LocalMedia> {
        return data
    }

    fun setData(data: List<LocalMedia>) {
        this.data = data
    }

    fun getCurrentDataPage(): Int {
        return currentDataPage
    }

    fun setCurrentDataPage(currentDataPage: Int) {
        this.currentDataPage = currentDataPage
    }

    fun isHasMore(): Boolean {
        return isHasMore
    }

    fun setHasMore(hasMore: Boolean) {
        isHasMore = hasMore
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(bucketId)
        dest.writeString(name)
        dest.writeString(firstImagePath)
        dest.writeInt(imageNum)
        dest.writeInt(checkedNum)
        dest.writeByte(if (isChecked) 1.toByte() else 0.toByte())
        dest.writeInt(ofAllType)
        dest.writeByte(if (isCameraFolder) 1.toByte() else 0.toByte())
        dest.writeTypedList(data)
        dest.writeInt(currentDataPage)
        dest.writeByte(if (isHasMore) 1.toByte() else 0.toByte())
    }

    constructor() {}
    protected constructor(`in`: Parcel) {
        bucketId = `in`.readLong()
        name = `in`.readString()
        firstImagePath = `in`.readString()
        imageNum = `in`.readInt()
        checkedNum = `in`.readInt()
        isChecked = `in`.readByte().toInt() != 0
        ofAllType = `in`.readInt()
        isCameraFolder = `in`.readByte().toInt() != 0
        data = `in`.createTypedArrayList<LocalMedia>(LocalMedia.CREATOR)!!
        currentDataPage = `in`.readInt()
        isHasMore = `in`.readByte().toInt() != 0
    }

    companion object {
        val CREATOR: Parcelable.Creator<LocalMediaFolder> = object : Parcelable.Creator<LocalMediaFolder?> {
            override fun createFromParcel(source: Parcel): LocalMediaFolder? {
                return LocalMediaFolder(source)
            }

            override fun newArray(size: Int): Array<LocalMediaFolder?> {
                return arrayOfNulls(size)
            }
        }
    }
}