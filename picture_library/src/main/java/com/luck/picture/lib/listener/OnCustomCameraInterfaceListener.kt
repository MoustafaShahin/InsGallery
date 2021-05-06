package com.luck.picture.lib.listener

import android.content.Context
import com.luck.picture.lib.config.PictureSelectionConfig

/**
 * @author：luck
 * @date：2020/4/27 3:24 PM
 * @describe：OnCustomCameraInterfaceListener
 */
interface OnCustomCameraInterfaceListener {
    /**
     * Camera Menu
     *
     * @param context
     * @param config
     * @param type
     */
    fun onCameraClick(context: Context?, config: PictureSelectionConfig?, type: Int)
}