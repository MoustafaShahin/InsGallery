package com.luck.picture.lib.instagram

import android.net.Uri
import com.luck.picture.lib.tools.ToastUtils
import java.io.File
import java.io.OutputStream
import java.lang.ref.WeakReference

/**
 * ================================================
 * Created by JessYan on 2020/6/5 18:48
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class BitmapCropSquareTask(bitmap: Bitmap?, imageOutputPath: String?, activity: PictureSelectorInstagramStyleActivity) : AsyncTask<Void?, Void?, Throwable?>() {
    private val mBitmap: Bitmap?
    private val mImageOutputPath: String?
    private val mActivityWeakReference: WeakReference<PictureSelectorInstagramStyleActivity>
    protected override fun doInBackground(vararg voids: Void): Throwable {
        val activity: PictureSelectorInstagramStyleActivity? = mActivityWeakReference.get()
        if (activity == null) {
            return NullPointerException("Activity is null")
        } else if (mBitmap == null) {
            return NullPointerException("Bitmap is null")
        } else if (mImageOutputPath!!.isEmpty()) {
            return NullPointerException("ImageOutputPath is null")
        }
        var outputStream: OutputStream? = null
        try {
            val cropWidth: Int = Math.min(mBitmap.getWidth(), mBitmap.getHeight())
            val cropOffsetX: Int = (mBitmap.getWidth() - cropWidth) / 2
            val cropOffsetY: Int = (mBitmap.getHeight() - cropWidth) / 2
            val croppedBitmap: Bitmap = Bitmap.createBitmap(mBitmap, cropOffsetX, cropOffsetY, cropWidth, cropWidth)
            outputStream = activity.getApplicationContext().getContentResolver().openOutputStream(Uri.fromFile(File(mImageOutputPath)))
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            croppedBitmap.recycle()
        } catch (t: Throwable) {
            t.printStackTrace()
            return t
        } finally {
            BitmapLoadUtils.close(outputStream)
        }
        return null
    }

    protected override fun onPostExecute(throwable: Throwable) {
        val activity: PictureSelectorInstagramStyleActivity? = mActivityWeakReference.get()
        if (activity != null && throwable == null) {
            activity.onActivityResult(UCrop.REQUEST_CROP, Activity.RESULT_OK, Intent()
                    .putExtra(EXTRA_FROM_CAMERA, true)
                    .putExtra(UCrop.EXTRA_OUTPUT_URI, Uri.fromFile(File(mImageOutputPath))))
        } else if (throwable != null) {
            ToastUtils.s(activity, throwable.message)
        }
    }

    companion object {
        const val EXTRA_FROM_CAMERA = "extra_from_camera"
    }

    init {
        mBitmap = bitmap
        mImageOutputPath = imageOutputPath
        mActivityWeakReference = WeakReference<PictureSelectorInstagramStyleActivity>(activity)
    }
}