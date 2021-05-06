package com.luck.picture.lib.config

import com.yalantis.ucrop.UCrop

/**
 * @author：luck
 * @date：2020-01-09 13:33
 * @describe： UCrop Configuration items
 */
class UCropOptions : UCrop.Options, Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {}

    constructor() {}
    protected constructor(`in`: Parcel?) {}

    companion object {
        val CREATOR: Parcelable.Creator<UCropOptions> = object : Parcelable.Creator<UCropOptions?> {
            override fun createFromParcel(source: Parcel): UCropOptions {
                return UCropOptions(source)
            }

            override fun newArray(size: Int): Array<UCropOptions> {
                return arrayOfNulls(size)
            }
        }
    }
}