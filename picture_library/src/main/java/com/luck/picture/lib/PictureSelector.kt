package com.luck.picture.lib

import androidx.fragment.app.Fragment
import com.luck.picture.lib.config.PictureConfig
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author：luck
 * @date：2017-5-24 22:30
 * @describe：PictureSelector
 */
class PictureSelector private constructor(activity: Activity?, fragment: Fragment? = null) {
    private val mActivity: WeakReference<Activity?>
    private val mFragment: WeakReference<Fragment?>?

    private constructor(fragment: Fragment) : this(fragment.activity, fragment) {}

    /**
     * @param chooseMode Select the type of picture you want，all or Picture or Video .
     * @return LocalMedia PictureSelectionModel
     * Use [].
     */
    fun openGallery(chooseMode: Int): PictureSelectionModel {
        return PictureSelectionModel(this, chooseMode)
    }

    /**
     * @param chooseMode Select the type of picture you want，Picture or Video.
     * @return LocalMedia PictureSelectionModel
     * Use [].
     */
    fun openCamera(chooseMode: Int): PictureSelectionModel {
        return PictureSelectionModel(this, chooseMode, true)
    }

    /**
     * 外部预览时设置样式
     *
     * @param themeStyle
     * @return
     */
    fun themeStyle(themeStyle: Int): PictureSelectionModel {
        return PictureSelectionModel(this, PictureMimeType.ofImage())
                .theme(themeStyle)
    }

    /**
     * 外部预览时动态代码设置样式
     *
     * @param style
     * @return
     */
    fun setPictureStyle(style: PictureParameterStyle): PictureSelectionModel {
        return PictureSelectionModel(this, PictureMimeType.ofImage())
                .setPictureStyle(style)
    }

    /**
     * set preview image
     *
     * @param position
     * @param medias
     */
    fun externalPicturePreview(position: Int, medias: List<LocalMedia?>?, enterAnimation: Int) {
        if (!DoubleUtils.isFastDoubleClick()) {
            if (activity != null) {
                val intent = Intent(activity, PictureExternalPreviewActivity::class.java)
                intent.putParcelableArrayListExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST,
                        medias as ArrayList<out Parcelable?>?)
                intent.putExtra(PictureConfig.EXTRA_POSITION, position)
                activity.startActivity(intent)
                activity.overridePendingTransition(if (enterAnimation != 0) enterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
            } else {
                throw NullPointerException("Starting the PictureSelector Activity cannot be empty ")
            }
        }
    }

    /**
     * set preview image
     *
     * @param position
     * @param medias
     * @param directory_path
     */
    fun externalPicturePreview(position: Int, directory_path: String?, medias: List<LocalMedia?>?, enterAnimation: Int) {
        if (!DoubleUtils.isFastDoubleClick()) {
            if (activity != null) {
                val intent = Intent(activity, PictureExternalPreviewActivity::class.java)
                intent.putParcelableArrayListExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST, medias as ArrayList<out Parcelable?>?)
                intent.putExtra(PictureConfig.EXTRA_POSITION, position)
                intent.putExtra(PictureConfig.EXTRA_DIRECTORY_PATH, directory_path)
                activity.startActivity(intent)
                activity.overridePendingTransition(if (enterAnimation != 0) enterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
            } else {
                throw NullPointerException("Starting the PictureSelector Activity cannot be empty ")
            }
        }
    }

    /**
     * set preview video
     *
     * @param path
     */
    fun externalPictureVideo(path: String?) {
        if (!DoubleUtils.isFastDoubleClick()) {
            if (activity != null) {
                val intent = Intent(activity, PictureVideoPlayActivity::class.java)
                intent.putExtra(PictureConfig.EXTRA_VIDEO_PATH, path)
                intent.putExtra(PictureConfig.EXTRA_PREVIEW_VIDEO, true)
                activity.startActivity(intent)
            } else {
                throw NullPointerException("Starting the PictureSelector Activity cannot be empty ")
            }
        }
    }

    /**
     * set preview audio
     *
     * @param path
     */
    fun externalPictureAudio(path: String?) {
        if (!DoubleUtils.isFastDoubleClick()) {
            if (activity != null) {
                val intent = Intent(activity, PicturePlayAudioActivity::class.java)
                intent.putExtra(PictureConfig.EXTRA_AUDIO_PATH, path)
                activity.startActivity(intent)
                activity.overridePendingTransition(R.anim.picture_anim_enter, 0)
            } else {
                throw NullPointerException("Starting the PictureSelector Activity cannot be empty ")
            }
        }
    }

    /**
     * @return Activity.
     */
    val activity: Activity?
        get() = mActivity.get()

    /**
     * @return Fragment.
     */
    val fragment: Fragment?
        get() = mFragment?.get()

    companion object {
        /**
         * Start PictureSelector for Activity.
         *
         * @param activity
         * @return PictureSelector instance.
         */
        fun create(activity: Activity): PictureSelector {
            return PictureSelector(activity)
        }

        /**
         * Start PictureSelector for Fragment.
         *
         * @param fragment
         * @return PictureSelector instance.
         */
        fun create(fragment: Fragment): PictureSelector {
            return PictureSelector(fragment)
        }

        /**
         * @param data
         * @return Selector Multiple LocalMedia
         */
        fun obtainMultipleResult(data: Intent?): List<LocalMedia> {
            if (data != null) {
                val result: List<LocalMedia> = data.getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_RESULT_SELECTION)
                return result ?: ArrayList<LocalMedia>()
            }
            return ArrayList<LocalMedia>()
        }

        /**
         * @param data
         * @return Put image Intent Data
         */
        fun putIntentResult(data: List<LocalMedia?>?): Intent {
            return Intent().putParcelableArrayListExtra(PictureConfig.EXTRA_RESULT_SELECTION,
                    data as ArrayList<out Parcelable?>?)
        }

        /**
         * @param bundle
         * @return get Selector  LocalMedia
         */
        fun obtainSelectorList(bundle: Bundle?): MutableList<LocalMedia?> {
            if (bundle != null) {
                val selectionMedias: List<LocalMedia> = bundle.getParcelableArrayList<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
                return selectionMedias ?: ArrayList<LocalMedia?>()
            }
            return ArrayList<LocalMedia?>()
        }

        /**
         * @param selectedImages
         * @return put Selector  LocalMedia
         */
        fun saveSelectorList(outState: Bundle, selectedImages: List<LocalMedia?>?) {
            outState.putParcelableArrayList(PictureConfig.EXTRA_SELECT_LIST,
                    selectedImages as ArrayList<out Parcelable?>?)
        }
    }

    init {
        mActivity = WeakReference<Activity?>(activity)
        mFragment = WeakReference(fragment)
    }
}