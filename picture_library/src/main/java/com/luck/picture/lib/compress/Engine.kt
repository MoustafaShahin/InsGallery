package com.luck.picture.lib.compress

import android.graphics.*
import android.media.*
import com.luck.picture.lib.compress.Checker
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Responsible for starting compress and managing active and cached resources.
 */
internal class Engine(srcImg: InputStreamProvider, private val tagImg: File, focusAlpha: Boolean, compressQuality: Int) {
    private val srcImg: InputStreamProvider
    private var srcWidth = 0
    private var srcHeight = 0
    private val focusAlpha: Boolean
    private var compressQuality: Int
    private fun computeSize(): Int {
        srcWidth = if (srcWidth % 2 == 1) srcWidth + 1 else srcWidth
        srcHeight = if (srcHeight % 2 == 1) srcHeight + 1 else srcHeight
        val longSide = Math.max(srcWidth, srcHeight)
        val shortSide = Math.min(srcWidth, srcHeight)
        val scale = shortSide.toFloat() / longSide
        return if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                1
            } else if (longSide < 4990) {
                2
            } else if (longSide > 4990 && longSide < 10240) {
                4
            } else {
                longSide / 1280
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            if (longSide / 1280 == 0) 1 else longSide / 1280
        } else {
            Math.ceil(longSide / (1280.0 / scale)).toInt()
        }
    }

    private fun rotatingImage(bitmap: Bitmap?, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @Throws(IOException::class)
    fun compress(): File {
        val options = BitmapFactory.Options()
        options.inSampleSize = computeSize()
        var tagBitmap = BitmapFactory.decodeStream(srcImg.open(), null, options)
        val stream = ByteArrayOutputStream()
        if (srcImg.getMedia() != null && !srcImg.getMedia().isCut()) {
            if (Checker.SINGLE.isJPG(srcImg.getMedia().getMimeType())) {
                var orientation: Int = srcImg.getMedia().getOrientation()
                if (orientation > 0) {
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> orientation = 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> orientation = 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> orientation = 270
                        else -> {
                        }
                    }
                    tagBitmap = rotatingImage(tagBitmap, orientation)
                }
            }
        }
        if (tagBitmap != null) {
            compressQuality = if (compressQuality <= 0 || compressQuality > 100) DEFAULT_QUALITY else compressQuality
            tagBitmap.compress(if (focusAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, compressQuality, stream)
            tagBitmap.recycle()
        }
        val fos = FileOutputStream(tagImg)
        fos.write(stream.toByteArray())
        fos.flush()
        fos.close()
        stream.close()
        return tagImg
    }

    companion object {
        private const val DEFAULT_QUALITY = 80
    }

    init {
        this.srcImg = srcImg
        this.focusAlpha = focusAlpha
        this.compressQuality = if (compressQuality <= 0) DEFAULT_QUALITY else compressQuality
        if (srcImg.getMedia() != null && srcImg.getMedia().getWidth() > 0 && srcImg.getMedia().getHeight() > 0) {
            srcWidth = srcImg.getMedia().getWidth()
            srcHeight = srcImg.getMedia().getHeight()
        } else {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            options.inSampleSize = 1
            BitmapFactory.decodeStream(srcImg.open(), null, options)
            srcWidth = options.outWidth
            srcHeight = options.outHeight
        }
    }
}