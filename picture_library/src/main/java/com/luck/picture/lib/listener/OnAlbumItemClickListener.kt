package com.luck.picture.lib.listener

import com.luck.picture.lib.entity.LocalMedia

/**
 * @author：luck
 * @date：2020-03-26 10:57
 * @describe：OnAlbumItemClickListener
 */
interface OnAlbumItemClickListener {
    /**
     * Album catalog item click event
     *
     * @param position
     * @param isCameraFolder
     * @param bucketId
     * @param folderName
     * @param data
     */
    fun onItemClick(position: Int, isCameraFolder: Boolean,
                    bucketId: Long, folderName: String?, data: List<LocalMedia?>?)
}