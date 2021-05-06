package com.luck.picture.lib.entity

/**
 * @author：luck
 * @date：2020-04-17 13:52
 * @describe：MediaData
 */
class MediaData {
    /**
     * Is there more
     */
    var isHasNextMore = false

    /**
     * data
     */
    var data: List<LocalMedia>? = null

    constructor() : super() {}
    constructor(isHasNextMore: Boolean, data: List<LocalMedia>?) : super() {
        this.isHasNextMore = isHasNextMore
        this.data = data
    }
}