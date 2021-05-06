package com.luck.picture.lib.config

import android.os.Parcelable
import java.util.*

/**
 * @author：luck
 * @date：2017-05-24 17:02
 * @describe：PictureSelector Config
 */
class PictureSelectionConfig : Parcelable {
    var chooseMode = 0
    var camera = false
    var isSingleDirectReturn = false
    var style: PictureParameterStyle? = null
    var cropStyle: PictureCropParameterStyle? = null
    var windowAnimationStyle: PictureWindowAnimationStyle? = null
    var instagramSelectionConfig: InstagramSelectionConfig? = null
    var compressSavePath: String? = null
    var suffixType: String? = null
    var focusAlpha = false
    var renameCompressFileName: String? = null
    var renameCropFileName: String? = null
    var specifiedFormat: String? = null
    var requestedOrientation = 0
    var buttonFeatures = 0
    var isCameraAroundState = false
    var isAndroidQTransform = false

    @StyleRes
    var themeStyleId = 0
    var selectionMode = 0
    var maxSelectNum = 0
    var minSelectNum = 0
    var maxVideoSelectNum = 0
    var minVideoSelectNum = 0
    var videoQuality = 0
    var cropCompressQuality = 0
    var videoMaxSecond = 0
    var videoMinSecond = 0
    var recordVideoSecond = 0
    var recordVideoMinSecond = 0
    var minimumCompressSize = 0
    var imageSpanCount = 0
    var aspect_ratio_x = 0
    var aspect_ratio_y = 0
    var cropWidth = 0
    var cropHeight = 0
    var compressQuality = 0
    var filterFileSize = 0f
    var language = 0
    var isMultipleRecyclerAnimation = false
    var isMultipleSkipCrop = false
    var isWeChatStyle = false
    var isUseCustomCamera = false
    var zoomAnim = false
    var isCompress = false
    var isOriginalControl = false
    var isCamera = false
    var isGif = false
    var enablePreview = false
    var enPreviewVideo = false
    var enablePreviewAudio = false
    var checkNumMode = false
    var openClickSound = false
    var enableCrop = false
    var freeStyleCropEnabled = false
    var circleDimmedLayer = false

    @ColorInt
    var circleDimmedColor = 0

    @ColorInt
    var circleDimmedBorderColor = 0
    var circleStrokeWidth = 0
    var showCropFrame = false
    var showCropGrid = false
    var hideBottomControls = false
    var rotateEnabled = false
    var scaleEnabled = false
    var previewEggs = false
    var synOrAsy = false
    var returnEmpty = false
    var isDragFrame = false
    var isNotPreviewDownload = false
    var isWithVideoImage = false
    var uCropOptions: com.luck.picture.lib.config.UCropOptions? = null
    var selectionMedias: List<LocalMedia>? = null
    var cameraFileName: String? = null
    var isCheckOriginalImage = false

    @Deprecated("")
    var overrideWidth = 0

    @Deprecated("")
    var overrideHeight = 0

    @Deprecated("")
    var sizeMultiplier = 0f

    @Deprecated("")
    var isChangeStatusBarFontColor = false

    @Deprecated("")
    var isOpenStyleNumComplete = false

    @Deprecated("")
    var isOpenStyleCheckNumMode = false

    @Deprecated("")
    var titleBarBackgroundColor = 0

    @Deprecated("")
    var pictureStatusBarColor = 0

    @Deprecated("")
    var cropTitleBarBackgroundColor = 0

    @Deprecated("")
    var cropStatusBarColorPrimaryDark = 0

    @Deprecated("")
    var cropTitleColor = 0

    @Deprecated("")
    var upResId = 0

    @Deprecated("")
    var downResId = 0
    var outPutCameraPath: String? = null
    var originalPath: String? = null
    var cameraPath: String? = null
    var cameraMimeType = 0
    var pageSize = 0
    var isPageStrategy = false
    var isFilterInvalidFile = false
    var isMaxSelectEnabledMask = false
    var animationMode = 0
    var isAutomaticTitleRecyclerTop = false
    var isCallbackMode = false
    var isAndroidQChangeWH = false
    var isAndroidQChangeVideoWH = false
    var isQuickCapture = false

    /**
     * 内测专用###########
     */
    var isFallbackVersion = false
    var isFallbackVersion2 = false
    var isFallbackVersion3 = false
    protected fun initDefaultValue() {
        chooseMode = com.luck.picture.lib.config.PictureMimeType.ofImage()
        camera = false
        themeStyleId = R.style.picture_default_style
        selectionMode = com.luck.picture.lib.config.PictureConfig.MULTIPLE
        maxSelectNum = 9
        minSelectNum = 0
        maxVideoSelectNum = 0
        minVideoSelectNum = 0
        videoQuality = 1
        language = -1
        cropCompressQuality = 90
        videoMaxSecond = 0
        videoMinSecond = 0
        filterFileSize = -1f
        recordVideoSecond = 60
        recordVideoMinSecond = 0
        compressQuality = 80
        minimumCompressSize = com.luck.picture.lib.config.PictureConfig.MAX_COMPRESS_SIZE
        imageSpanCount = 4
        isCompress = false
        isOriginalControl = false
        aspect_ratio_x = 0
        aspect_ratio_y = 0
        cropWidth = 0
        cropHeight = 0
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        buttonFeatures = CustomCameraView.BUTTON_STATE_BOTH //初始化按钮为可录制可拍照
        isCameraAroundState = false
        isWithVideoImage = false
        isAndroidQTransform = true
        isCamera = true
        isGif = false
        focusAlpha = false
        isCheckOriginalImage = false
        isSingleDirectReturn = false
        enablePreview = true
        enPreviewVideo = true
        enablePreviewAudio = true
        checkNumMode = false
        isNotPreviewDownload = false
        openClickSound = false
        isFallbackVersion = false
        isFallbackVersion2 = true
        isFallbackVersion3 = true
        enableCrop = false
        isWeChatStyle = false
        isUseCustomCamera = false
        isMultipleSkipCrop = true
        isMultipleRecyclerAnimation = true
        freeStyleCropEnabled = false
        circleDimmedLayer = false
        showCropFrame = true
        showCropGrid = true
        hideBottomControls = true
        rotateEnabled = true
        scaleEnabled = true
        previewEggs = false
        returnEmpty = false
        synOrAsy = true
        zoomAnim = true
        circleDimmedColor = 0
        circleDimmedBorderColor = 0
        circleStrokeWidth = 1
        isDragFrame = true
        compressSavePath = ""
        suffixType = ""
        cameraFileName = ""
        specifiedFormat = ""
        renameCompressFileName = ""
        renameCropFileName = ""
        selectionMedias = ArrayList<LocalMedia>()
        uCropOptions = null
        style = null
        cropStyle = null
        windowAnimationStyle = null
        titleBarBackgroundColor = 0
        pictureStatusBarColor = 0
        cropTitleBarBackgroundColor = 0
        cropStatusBarColorPrimaryDark = 0
        cropTitleColor = 0
        upResId = 0
        downResId = 0
        isChangeStatusBarFontColor = false
        isOpenStyleNumComplete = false
        isOpenStyleCheckNumMode = false
        outPutCameraPath = ""
        sizeMultiplier = 0.5f
        overrideWidth = 0
        overrideHeight = 0
        originalPath = ""
        cameraPath = ""
        instagramSelectionConfig = null
        cameraMimeType = -1
        pageSize = com.luck.picture.lib.config.PictureConfig.MAX_PAGE_SIZE
        isPageStrategy = true
        isFilterInvalidFile = false
        isMaxSelectEnabledMask = false
        animationMode = -1
        isAutomaticTitleRecyclerTop = true
        isCallbackMode = false
        isAndroidQChangeWH = true
        isAndroidQChangeVideoWH = false
        isQuickCapture = true
    }

    private object InstanceHolder {
        val instance = PictureSelectionConfig()
            get() = InstanceHolder.field
    }

    constructor() {}

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(chooseMode)
        dest.writeByte(if (camera) 1.toByte() else 0.toByte())
        dest.writeByte(if (isSingleDirectReturn) 1.toByte() else 0.toByte())
        dest.writeParcelable(style, flags)
        dest.writeParcelable(cropStyle, flags)
        dest.writeParcelable(windowAnimationStyle, flags)
        dest.writeParcelable(instagramSelectionConfig, flags)
        dest.writeString(compressSavePath)
        dest.writeString(suffixType)
        dest.writeByte(if (focusAlpha) 1.toByte() else 0.toByte())
        dest.writeString(renameCompressFileName)
        dest.writeString(renameCropFileName)
        dest.writeString(specifiedFormat)
        dest.writeInt(requestedOrientation)
        dest.writeInt(buttonFeatures)
        dest.writeByte(if (isCameraAroundState) 1.toByte() else 0.toByte())
        dest.writeByte(if (isAndroidQTransform) 1.toByte() else 0.toByte())
        dest.writeInt(themeStyleId)
        dest.writeInt(selectionMode)
        dest.writeInt(maxSelectNum)
        dest.writeInt(minSelectNum)
        dest.writeInt(maxVideoSelectNum)
        dest.writeInt(minVideoSelectNum)
        dest.writeInt(videoQuality)
        dest.writeInt(cropCompressQuality)
        dest.writeInt(videoMaxSecond)
        dest.writeInt(videoMinSecond)
        dest.writeInt(recordVideoSecond)
        dest.writeInt(recordVideoMinSecond)
        dest.writeInt(minimumCompressSize)
        dest.writeInt(imageSpanCount)
        dest.writeInt(aspect_ratio_x)
        dest.writeInt(aspect_ratio_y)
        dest.writeInt(cropWidth)
        dest.writeInt(cropHeight)
        dest.writeInt(compressQuality)
        dest.writeFloat(filterFileSize)
        dest.writeInt(language)
        dest.writeByte(if (isMultipleRecyclerAnimation) 1.toByte() else 0.toByte())
        dest.writeByte(if (isMultipleSkipCrop) 1.toByte() else 0.toByte())
        dest.writeByte(if (isWeChatStyle) 1.toByte() else 0.toByte())
        dest.writeByte(if (isUseCustomCamera) 1.toByte() else 0.toByte())
        dest.writeByte(if (zoomAnim) 1.toByte() else 0.toByte())
        dest.writeByte(if (isCompress) 1.toByte() else 0.toByte())
        dest.writeByte(if (isOriginalControl) 1.toByte() else 0.toByte())
        dest.writeByte(if (isCamera) 1.toByte() else 0.toByte())
        dest.writeByte(if (isGif) 1.toByte() else 0.toByte())
        dest.writeByte(if (enablePreview) 1.toByte() else 0.toByte())
        dest.writeByte(if (enPreviewVideo) 1.toByte() else 0.toByte())
        dest.writeByte(if (enablePreviewAudio) 1.toByte() else 0.toByte())
        dest.writeByte(if (checkNumMode) 1.toByte() else 0.toByte())
        dest.writeByte(if (openClickSound) 1.toByte() else 0.toByte())
        dest.writeByte(if (enableCrop) 1.toByte() else 0.toByte())
        dest.writeByte(if (freeStyleCropEnabled) 1.toByte() else 0.toByte())
        dest.writeByte(if (circleDimmedLayer) 1.toByte() else 0.toByte())
        dest.writeInt(circleDimmedColor)
        dest.writeInt(circleDimmedBorderColor)
        dest.writeInt(circleStrokeWidth)
        dest.writeByte(if (showCropFrame) 1.toByte() else 0.toByte())
        dest.writeByte(if (showCropGrid) 1.toByte() else 0.toByte())
        dest.writeByte(if (hideBottomControls) 1.toByte() else 0.toByte())
        dest.writeByte(if (rotateEnabled) 1.toByte() else 0.toByte())
        dest.writeByte(if (scaleEnabled) 1.toByte() else 0.toByte())
        dest.writeByte(if (previewEggs) 1.toByte() else 0.toByte())
        dest.writeByte(if (synOrAsy) 1.toByte() else 0.toByte())
        dest.writeByte(if (returnEmpty) 1.toByte() else 0.toByte())
        dest.writeByte(if (isDragFrame) 1.toByte() else 0.toByte())
        dest.writeByte(if (isNotPreviewDownload) 1.toByte() else 0.toByte())
        dest.writeByte(if (isWithVideoImage) 1.toByte() else 0.toByte())
        dest.writeParcelable(uCropOptions, flags)
        dest.writeTypedList(selectionMedias)
        dest.writeString(cameraFileName)
        dest.writeByte(if (isCheckOriginalImage) 1.toByte() else 0.toByte())
        dest.writeInt(overrideWidth)
        dest.writeInt(overrideHeight)
        dest.writeFloat(sizeMultiplier)
        dest.writeByte(if (isChangeStatusBarFontColor) 1.toByte() else 0.toByte())
        dest.writeByte(if (isOpenStyleNumComplete) 1.toByte() else 0.toByte())
        dest.writeByte(if (isOpenStyleCheckNumMode) 1.toByte() else 0.toByte())
        dest.writeInt(titleBarBackgroundColor)
        dest.writeInt(pictureStatusBarColor)
        dest.writeInt(cropTitleBarBackgroundColor)
        dest.writeInt(cropStatusBarColorPrimaryDark)
        dest.writeInt(cropTitleColor)
        dest.writeInt(upResId)
        dest.writeInt(downResId)
        dest.writeString(outPutCameraPath)
        dest.writeString(originalPath)
        dest.writeString(cameraPath)
        dest.writeInt(cameraMimeType)
        dest.writeInt(pageSize)
        dest.writeByte(if (isPageStrategy) 1.toByte() else 0.toByte())
        dest.writeByte(if (isFilterInvalidFile) 1.toByte() else 0.toByte())
        dest.writeByte(if (isMaxSelectEnabledMask) 1.toByte() else 0.toByte())
        dest.writeInt(animationMode)
        dest.writeByte(if (isAutomaticTitleRecyclerTop) 1.toByte() else 0.toByte())
        dest.writeByte(if (isCallbackMode) 1.toByte() else 0.toByte())
        dest.writeByte(if (isAndroidQChangeWH) 1.toByte() else 0.toByte())
        dest.writeByte(if (isAndroidQChangeVideoWH) 1.toByte() else 0.toByte())
        dest.writeByte(if (isQuickCapture) 1.toByte() else 0.toByte())
        dest.writeByte(if (isFallbackVersion) 1.toByte() else 0.toByte())
        dest.writeByte(if (isFallbackVersion2) 1.toByte() else 0.toByte())
        dest.writeByte(if (isFallbackVersion3) 1.toByte() else 0.toByte())
    }

    protected constructor(`in`: Parcel) {
        chooseMode = `in`.readInt()
        camera = `in`.readByte().toInt() != 0
        isSingleDirectReturn = `in`.readByte().toInt() != 0
        style = `in`.readParcelable(PictureParameterStyle::class.java.getClassLoader())
        cropStyle = `in`.readParcelable(PictureCropParameterStyle::class.java.getClassLoader())
        windowAnimationStyle = `in`.readParcelable(PictureWindowAnimationStyle::class.java.getClassLoader())
        instagramSelectionConfig = `in`.readParcelable(InstagramSelectionConfig::class.java.getClassLoader())
        compressSavePath = `in`.readString()
        suffixType = `in`.readString()
        focusAlpha = `in`.readByte().toInt() != 0
        renameCompressFileName = `in`.readString()
        renameCropFileName = `in`.readString()
        specifiedFormat = `in`.readString()
        requestedOrientation = `in`.readInt()
        buttonFeatures = `in`.readInt()
        isCameraAroundState = `in`.readByte().toInt() != 0
        isAndroidQTransform = `in`.readByte().toInt() != 0
        themeStyleId = `in`.readInt()
        selectionMode = `in`.readInt()
        maxSelectNum = `in`.readInt()
        minSelectNum = `in`.readInt()
        maxVideoSelectNum = `in`.readInt()
        minVideoSelectNum = `in`.readInt()
        videoQuality = `in`.readInt()
        cropCompressQuality = `in`.readInt()
        videoMaxSecond = `in`.readInt()
        videoMinSecond = `in`.readInt()
        recordVideoSecond = `in`.readInt()
        recordVideoMinSecond = `in`.readInt()
        minimumCompressSize = `in`.readInt()
        imageSpanCount = `in`.readInt()
        aspect_ratio_x = `in`.readInt()
        aspect_ratio_y = `in`.readInt()
        cropWidth = `in`.readInt()
        cropHeight = `in`.readInt()
        compressQuality = `in`.readInt()
        filterFileSize = `in`.readFloat()
        language = `in`.readInt()
        isMultipleRecyclerAnimation = `in`.readByte().toInt() != 0
        isMultipleSkipCrop = `in`.readByte().toInt() != 0
        isWeChatStyle = `in`.readByte().toInt() != 0
        isUseCustomCamera = `in`.readByte().toInt() != 0
        zoomAnim = `in`.readByte().toInt() != 0
        isCompress = `in`.readByte().toInt() != 0
        isOriginalControl = `in`.readByte().toInt() != 0
        isCamera = `in`.readByte().toInt() != 0
        isGif = `in`.readByte().toInt() != 0
        enablePreview = `in`.readByte().toInt() != 0
        enPreviewVideo = `in`.readByte().toInt() != 0
        enablePreviewAudio = `in`.readByte().toInt() != 0
        checkNumMode = `in`.readByte().toInt() != 0
        openClickSound = `in`.readByte().toInt() != 0
        enableCrop = `in`.readByte().toInt() != 0
        freeStyleCropEnabled = `in`.readByte().toInt() != 0
        circleDimmedLayer = `in`.readByte().toInt() != 0
        circleDimmedColor = `in`.readInt()
        circleDimmedBorderColor = `in`.readInt()
        circleStrokeWidth = `in`.readInt()
        showCropFrame = `in`.readByte().toInt() != 0
        showCropGrid = `in`.readByte().toInt() != 0
        hideBottomControls = `in`.readByte().toInt() != 0
        rotateEnabled = `in`.readByte().toInt() != 0
        scaleEnabled = `in`.readByte().toInt() != 0
        previewEggs = `in`.readByte().toInt() != 0
        synOrAsy = `in`.readByte().toInt() != 0
        returnEmpty = `in`.readByte().toInt() != 0
        isDragFrame = `in`.readByte().toInt() != 0
        isNotPreviewDownload = `in`.readByte().toInt() != 0
        isWithVideoImage = `in`.readByte().toInt() != 0
        uCropOptions = `in`.readParcelable(com.luck.picture.lib.config.UCropOptions::class.java.getClassLoader())
        selectionMedias = `in`.createTypedArrayList<LocalMedia>(LocalMedia.CREATOR)
        cameraFileName = `in`.readString()
        isCheckOriginalImage = `in`.readByte().toInt() != 0
        overrideWidth = `in`.readInt()
        overrideHeight = `in`.readInt()
        sizeMultiplier = `in`.readFloat()
        isChangeStatusBarFontColor = `in`.readByte().toInt() != 0
        isOpenStyleNumComplete = `in`.readByte().toInt() != 0
        isOpenStyleCheckNumMode = `in`.readByte().toInt() != 0
        titleBarBackgroundColor = `in`.readInt()
        pictureStatusBarColor = `in`.readInt()
        cropTitleBarBackgroundColor = `in`.readInt()
        cropStatusBarColorPrimaryDark = `in`.readInt()
        cropTitleColor = `in`.readInt()
        upResId = `in`.readInt()
        downResId = `in`.readInt()
        outPutCameraPath = `in`.readString()
        originalPath = `in`.readString()
        cameraPath = `in`.readString()
        cameraMimeType = `in`.readInt()
        pageSize = `in`.readInt()
        isPageStrategy = `in`.readByte().toInt() != 0
        isFilterInvalidFile = `in`.readByte().toInt() != 0
        isMaxSelectEnabledMask = `in`.readByte().toInt() != 0
        animationMode = `in`.readInt()
        isAutomaticTitleRecyclerTop = `in`.readByte().toInt() != 0
        isCallbackMode = `in`.readByte().toInt() != 0
        isAndroidQChangeWH = `in`.readByte().toInt() != 0
        isAndroidQChangeVideoWH = `in`.readByte().toInt() != 0
        isQuickCapture = `in`.readByte().toInt() != 0
        isFallbackVersion = `in`.readByte().toInt() != 0
        isFallbackVersion2 = `in`.readByte().toInt() != 0
        isFallbackVersion3 = `in`.readByte().toInt() != 0
    }

    companion object {
        var imageEngine: ImageEngine? = null
        var cacheResourcesEngine: CacheResourcesEngine? = null
        var listener: OnResultCallbackListener? = null
        var customVideoPlayCallback: OnVideoSelectedPlayCallback? = null
        var onCustomCameraInterfaceListener: OnCustomCameraInterfaceListener? = null
        val cleanInstance: PictureSelectionConfig
            get() {
                val selectionSpec: PictureSelectionConfig = Companion.instance
                selectionSpec.initDefaultValue()
                return selectionSpec
            }

        /**
         * 释放监听器
         */
        fun destroy() {
            listener = null
            customVideoPlayCallback = null
            onCustomCameraInterfaceListener = null
            onCustomCameraInterfaceListener = null
            cacheResourcesEngine = null
        }

        val CREATOR: Parcelable.Creator<PictureSelectionConfig> = object : Parcelable.Creator<PictureSelectionConfig?> {
            override fun createFromParcel(source: Parcel): PictureSelectionConfig {
                return PictureSelectionConfig(source)
            }

            override fun newArray(size: Int): Array<PictureSelectionConfig> {
                return arrayOfNulls(size)
            }
        }
    }
}