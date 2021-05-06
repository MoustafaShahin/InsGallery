package com.luck.pictureselector

import android.app.Application
import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

/**
 * @author：luck
 * @date：2019-12-03 22:53
 * @describe：Application
 */
class App : Application(), IApp, CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
        /** PictureSelector日志管理配制开始  */
        // PictureSelector 绑定监听用户获取全局上下文或其他...
        PictureAppMaster.getInstance().app = this
        // PictureSelector Crash日志监听
        PictureSelectorCrashUtils.init { t: Thread?, e: Throwable? -> }
        /** PictureSelector日志管理配制结束  */
    }

    override fun getAppContext(): Context {
        return this
    }

    override fun getPictureSelectorEngine(): PictureSelectorEngine {
        return PictureSelectorEngineImp()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    companion object {
        private val TAG = App::class.java.simpleName
    }
}