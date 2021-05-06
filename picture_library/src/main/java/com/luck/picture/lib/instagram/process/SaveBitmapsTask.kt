package com.luck.picture.lib.instagram.process

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/6/15 16:04
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class SaveBitmapsTask(context: Context, activity: InstagramMediaProcessActivity, folderName: String, bitmaps: List<Bitmap>, loadedMedias: List<LocalMedia>) : AsyncTask<Void?, Void?, Void?>() {
    private val mContextWeakReference: WeakReference<Context>
    private val mActivityWeakReference: WeakReference<InstagramMediaProcessActivity>
    private val folderName: String
    private val mBitmaps: List<Bitmap>
    private val mLoadedMedias: List<LocalMedia>
    private val mHandler: Handler
    private var mCount = 0
    protected override fun doInBackground(vararg voids: Void): Void {
        for (i in mBitmaps.indices) {
            val bitmap: Bitmap = mBitmaps[i]
            val media: LocalMedia = mLoadedMedias[i]
            saveImage(folderName, bitmap, media)
        }
        return null
    }

    private fun finish() {
        val activity: InstagramMediaProcessActivity? = mActivityWeakReference.get()
        if (activity != null) {
            activity.setResult(Activity.RESULT_OK, Intent().putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST, mLoadedMedias as ArrayList<out Parcelable>))
            activity.finish()
        }
    }

    private fun saveImage(folderName: String, image: Bitmap, media: LocalMedia) {
        val context = mContextWeakReference.get() ?: return
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(path, "$folderName/$fileName")
        var outputStream: OutputStream? = null
        try {
            file.parentFile.mkdirs()
            outputStream = context.contentResolver.openOutputStream(Uri.fromFile(file))
            image.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            image.recycle()
            MediaScannerConnection.scanFile(context, arrayOf(
                    file.toString()
            ), null,
                    OnScanCompletedListener { path1: String?, uri: Uri? ->
                        mHandler.post {
                            media.setCut(true)
                            media.setCutPath(path1)
                            media.setSize(File(path1).length())
                            media.setAndroidQToPath(if (SdkVersionUtils.checkedAndroid_Q()) path1 else media.getAndroidQToPath())
                            mCount++
                            if (mCount == mBitmaps.size) {
                                finish()
                            }
                        }
                    })
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            BitmapLoadUtils.close(outputStream)
        }
    }

    init {
        mContextWeakReference = WeakReference(context)
        mActivityWeakReference = WeakReference<InstagramMediaProcessActivity>(activity)
        this.folderName = folderName
        mBitmaps = bitmaps
        mLoadedMedias = loadedMedias
        mHandler = Handler()
    }
}