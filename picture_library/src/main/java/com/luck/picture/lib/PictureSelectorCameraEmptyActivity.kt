package com.luck.picture.lib

import android.Manifest
import android.net.Uri
import android.os.Environment
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.util.*

/**
 * @author：luck
 * @date：2019-11-15 21:41
 * @describe：PictureSelectorCameraEmptyActivity
 */
class PictureSelectorCameraEmptyActivity : PictureBaseActivity() {
    override fun immersive() {
        ImmersiveManage.immersiveAboveAPI23(this, ContextCompat.getColor(this, R.color.picture_color_transparent), ContextCompat.getColor(this, R.color.picture_color_transparent), openWhiteStatusBar)
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (config == null) {
            closeActivity()
            return
        }
        if (!config.isUseCustomCamera) {
            if (savedInstanceState == null) {
                if (PermissionChecker
                                .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                        PermissionChecker
                                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (PictureSelectionConfig.onCustomCameraInterfaceListener != null) {
                        if (config.chooseMode === PictureConfig.TYPE_VIDEO) {
                            PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, PictureConfig.TYPE_VIDEO)
                        } else {
                            PictureSelectionConfig.onCustomCameraInterfaceListener.onCameraClick(getContext(), config, PictureConfig.TYPE_IMAGE)
                        }
                    } else {
                        onTakePhoto()
                    }
                } else {
                    PermissionChecker.requestPermissions(this, arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE), PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE)
                }
            }
            setTheme(R.style.Picture_Theme_Translucent)
        }
    }

    override val resourceId: Int
        get() = R.layout.picture_empty

    /**
     * open camera
     */
    private fun onTakePhoto() {
        if (PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.CAMERA)) {
            var isPermissionChecker = true
            if (config != null && config.isUseCustomCamera) {
                isPermissionChecker = PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            }
            if (isPermissionChecker) {
                startCamera()
            } else {
                PermissionChecker
                        .requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE)
            }
        } else {
            PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE)
        }
    }

    /**
     * Open the Camera by type
     */
    private fun startCamera() {
        when (config.chooseMode) {
            PictureConfig.TYPE_ALL, PictureConfig.TYPE_IMAGE -> startOpenCamera()
            PictureConfig.TYPE_VIDEO -> startOpenCameraVideo()
            PictureConfig.TYPE_AUDIO -> startOpenCameraAudio()
            else -> {
            }
        }
    }

    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_CROP -> singleCropHandleResult(data)
                PictureConfig.REQUEST_CAMERA -> dispatchHandleCamera(data)
                else -> {
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (config != null && PictureSelectionConfig.listener != null) {
                PictureSelectionConfig.listener.onCancel()
            }
            closeActivity()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data == null) {
                return
            }
            val throwable = data.getSerializableExtra(UCrop.EXTRA_ERROR) as Throwable
            ToastUtils.s(getContext(), throwable.message)
        }
    }

    /**
     * Single picture clipping callback
     *
     * @param data
     */
    protected fun singleCropHandleResult(data: Intent?) {
        if (data == null) {
            return
        }
        val medias: MutableList<LocalMedia> = ArrayList<LocalMedia>()
        val resultUri: Uri = UCrop.getOutput(data) ?: return
        val cutPath = resultUri.path
        val isCutEmpty: Boolean = TextUtils.isEmpty(cutPath)
        val media = LocalMedia(config.cameraPath, 0, false,
                if (config.isCamera) 1 else 0, 0, config.chooseMode)
        if (SdkVersionUtils.checkedAndroid_Q()) {
            val lastIndexOf: Int = config.cameraPath.lastIndexOf("/") + 1
            media.setId(if (lastIndexOf > 0) ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) else -1)
            media.setAndroidQToPath(cutPath)
            if (isCutEmpty) {
                if (PictureMimeType.isContent(config.cameraPath)) {
                    val path: String = PictureFileUtils.getPath(this, Uri.parse(config.cameraPath))
                    media.setSize(if (!TextUtils.isEmpty(path)) File(path).length() else 0)
                } else {
                    media.setSize(File(config.cameraPath).length())
                }
            } else {
                media.setSize(File(cutPath).length())
            }
        } else {
            // Taking a photo generates a temporary id
            media.setId(System.currentTimeMillis())
            media.setSize(File(if (isCutEmpty) media.getPath() else cutPath).length())
        }
        media.setCut(!isCutEmpty)
        media.setCutPath(cutPath)
        val mimeType: String = PictureMimeType.getImageMimeType(cutPath)
        media.setMimeType(mimeType)
        var width = 0
        var height = 0
        media.setOrientation(-1)
        if (PictureMimeType.isContent(media.getPath())) {
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                val size: IntArray = MediaUtils.getVideoSizeForUri(getContext(), Uri.parse(media.getPath()))
                width = size[0]
                height = size[1]
            } else if (PictureMimeType.isHasImage(media.getMimeType())) {
                val size: IntArray = MediaUtils.getImageSizeForUri(getContext(), Uri.parse(media.getPath()))
                width = size[0]
                height = size[1]
            }
        } else {
            if (PictureMimeType.isHasVideo(media.getMimeType())) {
                val size: IntArray = MediaUtils.getVideoSizeForUrl(media.getPath())
                width = size[0]
                height = size[1]
            } else if (PictureMimeType.isHasImage(media.getMimeType())) {
                val size: IntArray = MediaUtils.getImageSizeForUrl(media.getPath())
                width = size[0]
                height = size[1]
            }
        }
        media.setWidth(width)
        media.setHeight(height)
        // The width and height of the image are reversed if there is rotation information
        MediaUtils.setOrientationAsynchronous(getContext(), media, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH
        ) { item ->
            medias.add(item)
            handlerResult(medias)
        }
    }

    /**
     * dispatchHandleCamera
     *
     * @param intent
     */
    protected fun dispatchHandleCamera(intent: Intent?) {
        val isAudio = config.chooseMode === PictureMimeType.ofAudio()
        config.cameraPath = if (isAudio) getAudioPath(intent) else config.cameraPath
        if (TextUtils.isEmpty(config.cameraPath)) {
            return
        }
        showPleaseDialog()
        PictureThreadUtils.executeByIo(object : SimpleTask<LocalMedia?>() {
            fun doInBackground(): LocalMedia {
                val media = LocalMedia()
                var mimeType = if (isAudio) PictureMimeType.MIME_TYPE_AUDIO else ""
                var newSize = IntArray(2)
                var duration: Long = 0
                if (!isAudio) {
                    if (PictureMimeType.isContent(config.cameraPath)) {
                        // content: Processing rules
                        val path: String = PictureFileUtils.getPath(getContext(), Uri.parse(config.cameraPath))
                        if (!TextUtils.isEmpty(path)) {
                            val cameraFile = File(path)
                            mimeType = PictureMimeType.getMimeType(config.cameraMimeType)
                            media.setSize(cameraFile.length())
                        }
                        if (PictureMimeType.isHasImage(mimeType)) {
                            newSize = MediaUtils.getImageSizeForUrlToAndroidQ(getContext(), config.cameraPath)
                        } else if (PictureMimeType.isHasVideo(mimeType)) {
                            newSize = MediaUtils.getVideoSizeForUri(getContext(), Uri.parse(config.cameraPath))
                            duration = MediaUtils.extractDuration(getContext(), SdkVersionUtils.checkedAndroid_Q(), config.cameraPath)
                        }
                        val lastIndexOf: Int = config.cameraPath.lastIndexOf("/") + 1
                        media.setId(if (lastIndexOf > 0) ValueOf.toLong(config.cameraPath.substring(lastIndexOf)) else -1)
                        media.setRealPath(path)
                        // Custom photo has been in the application sandbox into the file
                        val mediaPath: String? = if (intent != null) intent.getStringExtra(PictureConfig.EXTRA_MEDIA_PATH) else null
                        media.setAndroidQToPath(mediaPath)
                    } else {
                        val cameraFile: File = File(config.cameraPath)
                        mimeType = PictureMimeType.getMimeType(config.cameraMimeType)
                        media.setSize(cameraFile.length())
                        if (PictureMimeType.isHasImage(mimeType)) {
                            val degree: Int = PictureFileUtils.readPictureDegree(getContext(), config.cameraPath)
                            BitmapUtils.rotateImage(degree, config.cameraPath)
                            newSize = MediaUtils.getImageSizeForUrl(config.cameraPath)
                        } else if (PictureMimeType.isHasVideo(mimeType)) {
                            newSize = MediaUtils.getVideoSizeForUrl(config.cameraPath)
                            duration = MediaUtils.extractDuration(getContext(), SdkVersionUtils.checkedAndroid_Q(), config.cameraPath)
                        }
                        // Taking a photo generates a temporary id
                        media.setId(System.currentTimeMillis())
                    }
                    media.setPath(config.cameraPath)
                    media.setDuration(duration)
                    media.setMimeType(mimeType)
                    media.setWidth(newSize[0])
                    media.setHeight(newSize[1])
                    if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasVideo(media.getMimeType())) {
                        media.setParentFolderName(Environment.DIRECTORY_MOVIES)
                    } else {
                        media.setParentFolderName(PictureMimeType.CAMERA)
                    }
                    media.setChooseModel(config.chooseMode)
                    val bucketId: Long = MediaUtils.getCameraFirstBucketId(getContext())
                    media.setBucketId(bucketId)
                    // The width and height of the image are reversed if there is rotation information
                    MediaUtils.setOrientationSynchronous(getContext(), media, config.isAndroidQChangeWH, config.isAndroidQChangeVideoWH)
                }
                return media
            }

            fun onSuccess(result: LocalMedia) {
                // Refresh the system library
                dismissDialog()
                if (!SdkVersionUtils.checkedAndroid_Q()) {
                    if (config.isFallbackVersion3) {
                        PictureMediaScannerConnection(getContext(), config.cameraPath)
                    } else {
                        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(config.cameraPath))))
                    }
                }
                dispatchCameraHandleResult(result)
                // Solve some phone using Camera, DCIM will produce repetitive problems
                if (!SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isHasImage(result.getMimeType())) {
                    val lastImageId: Int = MediaUtils.getDCIMLastImageId(getContext())
                    if (lastImageId != -1) {
                        MediaUtils.removeMedia(getContext(), lastImageId)
                    }
                }
            }
        })
    }

    /**
     * dispatchCameraHandleResult
     *
     * @param media
     */
    private fun dispatchCameraHandleResult(media: LocalMedia) {
        val isHasImage: Boolean = PictureMimeType.isHasImage(media.getMimeType())
        if (config.enableCrop && isHasImage) {
            config.originalPath = config.cameraPath
            startCrop(config.cameraPath, media.getMimeType())
        } else if (config.isCompress && isHasImage && !config.isCheckOriginalImage) {
            val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
            result.add(media)
            compressImage(result)
        } else {
            val result: MutableList<LocalMedia> = ArrayList<LocalMedia>()
            result.add(media)
            onResult(result)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE ->                 // Store Permissions
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE)
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_jurisdiction))
                    closeActivity()
                }
            PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE ->                 // Camera Permissions
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto()
                } else {
                    closeActivity()
                    ToastUtils.s(getContext(), getString(R.string.picture_camera))
                }
            PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE ->                 // Recording Permissions
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto()
                } else {
                    closeActivity()
                    ToastUtils.s(getContext(), getString(R.string.picture_audio))
                }
        }
    }

    fun onBackPressed() {
        super.onBackPressed()
        closeActivity()
    }
}