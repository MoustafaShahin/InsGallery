package com.luck.picture.lib.tools

import android.content.*
import android.net.Uri
import android.text.TextUtils
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.tools.StringUtils
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File
import java.util.*

/**
 * @author：luck
 * @date：2019-11-08 19:25
 * @describe：Android Q相关处理类
 */
object AndroidQTransformUtils {
    /**
     * 解析Android Q版本下图片
     * #耗时操作需要放在子线程中操作
     *
     * @param ctx
     * @param uri
     * @param mineType
     * @param customFileName
     * @return
     */
    fun copyPathToAndroidQ(ctx: Context, url: String?, width: Int, height: Int, mineType: String?, customFileName: String?): String? {
        // 这里就是利用图片加载引擎的特性，因为图片加载器加载过了图片本地就有缓存，当然前提是用户设置了缓存策略
        if (PictureSelectionConfig.cacheResourcesEngine != null) {
            val cachePath: String = PictureSelectionConfig.cacheResourcesEngine.onCachePath(ctx, url)
            if (!TextUtils.isEmpty(cachePath)) {
                return cachePath
            }
        }

        // 走普通的文件复制流程，拷贝至应用沙盒内来
        var inBuffer: BufferedSource? = null
        try {
            val uri = Uri.parse(url)
            val encryptionValue: String = StringUtils.getEncryptionValue(url, width, height)
            val newPath: String = PictureFileUtils.createFilePath(ctx, encryptionValue, mineType, customFileName)
            val outFile = File(newPath)
            if (outFile.exists()) {
                return newPath
            }
            inBuffer = Objects.requireNonNull(ctx.contentResolver.openInputStream(uri)).source().buffer()
            val copyFileSuccess: Boolean = PictureFileUtils.bufferCopy(inBuffer, outFile)
            if (copyFileSuccess) {
                return newPath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (inBuffer != null && inBuffer.isOpen) {
                PictureFileUtils.close(inBuffer)
            }
        }
        return null
    }

    /**
     * 复制文件至AndroidQ手机相册目录
     *
     * @param context
     * @param inFile
     * @param outUri
     */
    fun copyPathToDCIM(context: Context, inFile: File?, outUri: Uri?): Boolean {
        try {
            val fileOutputStream = context.contentResolver.openOutputStream(outUri!!)
            return PictureFileUtils.bufferCopy(inFile, fileOutputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}