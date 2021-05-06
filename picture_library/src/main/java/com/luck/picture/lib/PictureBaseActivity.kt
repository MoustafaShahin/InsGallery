package com.luck.picture.lib

import android.Manifest
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.*

/**
 * @author：luck
 * @data：2018/3/28 下午1:00
 * @describe: BaseActivity
 */
abstract class PictureBaseActivity : AppCompatActivity() {
    protected var config: PictureSelectionConfig? = null
    protected var openWhiteStatusBar = false
    protected var numComplete = false
    protected var colorPrimary = 0
    protected var colorPrimaryDark = 0
    protected var mLoadingDialog: PictureLoadingDialog? = null
    protected var selectionMedias: MutableList<LocalMedia?>? = null
    protected var mHandler: Handler? = null
    protected var container: View? = null

    /**
     * if there more
     */
    protected var isHasMore = true

    /**
     * page
     */
    protected var mPage = 1

    /**
     * is onSaveInstanceState
     */
    protected var isOnSaveInstanceState = false

    /**
     * Whether to use immersion, subclasses copy the method to determine whether to use immersion
     *
     * @return
     */
    open val isImmersive: Boolean
        get() = true

    /**
     * Whether to change the screen direction
     *
     * @return
     */
    open val isRequestedOrientation: Boolean
        get() = true

    fun immersive() {
        ImmersiveManage.immersiveAboveAPI23(this, colorPrimaryDark, colorPrimary, openWhiteStatusBar)
    }

    /**
     * get Layout Resources Id
     *
     * @return
     */
    abstract val resourceId: Int

    /**
     * init Views
     */
    protected open fun initWidgets() {}

    /**
     * init PictureSelector Style
     */
    protected fun initPictureSelectorStyle() {}

    /**
     * Set CompleteText
     */
    protected fun initCompleteText(startCount: Int) {}

    /**
     * Set CompleteText
     */
    protected fun initCompleteText(list: List<LocalMedia?>?) {}
    protected override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            config = savedInstanceState.getParcelable(PictureConfig.EXTRA_CONFIG)
        }
        if (config == null) {
            config = if (getIntent() != null) getIntent().getParcelableExtra(PictureConfig.EXTRA_CONFIG) else config
        }
        checkConfigNull()
        PictureLanguageUtils.setAppLanguage(context, config.language)
        if (!config.camera) {
            setTheme(if (config.themeStyleId === 0) R.style.picture_default_style else config.themeStyleId)
        }
        super.onCreate(if (savedInstanceState == null) Bundle() else savedInstanceState)
        newCreateEngine()
        newCreateResultCallbackListener()
        if (isRequestedOrientation) {
            setNewRequestedOrientation()
        }
        mHandler = Handler(Looper.getMainLooper())
        initConfig()
        if (isImmersive) {
            immersive()
        }
        if (config.style != null && config.style.pictureNavBarColor !== 0) {
            NavBarUtils.setNavBarColor(this, config.style.pictureNavBarColor)
        }
        val layoutResID = resourceId
        if (layoutResID != 0) {
            setContentView(layoutResID)
        }
        initWidgets()
        initPictureSelectorStyle()
        isOnSaveInstanceState = false
    }

    /**
     * Get the image loading engine again, provided that the user implements the IApp interface in the Application
     */
    private fun newCreateEngine() {
        if (PictureSelectionConfig.imageEngine == null) {
            val baseEngine: PictureSelectorEngine = PictureAppMaster.getInstance().getPictureSelectorEngine()
            if (baseEngine != null) {
                val engine: ImageEngine = baseEngine.createEngine()
                PictureSelectionConfig.imageEngine = engine
            }
        }
    }

    /**
     * Retrieve the result callback listener, provided that the user implements the IApp interface in the Application
     */
    private fun newCreateResultCallbackListener() {
        if (config.isCallbackMode) {
            if (PictureSelectionConfig.listener == null) {
                val baseEngine: PictureSelectorEngine = PictureAppMaster.getInstance().getPictureSelectorEngine()
                if (baseEngine != null) {
                    val listener: OnResultCallbackListener<LocalMedia> = baseEngine.getResultCallbackListener()
                    PictureSelectionConfig.listener = listener
                }
            }
        }
    }

    protected override fun attachBaseContext(newBase: Context) {
        if (config == null) {
            super.attachBaseContext(newBase)
        } else {
            super.attachBaseContext(com.luck.picture.lib.PictureContextWrapper.Companion.wrap(newBase, config.language))
        }
    }

    /**
     * CheckConfigNull
     */
    private fun checkConfigNull() {
        if (config == null) {
            config = PictureSelectionConfig.getInstance()
        }
    }

    /**
     * setNewRequestedOrientation
     */
    protected fun setNewRequestedOrientation() {
        if (config != null && !config.camera) {
            setRequestedOrientation(config.requestedOrientation)
        }
    }

    /**
     * get Context
     *
     * @return this
     */
    protected val context: Context
        protected get() = this

    /**
     * init Config
     */
    private fun initConfig() {
        selectionMedias = if (config.selectionMedias == null) ArrayList<LocalMedia?>() else config.selectionMedias
        if (config.style != null) {
            openWhiteStatusBar = config.style.isChangeStatusBarFontColor
            if (config.style.pictureTitleBarBackgroundColor !== 0) {
                colorPrimary = config.style.pictureTitleBarBackgroundColor
            }
            if (config.style.pictureStatusBarColor !== 0) {
                colorPrimaryDark = config.style.pictureStatusBarColor
            }
            numComplete = config.style.isOpenCompletedNumStyle
            config.checkNumMode = config.style.isOpenCheckNumStyle
        } else {
            openWhiteStatusBar = config.isChangeStatusBarFontColor
            if (!openWhiteStatusBar) {
                openWhiteStatusBar = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_statusFontColor)
            }
            numComplete = config.isOpenStyleNumComplete
            if (!numComplete) {
                numComplete = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_numComplete)
            }
            config.checkNumMode = config.isOpenStyleCheckNumMode
            if (!config.checkNumMode) {
                config.checkNumMode = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_checkNumMode)
            }
            colorPrimary = if (config.titleBarBackgroundColor !== 0) {
                config.titleBarBackgroundColor
            } else {
                AttrsUtils.getTypeValueColor(this, R.attr.colorPrimary)
            }
            colorPrimaryDark = if (config.pictureStatusBarColor !== 0) {
                config.pictureStatusBarColor
            } else {
                AttrsUtils.getTypeValueColor(this, R.attr.colorPrimaryDark)
            }
        }
        if (config.openClickSound) {
            VoiceUtils.getInstance().init(context)
        }
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        isOnSaveInstanceState = true
        outState.putParcelable(PictureConfig.EXTRA_CONFIG, config)
    }

    /**
     * loading dialog
     */
    protected fun showPleaseDialog() {
        try {
            if (!isFinishing()) {
                if (mLoadingDialog == null) {
                    mLoadingDialog = PictureLoadingDialog(context)
                }
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss()
                }
                mLoadingDialog.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * dismiss dialog
     */
    protected fun dismissDialog() {
        if (!isFinishing()) {
            try {
                if (mLoadingDialog != null
                        && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss()
                }
            } catch (e: Exception) {
                mLoadingDialog = null
                e.printStackTrace()
            }
        }
    }

    /**
     * compressImage
     */
    protected fun compressImage(result: MutableList<LocalMedia?>) {
        showPleaseDialog()
        if (PictureSelectionConfig.cacheResourcesEngine != null) {
            // 在Android 10上通过图片加载引擎的缓存来获得沙盒内的图片
            PictureThreadUtils.executeByIo(object : SimpleTask<List<LocalMedia?>?>() {
                fun doInBackground(): List<LocalMedia?> {
                    val size = result.size
                    for (i in 0 until size) {
                        val media: LocalMedia = result[i] ?: continue
                        if (!PictureMimeType.isHasHttp(media.getPath())) {
                            val cachePath: String = PictureSelectionConfig.cacheResourcesEngine.onCachePath(context, media.getPath())
                            media.setAndroidQToPath(cachePath)
                        }
                    }
                    return result
                }

                fun onSuccess(result: MutableList<LocalMedia?>) {
                    compressToLuban(result)
                }
            })
        } else {
            compressToLuban(result)
        }
    }

    /**
     * compress
     *
     * @param result
     */
    private fun compressToLuban(result: MutableList<LocalMedia?>) {
        if (config.synOrAsy) {
            PictureThreadUtils.executeByIo(object : SimpleTask<List<File?>?>() {
                @Throws(Exception::class)
                fun doInBackground(): List<File> {
                    return Luban.with(context)
                            .loadMediaData(result)
                            .isCamera(config.camera)
                            .setTargetDir(config.compressSavePath)
                            .setCompressQuality(config.compressQuality)
                            .setFocusAlpha(config.focusAlpha)
                            .setNewCompressFileName(config.renameCompressFileName)
                            .ignoreBy(config.minimumCompressSize).get()
                }

                fun onSuccess(files: List<File>?) {
                    if (files != null && files.size > 0 && files.size == result.size) {
                        handleCompressCallBack(result, files)
                    } else {
                        onResult(result)
                    }
                }
            })
        } else {
            Luban.with(this)
                    .loadMediaData(result)
                    .ignoreBy(config.minimumCompressSize)
                    .isCamera(config.camera)
                    .setCompressQuality(config.compressQuality)
                    .setTargetDir(config.compressSavePath)
                    .setFocusAlpha(config.focusAlpha)
                    .setNewCompressFileName(config.renameCompressFileName)
                    .setCompressListener(object : OnCompressListener() {
                        fun onStart() {}
                        fun onSuccess(list: MutableList<LocalMedia?>) {
                            onResult(list)
                        }

                        fun onError(e: Throwable?) {
                            onResult(result)
                        }
                    }).launch()
        }
    }

    /**
     * handleCompressCallBack
     *
     * @param images
     * @param files
     */
    private fun handleCompressCallBack(images: MutableList<LocalMedia?>?, files: List<File>?) {
        if (images == null || files == null) {
            closeActivity()
            return
        }
        val isAndroidQ: Boolean = SdkVersionUtils.checkedAndroid_Q()
        val size = images.size
        if (files.size == size) {
            var i = 0
            while (i < size) {
                val file = files[i]
                if (file == null) {
                    i++
                    continue
                }
                val path = file.absolutePath
                val image: LocalMedia? = images[i]
                val http: Boolean = PictureMimeType.isHasHttp(path)
                val flag = !TextUtils.isEmpty(path) && http
                val isHasVideo: Boolean = PictureMimeType.isHasVideo(image.getMimeType())
                image.setCompressed(!isHasVideo && !flag)
                image.setCompressPath(if (isHasVideo || flag) null else path)
                if (isAndroidQ) {
                    image.setAndroidQToPath(image.getCompressPath())
                }
                i++
            }
        }
        onResult(images)
    }

    /**
     * crop
     *
     * @param originalPath
     * @param mimeType
     */
    protected fun startCrop(originalPath: String, mimeType: String) {
        if (DoubleUtils.isFastDoubleClick()) {
            return
        }
        if (TextUtils.isEmpty(originalPath)) {
            ToastUtils.s(this, getString(R.string.picture_not_crop_data))
            return
        }
        val options: UCrop.Options = basicOptions()
        if (PictureSelectionConfig.cacheResourcesEngine != null) {
            PictureThreadUtils.executeByIo(object : SimpleTask<String?>() {
                fun doInBackground(): String {
                    return PictureSelectionConfig.cacheResourcesEngine.onCachePath(context, originalPath)
                }

                fun onSuccess(result: String?) {
                    startSingleCropActivity(originalPath, result, mimeType, options)
                }
            })
        } else {
            startSingleCropActivity(originalPath, null, mimeType, options)
        }
    }

    /**
     * single crop
     *
     * @param originalPath
     * @param cachePath
     * @param mimeType
     * @param options
     */
    private fun startSingleCropActivity(originalPath: String, cachePath: String?, mimeType: String, options: UCrop.Options) {
        val isHttp: Boolean = PictureMimeType.isHasHttp(originalPath)
        val suffix = mimeType.replace("image/", ".")
        val file: File = File(PictureFileUtils.getDiskCacheDir(context),
                if (TextUtils.isEmpty(config.renameCropFileName)) DateUtils.getCreateFileName("IMG_CROP_").toString() + suffix else config.renameCropFileName)
        val uri: Uri
        uri = if (!TextUtils.isEmpty(cachePath)) {
            Uri.fromFile(File(cachePath))
        } else {
            if (isHttp || SdkVersionUtils.checkedAndroid_Q()) Uri.parse(originalPath) else Uri.fromFile(File(originalPath))
        }
        UCrop.of(uri, Uri.fromFile(file))
                .withOptions(options)
                .startAnimationActivity(this, if (config.windowAnimationStyle != null) config.windowAnimationStyle.activityCropEnterAnimation else R.anim.picture_anim_enter)
    }

    /**
     * multiple crop
     *
     * @param list
     */
    private var index = 0
    protected fun startCrop(list: ArrayList<CutInfo>?) {
        if (DoubleUtils.isFastDoubleClick()) {
            return
        }
        if (list == null || list.size == 0) {
            ToastUtils.s(this, getString(R.string.picture_not_crop_data))
            return
        }
        val options: UCrop.Options = basicOptions(list)
        val size = list.size
        index = 0
        if (config.chooseMode === PictureMimeType.ofAll() && config.isWithVideoImage) {
            val mimeType = if (size > 0) list[index].getMimeType() else ""
            val isHasVideo: Boolean = PictureMimeType.isHasVideo(mimeType)
            if (isHasVideo) {
                for (i in 0 until size) {
                    val cutInfo: CutInfo = list[i]
                    if (cutInfo != null && PictureMimeType.isHasImage(cutInfo.getMimeType())) {
                        index = i
                        break
                    }
                }
            }
        }
        if (PictureSelectionConfig.cacheResourcesEngine != null) {
            PictureThreadUtils.executeByIo(object : SimpleTask<List<CutInfo?>?>() {
                fun doInBackground(): List<CutInfo> {
                    for (i in 0 until size) {
                        val cutInfo: CutInfo = list[i]
                        val cachePath: String = PictureSelectionConfig.cacheResourcesEngine.onCachePath(context, cutInfo.getPath())
                        if (!TextUtils.isEmpty(cachePath)) {
                            cutInfo.setAndroidQToPath(cachePath)
                        }
                    }
                    return list
                }

                fun onSuccess(list: List<CutInfo>) {
                    if (index < size) {
                        startMultipleCropActivity(list[index], size, options)
                    }
                }
            })
        } else {
            if (index < size) {
                startMultipleCropActivity(list[index], size, options)
            }
        }
    }

    /**
     * startMultipleCropActivity
     *
     * @param cutInfo
     * @param options
     */
    private fun startMultipleCropActivity(cutInfo: CutInfo, count: Int, options: UCrop.Options) {
        val path: String = cutInfo.getPath()
        val mimeType: String = cutInfo.getMimeType()
        val isHttp: Boolean = PictureMimeType.isHasHttp(path)
        val uri: Uri
        uri = if (!TextUtils.isEmpty(cutInfo.getAndroidQToPath())) {
            Uri.fromFile(File(cutInfo.getAndroidQToPath()))
        } else {
            if (isHttp || SdkVersionUtils.checkedAndroid_Q()) Uri.parse(path) else Uri.fromFile(File(path))
        }
        val suffix = mimeType.replace("image/", ".")
        val file: File = File(PictureFileUtils.getDiskCacheDir(this),
                if (TextUtils.isEmpty(config.renameCropFileName)) DateUtils.getCreateFileName("IMG_CROP_")
                        .toString() + suffix else if (config.camera || count == 1) config.renameCropFileName else StringUtils.rename(config.renameCropFileName))
        UCrop.of(uri, Uri.fromFile(file))
                .withOptions(options)
                .startAnimationMultipleCropActivity(this, if (config.windowAnimationStyle != null) config.windowAnimationStyle.activityCropEnterAnimation else R.anim.picture_anim_enter)
    }

    /**
     * Set the crop style parameter
     *
     * @return
     */
    private fun basicOptions(): UCrop.Options {
        return basicOptions(null)
    }

    /**
     * Set the crop style parameter
     *
     * @return
     */
    private fun basicOptions(list: ArrayList<CutInfo>?): UCrop.Options {
        var toolbarColor = 0
        var statusColor = 0
        var titleColor = 0
        var isChangeStatusBarFontColor: Boolean
        if (config.cropStyle != null) {
            if (config.cropStyle.cropTitleBarBackgroundColor !== 0) {
                toolbarColor = config.cropStyle.cropTitleBarBackgroundColor
            }
            if (config.cropStyle.cropStatusBarColorPrimaryDark !== 0) {
                statusColor = config.cropStyle.cropStatusBarColorPrimaryDark
            }
            if (config.cropStyle.cropTitleColor !== 0) {
                titleColor = config.cropStyle.cropTitleColor
            }
            isChangeStatusBarFontColor = config.cropStyle.isChangeStatusBarFontColor
        } else {
            toolbarColor = if (config.cropTitleBarBackgroundColor !== 0) {
                config.cropTitleBarBackgroundColor
            } else {
                AttrsUtils.getTypeValueColor(this, R.attr.picture_crop_toolbar_bg)
            }
            statusColor = if (config.cropStatusBarColorPrimaryDark !== 0) {
                config.cropStatusBarColorPrimaryDark
            } else {
                AttrsUtils.getTypeValueColor(this, R.attr.picture_crop_status_color)
            }
            titleColor = if (config.cropTitleColor !== 0) {
                config.cropTitleColor
            } else {
                AttrsUtils.getTypeValueColor(this, R.attr.picture_crop_title_color)
            }
            isChangeStatusBarFontColor = config.isChangeStatusBarFontColor
            if (!isChangeStatusBarFontColor) {
                isChangeStatusBarFontColor = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_statusFontColor)
            }
        }
        val options: UCrop.Options = if (config.uCropOptions == null) Options() else config.uCropOptions
        options.isOpenWhiteStatusBar(isChangeStatusBarFontColor)
        options.setToolbarColor(toolbarColor)
        options.setStatusBarColor(statusColor)
        options.setToolbarWidgetColor(titleColor)
        options.setCircleDimmedLayer(config.circleDimmedLayer)
        options.setDimmedLayerColor(config.circleDimmedColor)
        options.setDimmedLayerBorderColor(config.circleDimmedBorderColor)
        options.setCircleStrokeWidth(config.circleStrokeWidth)
        options.setShowCropFrame(config.showCropFrame)
        options.setDragFrameEnabled(config.isDragFrame)
        options.setShowCropGrid(config.showCropGrid)
        options.setScaleEnabled(config.scaleEnabled)
        options.setRotateEnabled(config.rotateEnabled)
        options.isMultipleSkipCrop(config.isMultipleSkipCrop)
        options.setHideBottomControls(config.hideBottomControls)
        options.setCompressionQuality(config.cropCompressQuality)
        options.setRenameCropFileName(config.renameCropFileName)
        options.isCamera(config.camera)
        options.setCutListData(list)
        options.isWithVideoImage(config.isWithVideoImage)
        options.setFreeStyleCropEnabled(config.freeStyleCropEnabled)
        options.setCropExitAnimation(if (config.windowAnimationStyle != null) config.windowAnimationStyle.activityCropExitAnimation else 0)
        options.setNavBarColor(if (config.cropStyle != null) config.cropStyle.cropNavBarColor else 0)
        options.withAspectRatio(config.aspect_ratio_x, config.aspect_ratio_y)
        options.isMultipleRecyclerAnimation(config.isMultipleRecyclerAnimation)
        if (config.cropWidth > 0 && config.cropHeight > 0) {
            options.withMaxResultSize(config.cropWidth, config.cropHeight)
        }
        return options
    }

    /**
     * compress or callback
     *
     * @param result
     */
    protected fun handlerResult(result: MutableList<LocalMedia?>) {
        if (config.isCompress
                && !config.isCheckOriginalImage) {
            compressImage(result)
        } else {
            onResult(result)
        }
    }

    /**
     * If you don't have any albums, first create a camera film folder to come out
     *
     * @param folders
     */
    protected fun createNewFolder(folders: MutableList<LocalMediaFolder?>) {
        if (folders.size == 0) {
            // 没有相册 先创建一个最近相册出来
            val newFolder = LocalMediaFolder()
            val folderName: String = if (config.chooseMode === PictureMimeType.ofAudio()) getString(R.string.picture_all_audio) else getString(R.string.picture_camera_roll)
            newFolder.setName(folderName)
            newFolder.setFirstImagePath("")
            newFolder.setCameraFolder(true)
            newFolder.setBucketId(-1)
            newFolder.setChecked(true)
            folders.add(newFolder)
        }
    }

    /**
     * Insert the image into the camera folder
     *
     * @param path
     * @param imageFolders
     * @return
     */
    protected fun getImageFolder(path: String?, realPath: String?, imageFolders: MutableList<LocalMediaFolder>): LocalMediaFolder {
        val imageFile = File(if (PictureMimeType.isContent(path)) realPath else path)
        val folderFile = imageFile.parentFile
        for (folder in imageFolders) {
            if (folderFile != null && folder.getName().equals(folderFile.name)) {
                return folder
            }
        }
        val newFolder = LocalMediaFolder()
        newFolder.setName(if (folderFile != null) folderFile.name else "")
        newFolder.setFirstImagePath(path)
        imageFolders.add(newFolder)
        return newFolder
    }

    /**
     * return image result
     *
     * @param images
     */
    protected fun onResult(images: MutableList<LocalMedia?>) {
        val isAndroidQ: Boolean = SdkVersionUtils.checkedAndroid_Q()
        if (isAndroidQ && config.isAndroidQTransform) {
            showPleaseDialog()
            onResultToAndroidAsy(images)
        } else {
            dismissDialog()
            if (config.camera
                    && config.selectionMode === PictureConfig.MULTIPLE && selectionMedias != null) {
                images.addAll(if (images.size > 0) images.size - 1 else 0, selectionMedias!!)
            }
            if (config.isCheckOriginalImage) {
                val size = images.size
                for (i in 0 until size) {
                    val media: LocalMedia? = images[i]
                    media.setOriginal(true)
                    media.setOriginalPath(media.getPath())
                }
            }
            if (PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onResult(images)
            } else {
                val intent: Intent = com.luck.picture.lib.PictureSelector.Companion.putIntentResult(images)
                setResult(Activity.RESULT_OK, intent)
            }
            closeActivity()
        }
    }

    /**
     * Android Q
     *
     * @param images
     */
    private fun onResultToAndroidAsy(images: List<LocalMedia?>) {
        PictureThreadUtils.executeByIo(object : SimpleTask<List<LocalMedia?>?>() {
            fun doInBackground(): List<LocalMedia?> {
                val size = images.size
                for (i in 0 until size) {
                    val media: LocalMedia? = images[i]
                    if (media == null || TextUtils.isEmpty(media.getPath())) {
                        continue
                    }
                    val isCopyAndroidQToPath = (!media.isCut()
                            && !media.isCompressed()
                            && TextUtils.isEmpty(media.getAndroidQToPath()))
                    if (isCopyAndroidQToPath && PictureMimeType.isContent(media.getPath())) {
                        if (!PictureMimeType.isHasHttp(media.getPath())) {
                            val AndroidQToPath: String = AndroidQTransformUtils.copyPathToAndroidQ(context,
                                    media.getPath(), media.getWidth(), media.getHeight(), media.getMimeType(), config.cameraFileName)
                            media.setAndroidQToPath(AndroidQToPath)
                        }
                    } else if (media.isCut() && media.isCompressed()) {
                        media.setAndroidQToPath(media.getCompressPath())
                    }
                    if (config.isCheckOriginalImage) {
                        media.setOriginal(true)
                        media.setOriginalPath(media.getAndroidQToPath())
                    }
                }
                return images
            }

            fun onSuccess(images: MutableList<LocalMedia?>?) {
                dismissDialog()
                if (images != null) {
                    if (config.camera
                            && config.selectionMode === PictureConfig.MULTIPLE && selectionMedias != null) {
                        images.addAll(if (images.size > 0) images.size - 1 else 0, selectionMedias!!)
                    }
                    if (PictureSelectionConfig.listener != null) {
                        PictureSelectionConfig.listener.onResult(images)
                    } else {
                        val intent: Intent = com.luck.picture.lib.PictureSelector.Companion.putIntentResult(images)
                        setResult(Activity.RESULT_OK, intent)
                    }
                    closeActivity()
                }
            }
        })
    }

    /**
     * Close Activity
     */
    protected fun closeActivity() {
        finish()
        if (config.camera) {
            overridePendingTransition(0, R.anim.picture_anim_fade_out)
        } else {
            overridePendingTransition(0, if (config.windowAnimationStyle != null
                    && config.windowAnimationStyle.activityExitAnimation !== 0) config.windowAnimationStyle.activityExitAnimation else R.anim.picture_anim_exit)
        }
        if (config.camera) {
            if (context is com.luck.picture.lib.PictureSelectorCameraEmptyActivity
                    || context is com.luck.picture.lib.PictureCustomCameraActivity) {
                releaseResultListener()
            }
        } else {
            if (context is com.luck.picture.lib.PictureSelectorActivity) {
                releaseResultListener()
                if (config.openClickSound) {
                    VoiceUtils.getInstance().releaseSoundPool()
                }
            }
        }
    }

    protected override fun onDestroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss()
            mLoadingDialog = null
        }
        super.onDestroy()
    }

    /**
     * 删除部分手机 拍照在DCIM也生成一张的问题
     *
     * @param id
     */
    protected fun removeMedia(id: Int) {
        try {
            val cr: ContentResolver = getContentResolver()
            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection: String = MediaStore.Images.Media._ID + "=?"
            cr.delete(uri, selection, arrayOf(java.lang.Long.toString(id.toLong())))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取DCIM文件下最新一条拍照记录
     *
     * @param mimeType
     * @return
     */
    protected fun getLastImageId(mimeType: String?): Int {
        return try {
            //selection: 指定查询条件
            val absolutePath: String = PictureFileUtils.getDCIMCameraPath()
            val ORDER_BY: String = MediaStore.Files.FileColumns._ID + " DESC"
            val selection: String = MediaStore.Images.Media.DATA + " like ?"
            //定义selectionArgs：
            val selectionArgs = arrayOf("$absolutePath%")
            val data: Cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                    selection, selectionArgs, ORDER_BY)
            if (data != null && data.count > 0 && data.moveToFirst()) {
                val id = data.getInt(data.getColumnIndex(MediaStore.Images.Media._ID))
                val date = data.getLong(data.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                val duration: Int = DateUtils.dateDiffer(date)
                data.close()
                // DCIM文件下最近时间1s以内的图片，可以判定是最新生成的重复照片
                if (duration <= 1) id else -1
            } else {
                -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * get audio path
     *
     * @param data
     */
    protected fun getAudioPath(data: Intent?): String? {
        if (data != null && config.chooseMode === PictureMimeType.ofAudio()) {
            try {
                val uri: Uri = data.getData()
                if (uri != null) {
                    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) uri.path else MediaUtils.getAudioFilePathFromUri(context, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return ""
    }

    /**
     * start to camera、preview、crop
     */
    protected fun startOpenCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            val imageUri: Uri
            if (SdkVersionUtils.checkedAndroid_Q()) {
                imageUri = MediaUtils.createImageUri(getApplicationContext(), config.suffixType)
                if (imageUri != null) {
                    config.cameraPath = imageUri.toString()
                } else {
                    ToastUtils.s(context, "open is camera error，the uri is empty ")
                    if (config.camera) {
                        closeActivity()
                    }
                    return
                }
            } else {
                val chooseMode: Int = if (config.chooseMode === PictureConfig.TYPE_ALL) PictureConfig.TYPE_IMAGE else config.chooseMode
                var cameraFileName = ""
                if (!TextUtils.isEmpty(config.cameraFileName)) {
                    val isSuffixOfImage: Boolean = PictureMimeType.isSuffixOfImage(config.cameraFileName)
                    config.cameraFileName = if (!isSuffixOfImage) StringUtils.renameSuffix(config.cameraFileName, PictureMimeType.JPEG) else config.cameraFileName
                    cameraFileName = if (config.camera) config.cameraFileName else StringUtils.rename(config.cameraFileName)
                }
                val cameraFile: File = PictureFileUtils.createCameraFile(getApplicationContext(),
                        chooseMode, cameraFileName, config.suffixType, config.outPutCameraPath)
                if (cameraFile != null) {
                    config.cameraPath = cameraFile.absolutePath
                    imageUri = PictureFileUtils.parUri(this, cameraFile)
                } else {
                    ToastUtils.s(context, "open is camera error，the uri is empty ")
                    if (config.camera) {
                        closeActivity()
                    }
                    return
                }
            }
            config.cameraMimeType = PictureMimeType.ofImage()
            if (config.isCameraAroundState) {
                cameraIntent.putExtra(PictureConfig.CAMERA_FACING, PictureConfig.CAMERA_BEFORE)
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA)
        }
    }

    /**
     * start to camera、video
     */
    protected fun startOpenCameraVideo() {
        val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            val videoUri: Uri
            if (SdkVersionUtils.checkedAndroid_Q()) {
                videoUri = MediaUtils.createVideoUri(getApplicationContext(), config.suffixType)
                if (videoUri != null) {
                    config.cameraPath = videoUri.toString()
                } else {
                    ToastUtils.s(context, "open is camera error，the uri is empty ")
                    if (config.camera) {
                        closeActivity()
                    }
                    return
                }
            } else {
                val chooseMode: Int = if (config.chooseMode ===
                        PictureConfig.TYPE_ALL) PictureConfig.TYPE_VIDEO else config.chooseMode
                var cameraFileName = ""
                if (!TextUtils.isEmpty(config.cameraFileName)) {
                    val isSuffixOfImage: Boolean = PictureMimeType.isSuffixOfImage(config.cameraFileName)
                    config.cameraFileName = if (isSuffixOfImage) StringUtils.renameSuffix(config.cameraFileName, PictureMimeType.MP4) else config.cameraFileName
                    cameraFileName = if (config.camera) config.cameraFileName else StringUtils.rename(config.cameraFileName)
                }
                val cameraFile: File = PictureFileUtils.createCameraFile(getApplicationContext(),
                        chooseMode, cameraFileName, config.suffixType, config.outPutCameraPath)
                if (cameraFile != null) {
                    config.cameraPath = cameraFile.absolutePath
                    videoUri = PictureFileUtils.parUri(this, cameraFile)
                } else {
                    ToastUtils.s(context, "open is camera error，the uri is empty ")
                    if (config.camera) {
                        closeActivity()
                    }
                    return
                }
            }
            config.cameraMimeType = PictureMimeType.ofVideo()
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            if (config.isCameraAroundState) {
                cameraIntent.putExtra(PictureConfig.CAMERA_FACING, PictureConfig.CAMERA_BEFORE)
            }
            cameraIntent.putExtra(PictureConfig.EXTRA_QUICK_CAPTURE, config.isQuickCapture)
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, config.recordVideoSecond)
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, config.videoQuality)
            startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA)
        }
    }

    /**
     * start to camera audio
     */
    fun startOpenCameraAudio() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            val cameraIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                config.cameraMimeType = PictureMimeType.ofAudio()
                startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA)
            }
        } else {
            PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE)
        }
    }

    /**
     * Release listener
     */
    private fun releaseResultListener() {
        if (config != null) {
            PictureSelectionConfig.destroy()
            LocalMediaPageLoader.setInstanceNull()
            PictureThreadUtils.cancel(PictureThreadUtils.getIoPool())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA)
                }
            } else {
                ToastUtils.s(context, getString(R.string.picture_audio))
            }
        }
    }

    /**
     * showPermissionsDialog
     *
     * @param isCamera
     * @param errorMsg
     */
    protected fun showPermissionsDialog(isCamera: Boolean, errorMsg: String?) {}

    /**
     * Dialog
     *
     * @param content
     */
    protected fun showPromptDialog(content: String?) {
        if (!isFinishing()) {
            val dialog = PictureCustomDialog(context, R.layout.picture_prompt_dialog)
            val btnOk: TextView = dialog.findViewById(R.id.btnOk)
            val tvContent: TextView = dialog.findViewById(R.id.tv_content)
            tvContent.setText(content)
            btnOk.setOnClickListener(View.OnClickListener { v: View? ->
                if (!isFinishing()) {
                    dialog.dismiss()
                }
            })
            dialog.show()
        }
    }

    /**
     * sort
     *
     * @param imageFolders
     */
    protected fun sortFolder(imageFolders: List<LocalMediaFolder?>?) {
        Collections.sort(imageFolders, Comparator<T> { lhs: T, rhs: T ->
            if (lhs.getData() == null || rhs.getData() == null) {
                return@sort 0
            }
            val lSize: Int = lhs.getImageNum()
            val rSize: Int = rhs.getImageNum()
            Integer.compare(rSize, lSize)
        })
    }
}