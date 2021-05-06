package com.luck.picture.lib.tools

import android.graphics.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * @author：luck
 * @date：2020-01-15 18:22
 * @describe：BitmapUtils
 */
object BitmapUtils {
    /**
     * 旋转Bitmap
     *
     * @param bitmap
     * @param angle
     * @return
     */
    fun rotatingImage(bitmap: Bitmap?, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 判断拍照 图片是否旋转
     *
     * @param degree
     * @param file
     */
    fun rotateImage(degree: Int, path: String?) {
        if (degree > 0) {
            try {
                // 针对相片有旋转问题的处理方式
                val opts = BitmapFactory.Options()
                opts.inSampleSize = 2
                val file = File(path)
                var bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
                bitmap = rotatingImage(bitmap, degree)
                if (bitmap != null) {
                    saveBitmapFile(bitmap, file)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 保存Bitmap至本地
     *
     * @param bitmap
     * @param file
     */
    fun saveBitmapFile(bitmap: Bitmap, file: File?) {
        try {
            val bos = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取旋转角度
     *
     * @param orientation
     * @return
     */
    fun getRotationAngle(orientation: Int): Int {
        when (orientation) {
            1 -> return 0
            3 -> return 180
            6 -> return 90
            8 -> return 270
        }
        return 0
    }
}