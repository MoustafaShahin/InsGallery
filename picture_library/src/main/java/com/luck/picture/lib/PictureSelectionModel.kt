package com.luck.picture.lib

import androidx.annotation.IntRange
import androidx.fragment.app.Fragment
import com.luck.picture.lib.animators.AnimationType
import java.lang.ref.WeakReference

/**
 * @author：luck
 * @date：2017-5-24 21:30
 * @describe：PictureSelectionModel
 */
class PictureSelectionModel {
    private var selectionConfig: PictureSelectionConfig?
    private var selector: PictureSelector?

    constructor(selector: PictureSelector?, chooseMode: Int) {
        this.selector = selector
        selectionConfig = PictureSelectionConfig.getCleanInstance()
        selectionConfig.chooseMode = chooseMode
    }

    constructor(selector: PictureSelector?, chooseMode: Int, camera: Boolean) {
        this.selector = selector
        selectionConfig = PictureSelectionConfig.getCleanInstance()
        selectionConfig.camera = camera
        selectionConfig.chooseMode = chooseMode
    }

    /**
     * @param themeStyleId PictureSelector Theme style
     * @return PictureSelectionModel
     * Use [R.style.picture_default_style]
     */
    fun theme(@StyleRes themeStyleId: Int): PictureSelectionModel {
        selectionConfig.themeStyleId = themeStyleId
        return this
    }

    /**
     * @param locale Language
     * @return PictureSelectionModel
     */
    fun setLanguage(language: Int): PictureSelectionModel {
        selectionConfig.language = language
        return this
    }

    /**
     * Change the desired orientation of this activity.  If the activity
     * is currently in the foreground or otherwise impacting the screen
     * orientation, the screen will immediately be changed (possibly causing
     * the activity to be restarted). Otherwise, this will be used the next
     * time the activity is visible.
     *
     * @param requestedOrientation An orientation constant as used in
     * [ActivityInfo.screenOrientation].
     */
    fun setRequestedOrientation(requestedOrientation: Int): PictureSelectionModel {
        selectionConfig.requestedOrientation = requestedOrientation
        return this
    }

    /**
     * @param engine Image Load the engine
     * @return Use [].
     */
    @Deprecated("")
    fun loadImageEngine(engine: ImageEngine): PictureSelectionModel {
        if (PictureSelectionConfig.imageEngine !== engine) {
            PictureSelectionConfig.imageEngine = engine
        }
        return this
    }

    /**
     * @param engine Image Load the engine
     * @return
     */
    fun imageEngine(engine: ImageEngine): PictureSelectionModel {
        if (PictureSelectionConfig.imageEngine !== engine) {
            PictureSelectionConfig.imageEngine = engine
        }
        return this
    }

    /**
     * Only for Android version Q
     *
     * @param cacheResourcesEngine Image Cache
     * @return
     */
    @Deprecated("")
    fun loadCacheResourcesCallback(cacheResourcesEngine: CacheResourcesEngine): PictureSelectionModel {
        if (SdkVersionUtils.checkedAndroid_Q()) {
            if (PictureSelectionConfig.cacheResourcesEngine !== cacheResourcesEngine) {
                PictureSelectionConfig.cacheResourcesEngine = WeakReference<Any>(cacheResourcesEngine).get()
            }
        }
        return this
    }

    /**
     * @param selectionMode PictureSelector Selection model and PictureConfig.MULTIPLE or PictureConfig.SINGLE
     * @return
     */
    fun selectionMode(selectionMode: Int): PictureSelectionModel {
        selectionConfig.selectionMode = selectionMode
        return this
    }

    /**
     * @param isWeChatStyle Select style with or without WeChat enabled
     * @return
     */
    fun isWeChatStyle(isWeChatStyle: Boolean): PictureSelectionModel {
        selectionConfig.isWeChatStyle = isWeChatStyle
        return this
    }

    /**
     * 设置 Instagram 风格的配置参数
     * @param instagramConfig Instagram 参数
     * @return
     */
    fun setInstagramConfig(instagramConfig: InstagramSelectionConfig): PictureSelectionModel {
        selectionConfig.instagramSelectionConfig = instagramConfig
        return this
    }

    /**
     * @param isUseCustomCamera Whether to use a custom camera
     * @return
     */
    fun isUseCustomCamera(isUseCustomCamera: Boolean): PictureSelectionModel {
        selectionConfig.isUseCustomCamera = Build.VERSION.SDK_INT > VERSION_CODES.KITKAT && isUseCustomCamera
        return this
    }

    /**
     * @param callback Provide video playback control，Users are free to customize the video display interface
     * @return
     */
    fun bindCustomPlayVideoCallback(callback: OnVideoSelectedPlayCallback): PictureSelectionModel {
        PictureSelectionConfig.customVideoPlayCallback = WeakReference<Any>(callback).get()
        return this
    }

    /**
     * # The developer provides an additional callback interface to the user where the user can perform some custom actions
     * {link 如果是自定义相机则必须使用.startActivityForResult(this,PictureConfig.REQUEST_CAMERA);方式启动否则PictureSelector处理不了相机后的回调}
     *
     * @param listener
     * @return Use ${bindCustomCameraInterfaceListener}
     */
    @Deprecated("")
    fun bindPictureSelectorInterfaceListener(listener: OnCustomCameraInterfaceListener): PictureSelectionModel {
        PictureSelectionConfig.onCustomCameraInterfaceListener = WeakReference<Any>(listener).get()
        return this
    }

    /**
     * # The developer provides an additional callback interface to the user where the user can perform some custom actions
     * {link 如果是自定义相机则必须使用.startActivityForResult(this,PictureConfig.REQUEST_CAMERA);方式启动否则PictureSelector处理不了相机后的回调}
     *
     * @param listener
     * @return
     */
    fun bindCustomCameraInterfaceListener(listener: OnCustomCameraInterfaceListener): PictureSelectionModel {
        PictureSelectionConfig.onCustomCameraInterfaceListener = WeakReference<Any>(listener).get()
        return this
    }

    /**
     * @param buttonFeatures Set the record button function
     * # 具体参考 CustomCameraView.BUTTON_STATE_BOTH、BUTTON_STATE_ONLY_CAPTURE、BUTTON_STATE_ONLY_RECORDER
     * @return
     */
    fun setButtonFeatures(buttonFeatures: Int): PictureSelectionModel {
        selectionConfig.buttonFeatures = buttonFeatures
        return this
    }

    /**
     * @param enableCrop Do you want to start cutting ?
     * @return Use {link .isEnableCrop()}
     */
    @Deprecated("")
    fun enableCrop(enableCrop: Boolean): PictureSelectionModel {
        selectionConfig.enableCrop = enableCrop
        return this
    }

    /**
     * @param enableCrop Do you want to start cutting ?
     * @return
     */
    fun isEnableCrop(enableCrop: Boolean): PictureSelectionModel {
        selectionConfig.enableCrop = enableCrop
        return this
    }

    /**
     * @param uCropOptions UCrop parameter configuration is provided
     * @return
     */
    fun basicUCropConfig(uCropOptions: UCropOptions): PictureSelectionModel {
        selectionConfig.uCropOptions = uCropOptions
        return this
    }

    /**
     * @param isMultipleSkipCrop Whether multiple images can be skipped when cropping
     * @return
     */
    fun isMultipleSkipCrop(isMultipleSkipCrop: Boolean): PictureSelectionModel {
        selectionConfig.isMultipleSkipCrop = isMultipleSkipCrop
        return this
    }

    /**
     * @param enablePreviewAudio Do you want to ic_play audio ?
     * @return
     */
    @Deprecated("")
    fun enablePreviewAudio(enablePreviewAudio: Boolean): PictureSelectionModel {
        selectionConfig.enablePreviewAudio = enablePreviewAudio
        return this
    }

    /**
     * @param enablePreviewAudio Do you want to ic_play audio ?
     * @return
     */
    @Deprecated("")
    fun isEnablePreviewAudio(enablePreviewAudio: Boolean): PictureSelectionModel {
        selectionConfig.enablePreviewAudio = enablePreviewAudio
        return this
    }

    /**
     * @param freeStyleCropEnabled Crop frame is move ?
     * @return
     */
    fun freeStyleCropEnabled(freeStyleCropEnabled: Boolean): PictureSelectionModel {
        selectionConfig.freeStyleCropEnabled = freeStyleCropEnabled
        return this
    }

    /**
     * @param scaleEnabled Crop frame is zoom ?
     * @return
     */
    fun scaleEnabled(scaleEnabled: Boolean): PictureSelectionModel {
        selectionConfig.scaleEnabled = scaleEnabled
        return this
    }

    /**
     * @param rotateEnabled Crop frame is rotate ?
     * @return
     */
    fun rotateEnabled(rotateEnabled: Boolean): PictureSelectionModel {
        selectionConfig.rotateEnabled = rotateEnabled
        return this
    }

    /**
     * @param circleDimmedLayer Circular head cutting
     * @return
     */
    fun circleDimmedLayer(circleDimmedLayer: Boolean): PictureSelectionModel {
        selectionConfig.circleDimmedLayer = circleDimmedLayer
        return this
    }

    /**
     * @param circleDimmedColor setCircleDimmedColor
     * @return
     */
    @Deprecated("")
    fun setCircleDimmedColor(circleDimmedColor: Int): PictureSelectionModel {
        selectionConfig.circleDimmedColor = circleDimmedColor
        return this
    }

    /**
     * @param dimmedColor
     * @return
     */
    fun setCropDimmedColor(dimmedColor: Int): PictureSelectionModel {
        selectionConfig.circleDimmedColor = dimmedColor
        return this
    }

    /**
     * @param circleDimmedBorderColor setCircleDimmedBorderColor
     * @return
     */
    fun setCircleDimmedBorderColor(circleDimmedBorderColor: Int): PictureSelectionModel {
        selectionConfig.circleDimmedBorderColor = circleDimmedBorderColor
        return this
    }

    /**
     * @param circleStrokeWidth setCircleStrokeWidth
     * @return
     */
    fun setCircleStrokeWidth(circleStrokeWidth: Int): PictureSelectionModel {
        selectionConfig.circleStrokeWidth = circleStrokeWidth
        return this
    }

    /**
     * @param showCropFrame Whether to show crop frame
     * @return
     */
    fun showCropFrame(showCropFrame: Boolean): PictureSelectionModel {
        selectionConfig.showCropFrame = showCropFrame
        return this
    }

    /**
     * @param showCropGrid Whether to show CropGrid
     * @return
     */
    fun showCropGrid(showCropGrid: Boolean): PictureSelectionModel {
        selectionConfig.showCropGrid = showCropGrid
        return this
    }

    /**
     * @param hideBottomControls Whether is Clipping function bar
     * 单选有效
     * @return
     */
    fun hideBottomControls(hideBottomControls: Boolean): PictureSelectionModel {
        selectionConfig.hideBottomControls = hideBottomControls
        return this
    }

    /**
     * @param aspect_ratio_x Crop Proportion x
     * @param aspect_ratio_y Crop Proportion y
     * @return
     */
    fun withAspectRatio(aspect_ratio_x: Int, aspect_ratio_y: Int): PictureSelectionModel {
        selectionConfig.aspect_ratio_x = aspect_ratio_x
        selectionConfig.aspect_ratio_y = aspect_ratio_y
        return this
    }

    /**
     * @param isWithVideoImage Whether the pictures and videos can be selected together
     * @return
     */
    fun isWithVideoImage(isWithVideoImage: Boolean): PictureSelectionModel {
        selectionConfig.isWithVideoImage = selectionConfig.selectionMode !== PictureConfig.SINGLE && selectionConfig.chooseMode === PictureMimeType.ofAll() && isWithVideoImage
        return this
    }

    /**
     * When the maximum number of choices is reached, does the list enable the mask effect
     *
     * @param isMaxSelectEnabledMask
     * @return
     */
    fun isMaxSelectEnabledMask(isMaxSelectEnabledMask: Boolean): PictureSelectionModel {
        selectionConfig.isMaxSelectEnabledMask = isMaxSelectEnabledMask
        return this
    }

    /**
     * @param maxSelectNum PictureSelector max selection
     * @return
     */
    fun maxSelectNum(maxSelectNum: Int): PictureSelectionModel {
        selectionConfig.maxSelectNum = maxSelectNum
        return this
    }

    /**
     * @param minSelectNum PictureSelector min selection
     * @return
     */
    fun minSelectNum(minSelectNum: Int): PictureSelectionModel {
        selectionConfig.minSelectNum = minSelectNum
        return this
    }

    /**
     * @param maxVideoSelectNum PictureSelector video max selection
     * @return
     */
    fun maxVideoSelectNum(maxVideoSelectNum: Int): PictureSelectionModel {
        selectionConfig.maxVideoSelectNum = maxVideoSelectNum
        return this
    }

    /**
     * @param minVideoSelectNum PictureSelector video min selection
     * @return
     */
    fun minVideoSelectNum(minVideoSelectNum: Int): PictureSelectionModel {
        selectionConfig.minVideoSelectNum = minVideoSelectNum
        return this
    }

    /**
     * Turn off Android Q to solve the problem that the width and height are reversed
     *
     * @param isChangeWH
     * @return
     */
    fun closeAndroidQChangeWH(isChangeWH: Boolean): PictureSelectionModel {
        selectionConfig.isAndroidQChangeWH = isChangeWH
        return this
    }

    /**
     * Turn off Android Q to solve the problem that the width and height are reversed
     *
     * @param isChangeVideoWH
     * @return
     */
    fun closeAndroidQChangeVideoWH(isChangeVideoWH: Boolean): PictureSelectionModel {
        selectionConfig.isAndroidQChangeVideoWH = isChangeVideoWH
        return this
    }

    /**
     * By clicking the title bar consecutively, RecyclerView automatically rolls back to the top
     *
     * @param isAutomaticTitleRecyclerTop
     * @return
     */
    fun isAutomaticTitleRecyclerTop(isAutomaticTitleRecyclerTop: Boolean): PictureSelectionModel {
        selectionConfig.isAutomaticTitleRecyclerTop = isAutomaticTitleRecyclerTop
        return this
    }

    /**
     * @param Select whether to return directly
     * @return
     */
    fun isSingleDirectReturn(isSingleDirectReturn: Boolean): PictureSelectionModel {
        selectionConfig.isSingleDirectReturn = selectionConfig.selectionMode
        === PictureConfig.SINGLE && isSingleDirectReturn
        selectionConfig.isOriginalControl = (selectionConfig.selectionMode !== PictureConfig.SINGLE || !isSingleDirectReturn) && selectionConfig.isOriginalControl
        return this
    }

    /**
     * Whether to turn on paging mode
     *
     * @param isPageStrategy
     * @param pageSize       Maximum number of pages [is preferably no less than 20][PageSize]
     * @return
     */
    fun isPageStrategy(isPageStrategy: Boolean, pageSize: Int): PictureSelectionModel {
        selectionConfig.isPageStrategy = isPageStrategy
        selectionConfig.pageSize = if (pageSize < PictureConfig.MIN_PAGE_SIZE) PictureConfig.MAX_PAGE_SIZE else pageSize
        return this
    }

    /**
     * Whether to turn on paging mode
     *
     * @param isPageStrategy
     * @param pageSize            Maximum number of pages [is preferably no less than 20][PageSize]
     * @param isFilterInvalidFile Whether to filter invalid files [of the query performance is consumed,Especially on the Q version][Some]
     * @return
     */
    fun isPageStrategy(isPageStrategy: Boolean, pageSize: Int, isFilterInvalidFile: Boolean): PictureSelectionModel {
        selectionConfig.isPageStrategy = isPageStrategy
        selectionConfig.pageSize = if (pageSize < PictureConfig.MIN_PAGE_SIZE) PictureConfig.MAX_PAGE_SIZE else pageSize
        selectionConfig.isFilterInvalidFile = isFilterInvalidFile
        return this
    }

    /**
     * Whether to turn on paging mode
     *
     * @param isPageStrategy
     * @return
     */
    fun isPageStrategy(isPageStrategy: Boolean): PictureSelectionModel {
        selectionConfig.isPageStrategy = isPageStrategy
        return this
    }

    /**
     * Whether to turn on paging mode
     *
     * @param isPageStrategy
     * @param isFilterInvalidFile Whether to filter invalid files [of the query performance is consumed,Especially on the Q version][Some]
     * @return
     */
    fun isPageStrategy(isPageStrategy: Boolean, isFilterInvalidFile: Boolean): PictureSelectionModel {
        selectionConfig.isPageStrategy = isPageStrategy
        selectionConfig.isFilterInvalidFile = isFilterInvalidFile
        return this
    }

    /**
     * @param videoQuality video quality and 0 or 1
     * @return
     */
    fun videoQuality(videoQuality: Int): PictureSelectionModel {
        selectionConfig.videoQuality = videoQuality
        return this
    }

    /**
     * # alternative api cameraFileName(xxx.PNG);
     *
     * @param suffixType PictureSelector media format
     * @return
     */
    fun imageFormat(suffixType: String): PictureSelectionModel {
        selectionConfig.suffixType = suffixType
        return this
    }

    /**
     * @param cropWidth  crop width
     * @param cropHeight crop height
     * @return this
     */
    @Deprecated("""Crop image output width and height
      {@link cropImageWideHigh()}""")
    fun cropWH(cropWidth: Int, cropHeight: Int): PictureSelectionModel {
        selectionConfig.cropWidth = cropWidth
        selectionConfig.cropHeight = cropHeight
        return this
    }

    /**
     * @param cropWidth  crop width
     * @param cropHeight crop height
     * @return this
     */
    fun cropImageWideHigh(cropWidth: Int, cropHeight: Int): PictureSelectionModel {
        selectionConfig.cropWidth = cropWidth
        selectionConfig.cropHeight = cropHeight
        return this
    }

    /**
     * @param videoMaxSecond selection video max second
     * @return
     */
    fun videoMaxSecond(videoMaxSecond: Int): PictureSelectionModel {
        selectionConfig.videoMaxSecond = videoMaxSecond * 1000
        return this
    }

    /**
     * @param videoMinSecond selection video min second
     * @return
     */
    fun videoMinSecond(videoMinSecond: Int): PictureSelectionModel {
        selectionConfig.videoMinSecond = videoMinSecond * 1000
        return this
    }

    /**
     * @param recordVideoSecond video record second
     * @return
     */
    fun recordVideoSecond(recordVideoSecond: Int): PictureSelectionModel {
        selectionConfig.recordVideoSecond = recordVideoSecond
        return this
    }

    fun recordVideoMinSecond(recordVideoMinSecond: Int): PictureSelectionModel {
        selectionConfig.recordVideoMinSecond = recordVideoMinSecond
        return this
    }

    /**
     * @param width  glide width
     * @param height glide height
     * @return 2.2.9开始 Glide改为外部用户自己定义此方法没有意义了
     */
    @Deprecated("")
    fun glideOverride(@IntRange(from = 100) width: Int,
                      @IntRange(from = 100) height: Int): PictureSelectionModel {
        selectionConfig.overrideWidth = width
        selectionConfig.overrideHeight = height
        return this
    }

    /**
     * @param sizeMultiplier The multiplier to apply to the
     * [com.bumptech.glide.request.target.Target]'s dimensions when
     * loading the resource.
     * @return 2.2.9开始Glide改为外部用户自己定义此方法没有意义了
     */
    @Deprecated("")
    fun sizeMultiplier(@FloatRange(from = 0.1f) sizeMultiplier: Float): PictureSelectionModel {
        selectionConfig.sizeMultiplier = sizeMultiplier
        return this
    }

    /**
     * @param imageSpanCount PictureSelector image span count
     * @return
     */
    fun imageSpanCount(imageSpanCount: Int): PictureSelectionModel {
        selectionConfig.imageSpanCount = imageSpanCount
        return this
    }

    /**
     * @param Less than how many KB images are not compressed
     * @return
     */
    fun minimumCompressSize(size: Int): PictureSelectionModel {
        selectionConfig.minimumCompressSize = size
        return this
    }

    /**
     * @param compressQuality crop compress quality default 90
     * @return 请使用 cutOutQuality();方法
     */
    @Deprecated("")
    fun cropCompressQuality(compressQuality: Int): PictureSelectionModel {
        selectionConfig.cropCompressQuality = compressQuality
        return this
    }

    /**
     * @param cutQuality crop compress quality default 90
     * @return
     */
    fun cutOutQuality(cutQuality: Int): PictureSelectionModel {
        selectionConfig.cropCompressQuality = cutQuality
        return this
    }

    /**
     * @param isCompress Whether to open compress
     * @return Use {link .isCompress()}
     */
    @Deprecated("")
    fun compress(isCompress: Boolean): PictureSelectionModel {
        selectionConfig.isCompress = isCompress
        return this
    }

    /**
     * @param isCompress Whether to open compress
     * @return
     */
    fun isCompress(isCompress: Boolean): PictureSelectionModel {
        selectionConfig.isCompress = isCompress
        return this
    }

    /**
     * @param compressQuality Image compressed output quality
     * @return
     */
    fun compressQuality(compressQuality: Int): PictureSelectionModel {
        selectionConfig.compressQuality = compressQuality
        return this
    }

    /**
     * @param returnEmpty No data can be returned
     * @return
     */
    fun isReturnEmpty(returnEmpty: Boolean): PictureSelectionModel {
        selectionConfig.returnEmpty = returnEmpty
        return this
    }

    /**
     * @param synOrAsy Synchronous or asynchronous compression
     * @return
     */
    fun synOrAsy(synOrAsy: Boolean): PictureSelectionModel {
        selectionConfig.synOrAsy = synOrAsy
        return this
    }

    /**
     * @param focusAlpha After compression, the transparent channel is retained
     * @return
     */
    fun compressFocusAlpha(focusAlpha: Boolean): PictureSelectionModel {
        selectionConfig.focusAlpha = focusAlpha
        return this
    }

    /**
     * After recording with the system camera, does it support playing the video immediately using the system player
     *
     * @param isQuickCapture
     * @return
     */
    fun isQuickCapture(isQuickCapture: Boolean): PictureSelectionModel {
        selectionConfig.isQuickCapture = isQuickCapture
        return this
    }

    /**
     * @param isOriginalControl Whether the original image is displayed
     * @return
     */
    fun isOriginalImageControl(isOriginalControl: Boolean): PictureSelectionModel {
        selectionConfig.isOriginalControl = (!selectionConfig.camera
                && selectionConfig.chooseMode !== PictureMimeType.ofVideo() && selectionConfig.chooseMode !== PictureMimeType.ofAudio() && isOriginalControl)
        return this
    }

    /**
     * @param path save path
     * @return
     */
    fun compressSavePath(path: String): PictureSelectionModel {
        selectionConfig.compressSavePath = path
        return this
    }

    /**
     * Camera custom local file name
     * # Such as xxx.png
     *
     * @param fileName
     * @return
     */
    fun cameraFileName(fileName: String): PictureSelectionModel {
        selectionConfig.cameraFileName = fileName
        return this
    }

    /**
     * crop custom local file name
     * # Such as xxx.png
     *
     * @param renameCropFileName
     * @return
     */
    fun renameCropFileName(renameCropFileName: String): PictureSelectionModel {
        selectionConfig.renameCropFileName = renameCropFileName
        return this
    }

    /**
     * custom compress local file name
     * # Such as xxx.png
     *
     * @param renameFile
     * @return
     */
    fun renameCompressFile(renameFile: String): PictureSelectionModel {
        selectionConfig.renameCompressFileName = renameFile
        return this
    }

    /**
     * @param zoomAnim Picture list zoom anim
     * @return
     */
    fun isZoomAnim(zoomAnim: Boolean): PictureSelectionModel {
        selectionConfig.zoomAnim = zoomAnim
        return this
    }

    /**
     * @param previewEggs preview eggs  It doesn't make much sense
     * @return Use {link .isPreviewEggs()}
     */
    @Deprecated("")
    fun previewEggs(previewEggs: Boolean): PictureSelectionModel {
        selectionConfig.previewEggs = previewEggs
        return this
    }

    /**
     * @param previewEggs preview eggs  It doesn't make much sense
     * @return
     */
    fun isPreviewEggs(previewEggs: Boolean): PictureSelectionModel {
        selectionConfig.previewEggs = previewEggs
        return this
    }

    /**
     * @param isCamera Whether to open camera button
     * @return
     */
    fun isCamera(isCamera: Boolean): PictureSelectionModel {
        selectionConfig.isCamera = isCamera
        return this
    }

    /**
     * Extra used with [+  File.separator + &quot;CustomCamera&quot; + File.separator][.Environment.getExternalStorageDirectory]  to indicate that
     *
     * @param outPutCameraPath Camera save path 只支持Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
     * @return
     */
    fun setOutputCameraPath(outPutCameraPath: String): PictureSelectionModel {
        selectionConfig.outPutCameraPath = outPutCameraPath
        return this
    }

    /**
     * # file size The unit is M
     *
     * @param fileSize Filter file size
     * @return
     */
    fun queryMaxFileSize(fileSize: Float): PictureSelectionModel {
        selectionConfig.filterFileSize = fileSize
        return this
    }

    /**
     * @param isGif Whether to open gif
     * @return
     */
    fun isGif(isGif: Boolean): PictureSelectionModel {
        selectionConfig.isGif = isGif
        return this
    }

    /**
     * @param enablePreview Do you want to preview the picture?
     * @return Use {link .isPreviewImage()}
     */
    @Deprecated("")
    fun previewImage(enablePreview: Boolean): PictureSelectionModel {
        selectionConfig.enablePreview = enablePreview
        return this
    }

    /**
     * @param enablePreview Do you want to preview the picture?
     * @return
     */
    fun isPreviewImage(enablePreview: Boolean): PictureSelectionModel {
        selectionConfig.enablePreview = enablePreview
        return this
    }

    /**
     * @param enPreviewVideo Do you want to preview the video?
     * @return Use {link .isPreviewVideo()}
     */
    @Deprecated("")
    fun previewVideo(enPreviewVideo: Boolean): PictureSelectionModel {
        selectionConfig.enPreviewVideo = enPreviewVideo
        return this
    }

    /**
     * @param enPreviewVideo Do you want to preview the video?
     * @return
     */
    fun isPreviewVideo(enPreviewVideo: Boolean): PictureSelectionModel {
        selectionConfig.enPreviewVideo = enPreviewVideo
        return this
    }

    /**
     * @param isNotPreviewDownload Previews do not show downloads
     * @return
     */
    fun isNotPreviewDownload(isNotPreviewDownload: Boolean): PictureSelectionModel {
        selectionConfig.isNotPreviewDownload = isNotPreviewDownload
        return this
    }

    /**
     * @param Specify get image format
     * @return
     */
    fun querySpecifiedFormatSuffix(specifiedFormat: String): PictureSelectionModel {
        selectionConfig.specifiedFormat = specifiedFormat
        return this
    }

    /**
     * @param openClickSound Whether to open click voice
     * @return Use {link .isOpenClickSound()}
     */
    @Deprecated("")
    fun openClickSound(openClickSound: Boolean): PictureSelectionModel {
        selectionConfig.openClickSound = !selectionConfig.camera && openClickSound
        return this
    }

    /**
     * @param isOpenClickSound Whether to open click voice
     * @return
     */
    fun isOpenClickSound(openClickSound: Boolean): PictureSelectionModel {
        selectionConfig.openClickSound = !selectionConfig.camera && openClickSound
        return this
    }

    /**
     * 是否可拖动裁剪框(setFreeStyleCropEnabled 为true 有效)
     */
    fun isDragFrame(isDragFrame: Boolean): PictureSelectionModel {
        selectionConfig.isDragFrame = isDragFrame
        return this
    }

    /**
     * Whether the multi-graph clipping list is animated or not
     *
     * @param isAnimation
     * @return
     */
    fun isMultipleRecyclerAnimation(isAnimation: Boolean): PictureSelectionModel {
        selectionConfig.isMultipleRecyclerAnimation = isAnimation
        return this
    }

    /**
     * 设置摄像头方向(前后 默认后置)
     */
    fun isCameraAroundState(isCameraAroundState: Boolean): PictureSelectionModel {
        selectionConfig.isCameraAroundState = isCameraAroundState
        return this
    }

    /**
     * @param selectionMedia Select the selected picture set
     * @return Use {link .selectionData()}
     */
    @Deprecated("")
    fun selectionMedia(selectionMedia: List<LocalMedia?>): PictureSelectionModel {
        if (selectionConfig.selectionMode === PictureConfig.SINGLE && selectionConfig.isSingleDirectReturn) {
            selectionConfig.selectionMedias = null
        } else {
            selectionConfig.selectionMedias = selectionMedia
        }
        return this
    }

    /**
     * @param selectionData Select the selected picture set
     * @return
     */
    fun selectionData(selectionData: List<LocalMedia?>): PictureSelectionModel {
        if (selectionConfig.selectionMode === PictureConfig.SINGLE && selectionConfig.isSingleDirectReturn) {
            selectionConfig.selectionMedias = null
        } else {
            selectionConfig.selectionMedias = selectionData
        }
        return this
    }

    /**
     * 是否改变状态栏字段颜色(黑白字体转换)
     * #适合所有style使用
     *
     * @param isChangeStatusBarFontColor
     * @return
     */
    @Deprecated("")
    fun isChangeStatusBarFontColor(isChangeStatusBarFontColor: Boolean): PictureSelectionModel {
        selectionConfig.isChangeStatusBarFontColor = isChangeStatusBarFontColor
        return this
    }

    /**
     * 选择图片样式0/9
     * #适合所有style使用
     *
     * @param isOpenStyleNumComplete
     * @return 使用setPictureStyle方法
     */
    @Deprecated("")
    fun isOpenStyleNumComplete(isOpenStyleNumComplete: Boolean): PictureSelectionModel {
        selectionConfig.isOpenStyleNumComplete = isOpenStyleNumComplete
        return this
    }

    /**
     * 是否开启数字选择模式
     * #适合qq style 样式使用
     *
     * @param isOpenStyleCheckNumMode
     * @return 使用setPictureStyle方法
     */
    @Deprecated("")
    fun isOpenStyleCheckNumMode(isOpenStyleCheckNumMode: Boolean): PictureSelectionModel {
        selectionConfig.isOpenStyleCheckNumMode = isOpenStyleCheckNumMode
        return this
    }

    /**
     * 设置标题栏背景色
     *
     * @param color
     * @return 使用setPictureStyle方法
     */
    @Deprecated("")
    fun setTitleBarBackgroundColor(@ColorInt color: Int): PictureSelectionModel {
        selectionConfig.titleBarBackgroundColor = color
        return this
    }

    /**
     * 状态栏背景色
     *
     * @param color
     * @return 使用setPictureStyle方法
     */
    @Deprecated("")
    fun setStatusBarColorPrimaryDark(@ColorInt color: Int): PictureSelectionModel {
        selectionConfig.pictureStatusBarColor = color
        return this
    }

    /**
     * 裁剪页面标题背景色
     *
     * @param color
     * @return 使用setPictureCropStyle方法
     */
    @Deprecated("")
    fun setCropTitleBarBackgroundColor(@ColorInt color: Int): PictureSelectionModel {
        selectionConfig.cropTitleBarBackgroundColor = color
        return this
    }

    /**
     * 裁剪页面状态栏背景色
     *
     * @param color
     * @return 使用setPictureCropStyle方法
     */
    @Deprecated("")
    fun setCropStatusBarColorPrimaryDark(@ColorInt color: Int): PictureSelectionModel {
        selectionConfig.cropStatusBarColorPrimaryDark = color
        return this
    }

    /**
     * 裁剪页面标题文字颜色
     *
     * @param color
     * @return 使用setPictureCropStyle方法
     */
    @Deprecated("")
    fun setCropTitleColor(@ColorInt color: Int): PictureSelectionModel {
        selectionConfig.cropTitleColor = color
        return this
    }

    /**
     * 设置相册标题右侧向上箭头图标
     *
     * @param resId
     * @return 使用setPictureStyle方法
     */
    @Deprecated("")
    fun setUpArrowDrawable(resId: Int): PictureSelectionModel {
        selectionConfig.upResId = resId
        return this
    }

    /**
     * 设置相册标题右侧向下箭头图标
     *
     * @param resId
     * @return 使用setPictureStyle方法
     */
    @Deprecated("")
    fun setDownArrowDrawable(resId: Int): PictureSelectionModel {
        selectionConfig.downResId = resId
        return this
    }

    /**
     * 动态设置裁剪主题样式
     *
     * @param style 裁剪页主题
     * @return
     */
    fun setPictureCropStyle(style: PictureCropParameterStyle): PictureSelectionModel {
        selectionConfig.cropStyle = style
        return this
    }

    /**
     * 动态设置相册主题样式
     *
     * @param style 主题
     * @return
     */
    fun setPictureStyle(style: PictureParameterStyle): PictureSelectionModel {
        selectionConfig.style = style
        return this
    }

    /**
     * Dynamically set the album to start and exit the animation
     *
     * @param style Activity Launch exit animation theme
     * @return
     */
    fun setPictureWindowAnimationStyle(windowAnimationStyle: PictureWindowAnimationStyle): PictureSelectionModel {
        selectionConfig.windowAnimationStyle = windowAnimationStyle
        return this
    }

    /**
     * Photo album list animation {}
     * Use [or SLIDE_IN_BOTTOM_ANIMATION][AnimationType.ALPHA_IN_ANIMATION] directly.
     *
     * @param animationMode
     * @return
     */
    fun setRecyclerAnimationMode(animationMode: Int): PictureSelectionModel {
        selectionConfig.animationMode = animationMode
        return this
    }

    /**
     * # If you want to handle the Android Q path, if not, just return the uri，
     * The getAndroidQToPath(); field will be empty
     *
     * @param isAndroidQTransform
     * @return
     */
    fun isAndroidQTransform(isAndroidQTransform: Boolean): PictureSelectionModel {
        selectionConfig.isAndroidQTransform = isAndroidQTransform
        return this
    }

    /**
     * # 内部方法-要使用此方法时最好先咨询作者！！！
     *
     * @param isFallbackVersion 仅供特殊情况内部使用 如果某功能出错此开关可以回退至之前版本
     * @return
     */
    fun isFallbackVersion(isFallbackVersion: Boolean): PictureSelectionModel {
        selectionConfig.isFallbackVersion = isFallbackVersion
        return this
    }

    /**
     * # 内部方法-要使用此方法时最好先咨询作者！！！
     *
     * @param isFallbackVersion 仅供特殊情况内部使用 如果某功能出错此开关可以回退至之前版本
     * @return
     */
    fun isFallbackVersion2(isFallbackVersion: Boolean): PictureSelectionModel {
        selectionConfig.isFallbackVersion2 = isFallbackVersion
        return this
    }

    /**
     * # 内部方法-要使用此方法时最好先咨询作者！！！
     *
     * @param isFallbackVersion 仅供特殊情况内部使用 如果某功能出错此开关可以回退至之前版本
     * @return
     */
    fun isFallbackVersion3(isFallbackVersion: Boolean): PictureSelectionModel {
        selectionConfig.isFallbackVersion3 = isFallbackVersion
        return this
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    fun forResult(requestCode: Int) {
        if (!DoubleUtils.isFastDoubleClick()) {
            val activity: Activity = selector.getActivity()
            if (activity == null || selectionConfig == null) {
                return
            }
            val intent: Intent
            if (selectionConfig.camera && selectionConfig.isUseCustomCamera) {
                intent = Intent(activity, PictureCustomCameraActivity::class.java)
            } else {
                intent = Intent(activity, if (selectionConfig.camera) PictureSelectorCameraEmptyActivity::class.java else if (selectionConfig.isWeChatStyle) PictureSelectorWeChatStyleActivity::class.java else PictureSelectorActivity::class.java)
            }
            InstagramSelectionConfig.convertIntent(selectionConfig, intent)
            selectionConfig.isCallbackMode = false
            val fragment: Fragment = selector.getFragment()
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode)
            } else {
                activity.startActivityForResult(intent, requestCode)
            }
            val windowAnimationStyle: PictureWindowAnimationStyle = selectionConfig.windowAnimationStyle
            activity.overridePendingTransition(if (windowAnimationStyle != null &&
                    windowAnimationStyle.activityEnterAnimation !== 0) windowAnimationStyle.activityEnterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
        }
    }

    /**
     * # replace for setPictureWindowAnimationStyle();
     * Start to select media and wait for result.
     *
     *
     * # Use PictureWindowAnimationStyle to achieve animation effects
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    @Deprecated("")
    fun forResult(requestCode: Int, enterAnim: Int, exitAnim: Int) {
        if (!DoubleUtils.isFastDoubleClick()) {
            val activity: Activity = selector.getActivity() ?: return
            val intent = Intent(activity, if (selectionConfig != null && selectionConfig.camera) PictureSelectorCameraEmptyActivity::class.java else if (selectionConfig.isWeChatStyle) PictureSelectorWeChatStyleActivity::class.java else PictureSelectorActivity::class.java)
            InstagramSelectionConfig.convertIntent(selectionConfig, intent)
            selectionConfig.isCallbackMode = false
            val fragment: Fragment = selector.getFragment()
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode)
            } else {
                activity.startActivityForResult(intent, requestCode)
            }
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    /**
     * Start to select media and wait for result.
     *
     * @param listener The resulting callback listens
     */
    fun forResult(listener: OnResultCallbackListener?) {
        if (!DoubleUtils.isFastDoubleClick()) {
            val activity: Activity = selector.getActivity()
            if (activity == null || selectionConfig == null) {
                return
            }
            // 绑定回调监听
            PictureSelectionConfig.listener = WeakReference<Any>(listener).get()
            selectionConfig.isCallbackMode = true
            val intent: Intent
            if (selectionConfig.camera && selectionConfig.isUseCustomCamera) {
                intent = Intent(activity, PictureCustomCameraActivity::class.java)
            } else {
                intent = Intent(activity, if (selectionConfig.camera) PictureSelectorCameraEmptyActivity::class.java else if (selectionConfig.isWeChatStyle) PictureSelectorWeChatStyleActivity::class.java else PictureSelectorActivity::class.java)
            }
            InstagramSelectionConfig.convertIntent(selectionConfig, intent)
            val fragment: Fragment = selector.getFragment()
            if (fragment != null) {
                fragment.startActivity(intent)
            } else {
                activity.startActivity(intent)
            }
            val windowAnimationStyle: PictureWindowAnimationStyle = selectionConfig.windowAnimationStyle
            activity.overridePendingTransition(if (windowAnimationStyle != null &&
                    windowAnimationStyle.activityEnterAnimation !== 0) windowAnimationStyle.activityEnterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
        }
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     * @param listener    The resulting callback listens
     */
    fun forResult(requestCode: Int, listener: OnResultCallbackListener?) {
        if (!DoubleUtils.isFastDoubleClick()) {
            val activity: Activity = selector.getActivity()
            if (activity == null || selectionConfig == null) {
                return
            }
            // 绑定回调监听
            PictureSelectionConfig.listener = WeakReference<Any>(listener).get()
            selectionConfig.isCallbackMode = true
            val intent: Intent
            if (selectionConfig.camera && selectionConfig.isUseCustomCamera) {
                intent = Intent(activity, PictureCustomCameraActivity::class.java)
            } else {
                intent = Intent(activity, if (selectionConfig.camera) PictureSelectorCameraEmptyActivity::class.java else if (selectionConfig.isWeChatStyle) PictureSelectorWeChatStyleActivity::class.java else PictureSelectorActivity::class.java)
            }
            InstagramSelectionConfig.convertIntent(selectionConfig, intent)
            val fragment: Fragment = selector.getFragment()
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode)
            } else {
                activity.startActivityForResult(intent, requestCode)
            }
            val windowAnimationStyle: PictureWindowAnimationStyle = selectionConfig.windowAnimationStyle
            activity.overridePendingTransition(if (windowAnimationStyle != null &&
                    windowAnimationStyle.activityEnterAnimation !== 0) windowAnimationStyle.activityEnterAnimation else R.anim.picture_anim_enter, R.anim.picture_anim_fade_in)
        }
    }

    /**
     * 提供外部预览图片方法
     *
     * @param position
     * @param medias
     */
    fun openExternalPreview(position: Int, medias: List<LocalMedia?>?) {
        if (selector != null) {
            selector.externalPicturePreview(position, medias,
                    if (selectionConfig.windowAnimationStyle != null &&
                            selectionConfig.windowAnimationStyle.activityPreviewEnterAnimation !== 0) selectionConfig.windowAnimationStyle.activityPreviewEnterAnimation else 0)
        } else {
            throw NullPointerException("This PictureSelector is Null")
        }
    }

    /**
     * 提供外部预览图片方法-带自定义下载保存路径
     * # 废弃 由于Android Q沙盒机制 此方法不在需要了
     *
     * @param position
     * @param medias
     */
    @Deprecated("")
    fun openExternalPreview(position: Int, directory_path: String?, medias: List<LocalMedia?>?) {
        if (selector != null) {
            selector.externalPicturePreview(position, directory_path, medias,
                    if (selectionConfig.windowAnimationStyle != null &&
                            selectionConfig.windowAnimationStyle.activityPreviewEnterAnimation !== 0) selectionConfig.windowAnimationStyle.activityPreviewEnterAnimation else 0)
        } else {
            throw NullPointerException("This PictureSelector is Null")
        }
    }

    /**
     * set preview video
     *
     * @param path
     */
    fun externalPictureVideo(path: String?) {
        if (selector != null) {
            selector.externalPictureVideo(path)
        } else {
            throw NullPointerException("This PictureSelector is Null")
        }
    }
}