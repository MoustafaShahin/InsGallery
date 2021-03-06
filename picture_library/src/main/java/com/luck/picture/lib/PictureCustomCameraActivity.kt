package com.luck.picture.lib

import android.Manifest
import android.util.Log
import android.view.View
import android.widget.Button
import com.luck.picture.lib.camera.CustomCameraView
import java.io.File
import java.lang.ref.WeakReference

/**
 * @author：luck
 * @date：2020-01-04 14:05
 * @describe：Custom photos and videos
 */
class PictureCustomCameraActivity : PictureSelectorCameraEmptyActivity() {
    private var mCameraView: CustomCameraView? = null
    protected var isEnterSetting = false
    override val isImmersive: Boolean
        get() = false

    protected override fun onCreate(savedInstanceState: Bundle?) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp: WindowManager.LayoutParams = getWindow().getAttributes()
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            getWindow().setAttributes(lp)
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        // 验证存储权限
        val isExternalStorage = PermissionChecker
                .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!isExternalStorage) {
            PermissionChecker.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE)
            return
        }

        // 验证相机权限和麦克风权限
        if (PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.CAMERA)) {
            val isRecordAudio: Boolean = PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (isRecordAudio) {
                createCameraView()
            } else {
                PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE)
            }
        } else {
            PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE)
        }
    }

    /**
     * 创建CameraView
     */
    private fun createCameraView() {
        if (mCameraView == null) {
            mCameraView = CustomCameraView(getContext())
            setContentView(mCameraView)
            initView()
        }
    }

    protected fun onResume() {
        super.onResume()
        // 这里只针对权限被手动拒绝后进入设置页面重新获取权限后的操作
        if (isEnterSetting) {
            val isExternalStorage = PermissionChecker
                    .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                    PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (isExternalStorage) {
                val isCameraPermissionChecker: Boolean = PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.CAMERA)
                if (isCameraPermissionChecker) {
                    val isRecordAudio: Boolean = PermissionChecker
                            .checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    if (isRecordAudio) {
                        createCameraView()
                    } else {
                        showPermissionsDialog(false, getString(R.string.picture_audio))
                    }
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_camera))
                }
            } else {
                showPermissionsDialog(false, getString(R.string.picture_jurisdiction))
            }
            isEnterSetting = false
        }
    }

    /**
     * 初始化控件
     */
    protected fun initView() {
        mCameraView.setPictureSelectionConfig(config)
        // 绑定生命周期
        mCameraView.setBindToLifecycle(WeakReference(this).get())
        // 视频最大拍摄时长
        if (config.recordVideoSecond > 0) {
            mCameraView.setRecordVideoMaxTime(config.recordVideoSecond)
        }
        // 视频最小拍摄时长
        if (config.recordVideoMinSecond > 0) {
            mCameraView.setRecordVideoMinTime(config.recordVideoMinSecond)
        }
        // 获取CameraView
        val cameraView: CameraView = mCameraView.getCameraView()
        if (cameraView != null && config.isCameraAroundState) {
            cameraView.toggleCamera()
        }
        // 获取录制按钮
        val captureLayout: CaptureLayout = mCameraView.getCaptureLayout()
        if (captureLayout != null) {
            captureLayout.setButtonFeatures(config.buttonFeatures)
        }
        // 拍照预览
        mCameraView.setImageCallbackListener { file, imageView ->
            if (config != null && PictureSelectionConfig.imageEngine != null && file != null) {
                PictureSelectionConfig.imageEngine.loadImage(getContext(), file.getAbsolutePath(), imageView)
            }
        }
        // 设置拍照或拍视频回调监听
        mCameraView.setCameraListener(object : CameraListener() {
            fun onPictureSuccess(file: File) {
                config.cameraMimeType = PictureMimeType.ofImage()
                val intent = Intent()
                intent.putExtra(PictureConfig.EXTRA_MEDIA_PATH, file.absolutePath)
                intent.putExtra(PictureConfig.EXTRA_CONFIG, config)
                if (config.camera) {
                    dispatchHandleCamera(intent)
                } else {
                    setResult(RESULT_OK, intent)
                    onBackPressed()
                }
            }

            fun onRecordSuccess(file: File) {
                config.cameraMimeType = PictureMimeType.ofVideo()
                val intent = Intent()
                intent.putExtra(PictureConfig.EXTRA_MEDIA_PATH, file.absolutePath)
                intent.putExtra(PictureConfig.EXTRA_CONFIG, config)
                if (config.camera) {
                    dispatchHandleCamera(intent)
                } else {
                    setResult(RESULT_OK, intent)
                    onBackPressed()
                }
            }

            fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                Log.i(TAG, "onError: $message")
            }
        })

        //左边按钮点击事件
        mCameraView.setOnClickListener { onBackPressed() }
    }

    override fun onBackPressed() {
        if (config != null && config.camera && PictureSelectionConfig.listener != null) {
            PictureSelectionConfig.listener.onCancel()
        }
        closeActivity()
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE ->                 // 存储权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE)
                } else {
                    showPermissionsDialog(true, getString(R.string.picture_jurisdiction))
                }
            PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE ->                 // 相机权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val isRecordAudio: Boolean = PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    if (isRecordAudio) {
                        createCameraView()
                    } else {
                        PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE)
                    }
                } else {
                    showPermissionsDialog(true, getString(R.string.picture_camera))
                }
            PictureConfig.APPLY_RECORD_AUDIO_PERMISSIONS_CODE ->                 // 录音权限
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createCameraView()
                } else {
                    showPermissionsDialog(false, getString(R.string.picture_audio))
                }
        }
    }

    protected override fun showPermissionsDialog(isCamera: Boolean, errorMsg: String?) {
        if (isFinishing()) {
            return
        }
        val dialog = PictureCustomDialog(getContext(), R.layout.picture_wind_base_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val btn_cancel: Button = dialog.findViewById(R.id.btn_cancel)
        val btn_commit: Button = dialog.findViewById(R.id.btn_commit)
        btn_commit.setText(getString(R.string.picture_go_setting))
        val tvTitle: TextView = dialog.findViewById(R.id.tvTitle)
        val tv_content: TextView = dialog.findViewById(R.id.tv_content)
        tvTitle.setText(getString(R.string.picture_prompt))
        tv_content.setText(errorMsg)
        btn_cancel.setOnClickListener { v: View? ->
            if (!isFinishing()) {
                dialog.dismiss()
            }
            closeActivity()
        }
        btn_commit.setOnClickListener { v: View? ->
            if (!isFinishing()) {
                dialog.dismiss()
            }
            PermissionChecker.launchAppDetailsSettings(getContext())
            isEnterSetting = true
        }
        dialog.show()
    }

    companion object {
        private val TAG = PictureCustomCameraActivity::class.java.simpleName
    }
}