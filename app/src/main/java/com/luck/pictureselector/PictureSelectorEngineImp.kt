package com.luck.pictureselector

import android.util.Log


/**
 * @author：luck
 * @date：2020/4/22 12:15 PM
 * @describe：PictureSelectorEngineImp
 */
class PictureSelectorEngineImp : PictureSelectorEngine {
    override fun createEngine(): ImageEngine {
        // TODO 这种情况是内存极度不足的情况下，比如开启开发者选项中的不保留活动或后台进程限制，导致ImageEngine被回收
        // 重新创建图片加载引擎
        return GlideEngine.createGlideEngine()
    }

    override fun getResultCallbackListener(): OnResultCallbackListener<LocalMedia?> {
        return object : OnResultCallbackListener<LocalMedia?> {


            override fun onCancel() {
                Log.i(TAG, "PictureSelector onCancel")
            }

            override fun onResult(result: MutableList<LocalMedia?>?) {
                // TODO 这种情况是内存极度不足的情况下，比如开启开发者选项中的不保留活动或后台进程限制，导致OnResultCallbackListener被回收
                // 可以在这里进行一些补救措施，通过广播或其他方式将结果推送到相应页面，防止结果丢失的情况
                Log.i(TAG, "onResult:" + result?.size)            }
        }
    }

    companion object {
        private val TAG = PictureSelectorEngineImp::class.java.simpleName
    }
}