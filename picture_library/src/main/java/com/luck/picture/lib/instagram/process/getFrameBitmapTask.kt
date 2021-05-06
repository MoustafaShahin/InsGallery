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
class getFrameBitmapTask(context: Context?, media: LocalMedia, isAspectRatio: Boolean, time: Long, onCompleteListener: OnCompleteListener?) : AsyncTask<Void?, Void?, Bitmap?>() {
    private val mContextWeakReference: WeakReference<Context?>
    private val mMedia: LocalMedia
    private val mOnCompleteListener: OnCompleteListener?
    private val isAspectRatio: Boolean
    private val mTime: Long
    private var mCropWidth = 0
    private var mCropHeight = 0

    constructor(context: Context?, media: LocalMedia, isAspectRatio: Boolean, time: Long, cropWidth: Int, cropHeight: Int, onCompleteListener: OnCompleteListener?) : this(context, media, isAspectRatio, time, onCompleteListener) {
        mCropWidth = cropWidth
        mCropHeight = cropHeight
    }

    protected override fun doInBackground(vararg voids: Void): Bitmap {
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
                var frame: Bitmap = mediaMetadataRetriever.getFrameAtTime(mTime)
                if (isAspectRatio) {
                    val width: Int = frame.getWidth()
                    val height: Int = frame.getHeight()
                    val instagramAspectRatio: Float = InstagramPreviewContainer.getInstagramAspectRatio(width, height)
                    val targetAspectRatio = if (instagramAspectRatio > 0) instagramAspectRatio else width * 1.0f / height
                    val adjustWidth: Int
                    val adjustHeight: Int
                    val resizeScale: Float
                    var cropOffsetX = 0
                    var cropOffsetY = 0
                    if (height > width) {
                        adjustHeight = ScreenUtils.getScreenWidth(context)
                        adjustWidth = (adjustHeight * targetAspectRatio).toInt()
                        if (instagramAspectRatio > 0) {
                            resizeScale = adjustWidth * 1.0f / width
                            cropOffsetY = ((height * resizeScale - adjustHeight) / 2).toInt()
                        } else {
                            resizeScale = adjustHeight * 1.0f / height
                        }
                    } else {
                        adjustWidth = ScreenUtils.getScreenWidth(context)
                        adjustHeight = (adjustWidth / targetAspectRatio).toInt()
                        if (instagramAspectRatio > 0) {
                            resizeScale = adjustHeight * 1.0f / height
                            cropOffsetX = ((width * resizeScale - adjustWidth) / 2).toInt()
                        } else {
                            resizeScale = adjustWidth * 1.0f / width
                        }
                    }
                    frame = Bitmap.createScaledBitmap(frame,
                            Math.round(width * resizeScale),
                            Math.round(height * resizeScale), false)
                    frame = Bitmap.createBitmap(frame, cropOffsetX, cropOffsetY, adjustWidth, adjustHeight)
                } else {
                    if (mCropWidth > 0 && mCropHeight > 0) {
                        val scale: Float
                        if (frame.getWidth() > frame.getHeight()) {
                            scale = mCropHeight * 1f / frame.getHeight()
                        } else {
                            scale = mCropWidth * 1f / frame.getWidth()
                        }
                        frame = Bitmap.createScaledBitmap(frame,
                                Math.round(frame.getWidth() * scale),
                                Math.round(frame.getHeight() * scale), false)
                    }
                    val cropWidth: Int = Math.min(frame.getWidth(), frame.getHeight())
                    val cropOffsetX: Int = (frame.getWidth() - cropWidth) / 2
                    val cropOffsetY: Int = (frame.getHeight() - cropWidth) / 2
                    frame = Bitmap.createBitmap(frame, cropOffsetX, cropOffsetY, cropWidth, cropWidth)
                }
                mediaMetadataRetriever.release()
                return frame
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return null
    }

    protected override fun onPostExecute(bitmap: Bitmap) {
        mOnCompleteListener?.onGetBitmapComplete(bitmap)
    }

    interface OnCompleteListener {
        fun onGetBitmapComplete(bitmap: Bitmap?)
    }

    init {
        mContextWeakReference = WeakReference(context)
        mMedia = media
        this.isAspectRatio = isAspectRatio
        mTime = time
        mOnCompleteListener = onCompleteListener
    }
}