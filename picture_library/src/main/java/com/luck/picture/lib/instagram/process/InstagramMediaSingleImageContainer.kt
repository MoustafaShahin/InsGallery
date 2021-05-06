package com.luck.picture.lib.instagram.process

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.dialog.PictureCustomDialog
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.lang.ref.WeakReference

/**
 * ================================================
 * Created by JessYan on 2020/6/1 15:27
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramMediaSingleImageContainer(context: Context, config: PictureSelectionConfig, bitmap: Bitmap, isAspectRatio: Boolean, selectionFilter: Int) : FrameLayout(context), OnItemClickListener, ProcessStateCallBack {
    private val mImageView: GPUImageView
    private val mRecyclerView: RecyclerView
    private val mPaint: Paint
    private val mAdapter: InstagramFilterAdapter
    private var mSelectionPosition = 0
    private val mLoadingView: View
    private val mConfig: PictureSelectionConfig
    private val mBitmap: Bitmap
    private val mSelectionFilter: Int
    private var mDialog: PictureCustomDialog? = null
    private val mGpuImage: GPUImage
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
        mRecyclerView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - width, MeasureSpec.EXACTLY))
        measureChild(mLoadingView, widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        var viewTop: Int = (width - mImageView.getMeasuredHeight()) / 2
        var viewLeft: Int = (width - mImageView.getMeasuredWidth()) / 2
        mImageView.layout(viewLeft, viewTop, viewLeft + mImageView.getMeasuredWidth(), viewTop + mImageView.getMeasuredHeight())
        viewTop = width
        viewLeft = 0
        mRecyclerView.layout(viewLeft, viewTop, viewLeft + mRecyclerView.getMeasuredWidth(), viewTop + mRecyclerView.getMeasuredHeight())
        viewTop += (height - width - mLoadingView.measuredHeight) / 2
        viewLeft = (width - mLoadingView.measuredWidth) / 2
        mLoadingView.layout(viewLeft, viewTop, viewLeft + mLoadingView.measuredWidth, viewTop + mLoadingView.measuredHeight)
    }

    protected override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, getMeasuredWidth().toFloat(), getMeasuredWidth().toFloat(), mPaint)
    }

    fun onItemClick(view: View, position: Int, filterType: FilterType?) {
        mRecyclerView.smoothScrollBy(view.left - getMeasuredWidth() / 3, 0)
        if (mSelectionPosition != position) {
            mImageView.setFilter(FilterType.createFilterForType(getContext(), filterType))
            mGpuImage.setFilter(FilterType.createFilterForType(getContext(), filterType))
            val previousPosition = mSelectionPosition
            mSelectionPosition = position
            mAdapter.setSelectionPosition(position)
            (view as FilterItemView).selection(true)
            val previousHolder: RecyclerView.ViewHolder = mRecyclerView.findViewHolderForAdapterPosition(previousPosition)
            if (previousHolder != null && previousHolder.itemView != null) {
                (previousHolder.itemView as FilterItemView).selection(false)
            } else {
                mAdapter.notifyItemChanged(previousPosition)
            }
        }
    }

    fun onSaveImage(listener: GPUImageView.OnPictureSavedListener?) {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        SaveTask(getContext().getApplicationContext(), "Filters", fileName, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun onBack(activity: InstagramMediaProcessActivity) {
        if (mSelectionFilter >= 0 && mSelectionFilter != mSelectionPosition) {
            if (!activity.isFinishing()) {
                if (mDialog == null) {
                    val layout = ProcessAlertView(getContext(), mConfig)
                    layout.setOnAlertListener(object : onAlertListener() {
                        fun onAgree() {
                            activity.finish()
                        }

                        fun onCancel() {
                            mDialog!!.dismiss()
                        }
                    })
                    mDialog = PictureCustomDialog(getContext(), layout)
                    mDialog!!.setCancelable(true)
                    mDialog!!.setCanceledOnTouchOutside(false)
                }
                mDialog!!.show()
            }
        } else {
            activity.setResult(InstagramMediaProcessActivity.RESULT_MEDIA_PROCESS_CANCELED)
            activity.finish()
        }
    }

    override fun onCenterFeature(activity: InstagramMediaProcessActivity?, view: ImageView?) {}
    fun onProcess(activity: InstagramMediaProcessActivity) {
        if (mSelectionFilter == -1) {
            onSaveImage(GPUImageView.OnPictureSavedListener { uri: Uri? ->
                activity.setResult(Activity.RESULT_OK, Intent().putExtra(UCrop.EXTRA_OUTPUT_URI, uri))
                activity.finish()
            })
        } else {
            if (mSelectionFilter != mSelectionPosition) {
                activity.setResult(Activity.RESULT_OK, Intent().putExtra(InstagramMediaProcessActivity.EXTRA_SINGLE_IMAGE_SELECTION_FILTER, mSelectionPosition))
            }
            activity.finish()
        }
    }

    override fun onActivityResult(activity: InstagramMediaProcessActivity?, requestCode: Int, resultCode: Int, data: Intent?) {}
    private inner class SaveTask(context: Context?, folderName: String, fileName: String, width: Int, height: Int,
                                 listener: GPUImageView.OnPictureSavedListener?) : AsyncTask<Void?, Void?, Void?>() {
        private val folderName: String
        private val fileName: String
        private val width: Int
        private val height: Int
        private val listener: GPUImageView.OnPictureSavedListener?
        private val handler: Handler
        private val mContextWeakReference: WeakReference<Context?>

        constructor(context: Context?, folderName: String, fileName: String,
                    listener: GPUImageView.OnPictureSavedListener?) : this(context, folderName, fileName, 0, 0, listener) {
        }

        protected override fun doInBackground(vararg params: Void): Void {
            try {
//                Bitmap result = width != 0 ? mImageView.capture(width, height) : mImageView.capture();
                val result: Bitmap = mGpuImage.getBitmapWithFilterApplied(mBitmap)
                saveImage(folderName, fileName, result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun saveImage(folderName: String, fileName: String, image: Bitmap) {
            val context = mContextWeakReference.get() ?: return
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
                            if (listener != null) {
                                handler.post { listener.onPictureSaved(Uri.fromFile(File(path1))) }
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
            this.folderName = folderName
            this.fileName = fileName
            this.width = width
            this.height = height
            this.listener = listener
            handler = Handler()
        }
    }

    private class LoadBitmapTask(private val mContext: Context, imageContainer: InstagramMediaSingleImageContainer, bitmap: Bitmap) : AsyncTask<Void?, Void?, Void?>() {
        private val mImageContainer: WeakReference<InstagramMediaSingleImageContainer>
        private val mBitmap: Bitmap
        protected override fun doInBackground(vararg voids: Void): Void {
            val imageContainer = mImageContainer.get()
            imageContainer?.mAdapter?.getThumbnailBitmaps(mContext, mBitmap)
            return null
        }

        protected override fun onPostExecute(aVoid: Void) {
            val imageContainer = mImageContainer.get()
            if (imageContainer != null) {
                imageContainer.mLoadingView.visibility = View.INVISIBLE
                imageContainer.mRecyclerView.setAdapter(imageContainer.mAdapter)
                if (imageContainer.mSelectionFilter > 0) {
                    imageContainer.mRecyclerView.scrollToPosition(imageContainer.mSelectionFilter - 1)
                }
            }
        }

        init {
            mImageContainer = WeakReference(imageContainer)
            mBitmap = bitmap
        }
    }

    init {
        mConfig = config
        mBitmap = bitmap
        mSelectionFilter = selectionFilter
        setWillNotDraw(false)
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            mPaint.color = Color.parseColor("#363636")
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            mPaint.color = Color.parseColor("#004561")
        } else {
            mPaint.color = Color.parseColor("#efefef")
        }
        mGpuImage = GPUImage(context)
        mImageView = GPUImageView(context)
        addView(mImageView)
        if (isAspectRatio) {
            val targetAspectRatio: Float = InstagramPreviewContainer.getInstagramAspectRatio(bitmap.getWidth(), bitmap.getHeight())
            if (targetAspectRatio > 0) {
                mImageView.setRatio(targetAspectRatio)
            }
        }
        mImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        mImageView.setImage(bitmap)
        mRecyclerView = RecyclerView(context)
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.addItemDecoration(InstagramFilterItemDecoration(ScreenUtils.dip2px(context, 9)))
        mRecyclerView.setLayoutManager(LinearLayoutManager(context, RecyclerView.HORIZONTAL, false))
        mAdapter = InstagramFilterAdapter(context, config)
        if (mSelectionFilter > 0) {
            mImageView.setFilter(FilterType.createFilterForType(getContext(), mAdapter.getItem(mSelectionFilter)))
            mSelectionPosition = mSelectionFilter
            mAdapter.setSelectionPosition(mSelectionFilter)
        }
        mAdapter.setOnItemClickListener(this)
        addView(mRecyclerView)
        mLoadingView = LayoutInflater.from(context).inflate(R.layout.picture_alert_dialog, this, false)
        addView(mLoadingView)
        LoadBitmapTask(context, this, bitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}