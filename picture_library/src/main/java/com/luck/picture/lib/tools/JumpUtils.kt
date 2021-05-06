package com.luck.picture.lib.tools

import android.app.Activity
import android.content.*
import android.os.Bundle
import com.luck.picture.lib.PicturePreviewActivity
import com.luck.picture.lib.PictureSelectorPreviewWeChatStyleActivity
import com.luck.picture.lib.PictureVideoPlayActivity

/**
 * @author：luck
 * @date：2019-11-23 18:57
 * @describe：Activity跳转
 */
object JumpUtils {
    /**
     * 启动视频播放页面
     *
     * @param context
     * @param bundle
     */
    fun startPictureVideoPlayActivity(context: Context, bundle: Bundle?, requestCode: Int) {
        if (!DoubleUtils.isFastDoubleClick()) {
            val intent = Intent()
            intent.setClass(context, PictureVideoPlayActivity::class.java)
            intent.putExtras(bundle!!)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                context.startActivityForResult(intent, requestCode)
            }
        }
    }

    /**
     * 启动预览界面
     *
     * @param context
     * @param isWeChatStyle
     * @param bundle
     * @param requestCode
     */
    fun startPicturePreviewActivity(context: Context, isWeChatStyle: Boolean, bundle: Bundle?, requestCode: Int) {
        if (!DoubleUtils.isFastDoubleClick()) {
            val intent = Intent()
            intent.setClass(context, if (isWeChatStyle) PictureSelectorPreviewWeChatStyleActivity::class.java else PicturePreviewActivity::class.java)
            intent.putExtras(bundle!!)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                context.startActivityForResult(intent, requestCode)
            }
        }
    }
}