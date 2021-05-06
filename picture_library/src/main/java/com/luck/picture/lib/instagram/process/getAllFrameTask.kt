package com.luck.picture.lib.instagram.process

import android.content.Context
import android.net.Uri
import com.luck.picture.lib.config.PictureMimeType
import java.io.File
import java.lang.ref.WeakReference

/**
 * ================================================
 * Created by JessYan on 2020/6/22 16:10
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class getAllFrameTask(context: Context, media: LocalMedia, totalThumbsCount: Int, startPosition: Long,
                      endPosition: Long, onSingleBitmapListener: OnSingleBitmapListener?) : AsyncTask<Void?, Void?, Void?>() {
    private val mContextWeakReference: WeakReference<Context>
    private val mMedia: LocalMedia
    private val mTotalThumbsCount: Int
    private val mStartPosition: Long
    private val mEndPosition: Long
    private val mOnSingleBitmapListener: OnSingleBitmapListener?
    private var isStop = false
    fun setStop(stop: Boolean) {
        isStop = stop
    }

    protected override fun doInBackground(vararg voids: Void): Void {
        val context = mContextWeakReference.get()
        if (context != null) {
            try {
                val mediaMetadataRetriever = MediaMetadataRetriever()
                val uri: Uri
                uri = if (SdkVersionUtils.checkedAndroid_Q() && PictureMimeType.isContent(mMedia.getPath())) {
                    Uri.parse(mMedia.getPath())
                } else {
                    Uri.fromFile(File(mMedia.getPath()))
                }
                mediaMetadataRetriever.setDataSource(context, uri)
                val interval = (mEndPosition - mStartPosition) / (mTotalThumbsCount - 1)
                for (i in 0 until mTotalThumbsCount) {
                    if (isStop) {
                        break
                    }
                    val frameTime = mStartPosition + interval * i
                    var bitmap: Bitmap = mediaMetadataRetriever.getFrameAtTime(frameTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap == null) {
                        continue
                    }
                    try {
                        val cropWidth: Int = Math.min(bitmap.getWidth(), bitmap.getHeight())
                        val cropOffsetX: Int = (bitmap.getWidth() - cropWidth) / 2
                        val cropOffsetY: Int = (bitmap.getHeight() - cropWidth) / 2
                        bitmap = Bitmap.createBitmap(bitmap, cropOffsetX, cropOffsetY, cropWidth, cropWidth)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    mOnSingleBitmapListener?.onSingleBitmapComplete(bitmap)
                }
                mediaMetadataRetriever.release()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return null
    }

    interface OnSingleBitmapListener {
        fun onSingleBitmapComplete(bitmap: Bitmap?)
    }

    init {
        mContextWeakReference = WeakReference(context)
        mMedia = media
        mTotalThumbsCount = totalThumbsCount
        mStartPosition = startPosition
        mEndPosition = endPosition
        mOnSingleBitmapListener = onSingleBitmapListener
    }
}