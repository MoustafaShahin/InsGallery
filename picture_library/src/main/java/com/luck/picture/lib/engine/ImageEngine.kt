package com.luck.picture.lib.engine

import android.content.Context
import android.widget.ImageView
import com.luck.picture.lib.listener.OnImageCompleteCallback

/**
 * @author：luck
 * @date：2019-11-13 16:59
 * @describe：ImageEngine
 */
interface ImageEngine {
    /**
     * Loading image
     *
     * @param context
     * @param url
     * @param imageView
     */
    fun loadImage(context: Context, url: String, imageView: ImageView)

    /**
     * Loading image
     *
     * @param context
     * @param url
     * @param imageView
     */
    fun loadImage(context: Context, url: String, imageView: ImageView, longImageView: SubsamplingScaleImageView?, callback: OnImageCompleteCallback?)

    /**
     * Load network long graph adaption
     *
     * @param context
     * @param url
     * @param imageView
     */
    @Deprecated("")
    fun loadImage(context: Context, url: String, imageView: ImageView, longImageView: SubsamplingScaleImageView?)

    /**
     * Load album catalog pictures
     *
     * @param context
     * @param url
     * @param imageView
     */
    fun loadFolderImage(context: Context, url: String, imageView: ImageView)

    /**
     * Load GIF image
     *
     * @param context
     * @param url
     * @param imageView
     */
    fun loadAsGifImage(context: Context, url: String, imageView: ImageView)

    /**
     * Load picture list picture
     *
     * @param context
     * @param url
     * @param imageView
     */
    fun loadGridImage(context: Context, url: String, imageView: ImageView)
}