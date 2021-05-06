package com.luck.picture.lib.instagram.process

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureMimeType
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/6/1 15:27
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramMediaMultiImageContainer(activity: InstagramMediaProcessActivity, config: PictureSelectionConfig, medias: List<LocalMedia>?, isAspectRatio: Boolean) : FrameLayout(activity), OnItemClickListener, OnItemClickListener, ProcessStateCallBack {
    private val mMediaRecyclerView: RecyclerView
    private val mFilterRecyclerView: RecyclerView
    private val mMediaAdapter: MediaAdapter?
    private val mFilterAdapter: InstagramFilterAdapter?
    private val mBitmaps: MutableList<Bitmap>
    private val mMediaLoadingView: View
    private val mFilterLoadingView: View
    private var mSelectionPosition = 0
    private var mGpuImage: GPUImage? = null
    private val mActivity: InstagramMediaProcessActivity
    private val mConfig: PictureSelectionConfig
    private val mMedias: List<LocalMedia>?
    private val mIsAspectRatio: Boolean
    private val mContext: Context
    private var mCurrentFilterType: FilterType = FilterType.I_NORMAL
    private var mProcessPosition = 0
    private var mApplyFilters: IntArray?
    private var isLoadingBitmap = false
    private var isApplyingFilter = false
    private val mLoadedMedias: MutableList<LocalMedia>
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        mMediaRecyclerView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
        mFilterRecyclerView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - width, MeasureSpec.EXACTLY))
        measureChild(mMediaLoadingView, widthMeasureSpec, heightMeasureSpec)
        measureChild(mFilterLoadingView, widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        var viewTop = 0
        var viewLeft = 0
        mMediaRecyclerView.layout(viewLeft, viewTop, viewLeft + mMediaRecyclerView.getMeasuredWidth(), viewTop + mMediaRecyclerView.getMeasuredHeight())
        viewTop = width
        viewLeft = 0
        mFilterRecyclerView.layout(viewLeft, viewTop, viewLeft + mFilterRecyclerView.getMeasuredWidth(), viewTop + mFilterRecyclerView.getMeasuredHeight())
        viewTop = (width - mMediaLoadingView.measuredHeight) / 2
        viewLeft = (width - mMediaLoadingView.measuredWidth) / 2
        mMediaLoadingView.layout(viewLeft, viewTop, viewLeft + mMediaLoadingView.measuredWidth, viewTop + mMediaLoadingView.measuredHeight)
        viewTop = width + (height - width - mFilterLoadingView.measuredHeight) / 2
        viewLeft = (width - mFilterLoadingView.measuredWidth) / 2
        mFilterLoadingView.layout(viewLeft, viewTop, viewLeft + mFilterLoadingView.measuredWidth, viewTop + mFilterLoadingView.measuredHeight)
    }

    fun onItemClick(view: View, position: Int, filterType: FilterType) {
        mCurrentFilterType = filterType
        mFilterRecyclerView.smoothScrollBy(view.left - getMeasuredWidth() / 3, 0)
        if (mSelectionPosition != position) {
            if (mGpuImage == null) {
                mGpuImage = GPUImage(getContext())
            }
            isApplyingFilter = true
            ApplyFilterBitmapTask(this, filterType).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            val previousPosition = mSelectionPosition
            mSelectionPosition = position
            mFilterAdapter.setSelectionPosition(position)
            (view as FilterItemView).selection(true)
            val previousHolder: RecyclerView.ViewHolder = mFilterRecyclerView.findViewHolderForAdapterPosition(previousPosition)
            if (previousHolder != null && previousHolder.itemView != null) {
                (previousHolder.itemView as FilterItemView).selection(false)
            } else {
                mFilterAdapter.notifyItemChanged(previousPosition)
            }
        }
    }

    fun onItemClick(view: View?, position: Int, bitmap: Bitmap?) {
        mProcessPosition = position
        val result: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
        result.add(mMedias!![position])
        val bundle = Bundle()
        bundle.putBoolean(InstagramMediaProcessActivity.EXTRA_ASPECT_RATIO, mIsAspectRatio)
        bundle.putInt(InstagramMediaProcessActivity.EXTRA_SINGLE_IMAGE_FILTER, mApplyFilters!![mProcessPosition])
        InstagramMediaProcessActivity.launchActivity(mActivity, mConfig, result, bundle, InstagramMediaProcessActivity.REQUEST_SINGLE_IMAGE_PROCESS)
    }

    fun onBack(activity: InstagramMediaProcessActivity) {
        activity.setResult(InstagramMediaProcessActivity.RESULT_MEDIA_PROCESS_CANCELED)
        activity.finish()
    }

    override fun onCenterFeature(activity: InstagramMediaProcessActivity?, view: ImageView?) {}
    fun onProcess(activity: InstagramMediaProcessActivity) {
        if (isLoadingBitmap || isApplyingFilter) {
            ToastUtils.s(getContext(), getContext().getString(R.string.next_alert))
        } else {
            PictureLoadingDialog(getContext()).show()
            SaveBitmapsTask(getContext().getApplicationContext(), activity, "Filters", mMediaAdapter.getBitmaps(), mLoadedMedias).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    override fun onActivityResult(activity: InstagramMediaProcessActivity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == InstagramMediaProcessActivity.REQUEST_SINGLE_IMAGE_PROCESS) {
            if (resultCode == Activity.RESULT_OK) {
                var selectionFilter = -1
                if (data != null) {
                    selectionFilter = data.getIntExtra(InstagramMediaProcessActivity.EXTRA_SINGLE_IMAGE_SELECTION_FILTER, -1)
                }
                if (selectionFilter >= 0 && mFilterAdapter != null && mMediaAdapter != null && mApplyFilters != null) {
                    mApplyFilters!![mProcessPosition] = selectionFilter
                    if (mCurrentFilterType.ordinal() !== selectionFilter) {
                        mSelectionPosition = -1
                        mFilterAdapter.setSelectionPosition(-1)
                        val holder: RecyclerView.ViewHolder = mFilterRecyclerView.findViewHolderForAdapterPosition(mCurrentFilterType.ordinal())
                        if (holder != null && holder.itemView != null) {
                            (holder.itemView as FilterItemView).selection(false)
                        } else {
                            mFilterAdapter.notifyItemChanged(mCurrentFilterType.ordinal())
                        }
                    }
                    if (mGpuImage == null) {
                        mGpuImage = GPUImage(getContext())
                    }
                    mGpuImage.setFilter(FilterType.createFilterForType(getContext(), mFilterAdapter.getItem(selectionFilter)))
                    val newBitmap: Bitmap = mGpuImage.getBitmapWithFilterApplied(mBitmaps[mProcessPosition])
                    mMediaAdapter.getBitmaps().remove(mProcessPosition)
                    mMediaAdapter.getBitmaps().add(mProcessPosition, newBitmap)
                    mMediaAdapter.notifyItemChanged(mProcessPosition)
                }
            }
        }
    }

    private class ApplyFilterBitmapTask(container: InstagramMediaMultiImageContainer, filterType: FilterType) : AsyncTask<Void?, Void?, List<Bitmap?>?>() {
        private val mContainerWeakReference: WeakReference<InstagramMediaMultiImageContainer>
        private val mFilterType: FilterType
        protected override fun doInBackground(vararg voids: Void): List<Bitmap> {
            val container = mContainerWeakReference.get()
            if (container != null) {
                container.mGpuImage.setFilter(FilterType.createFilterForType(container.getContext(), mFilterType))
                val newBitMaps: MutableList<Bitmap> = ArrayList<Bitmap>()
                for (i in container.mBitmaps.indices) {
                    container.mApplyFilters!![i] = mFilterType.ordinal()
                    val bitmap: Bitmap = container.mBitmaps[i]
                    newBitMaps.add(container.mGpuImage.getBitmapWithFilterApplied(bitmap))
                }
                return newBitMaps
            }
            return null
        }

        protected override fun onPostExecute(bitmaps: List<Bitmap?>) {
            val container = mContainerWeakReference.get()
            if (container != null && bitmaps != null) {
                container.mMediaAdapter.setBitmaps(bitmaps)
                container.mMediaAdapter.notifyDataSetChanged()
                container.isApplyingFilter = false
            }
        }

        init {
            mContainerWeakReference = WeakReference(container)
            mFilterType = filterType
        }
    }

    private class LoadFilterBitmapTask(private val mContext: Context, imageContainer: InstagramMediaMultiImageContainer, bitmap: Bitmap) : AsyncTask<Void?, Void?, Void?>() {
        private val mImageContainer: WeakReference<InstagramMediaMultiImageContainer>
        private val mBitmap: Bitmap
        protected override fun doInBackground(vararg voids: Void): Void {
            val imageContainer = mImageContainer.get()
            if (imageContainer != null) {
                val gpuImage = GPUImage(mContext)
                gpuImage.setFilter(GPUImageGaussianBlurFilter(25))
                imageContainer.mFilterAdapter.getThumbnailBitmaps(mContext, gpuImage.getBitmapWithFilterApplied(mBitmap))
            }
            return null
        }

        protected override fun onPostExecute(aVoid: Void) {
            val imageContainer = mImageContainer.get()
            if (imageContainer != null) {
                imageContainer.mFilterLoadingView.visibility = View.INVISIBLE
                imageContainer.mFilterRecyclerView.setAdapter(imageContainer.mFilterAdapter)
            }
        }

        init {
            mImageContainer = WeakReference(imageContainer)
            mBitmap = bitmap
        }
    }

    class LoadBitmapTask(private val mContext: Context, container: InstagramMediaMultiImageContainer, bitmaps: MutableList<Bitmap>, medias: List<LocalMedia>?, maxBitmapSize: Int) : AsyncTask<Void?, Void?, Void?>() {
        private val mContainerWeakReference: WeakReference<InstagramMediaMultiImageContainer>
        private val mBitmaps: MutableList<Bitmap>
        private val mMedias: List<LocalMedia>?
        private val mMaxBitmapSize: Int
        protected override fun doInBackground(vararg voids: Void): Void {
            val container = mContainerWeakReference.get()
            if (container != null) {
                container.isLoadingBitmap = true
                startLoadBitmapTask(mContext, container, mMedias, mMaxBitmapSize, mBitmaps)
            }
            return null
        }

        private fun startLoadBitmapTask(context: Context, container: InstagramMediaMultiImageContainer, medias: List<LocalMedia>?, maxBitmapSize: Int, bitmaps: MutableList<Bitmap>) {
            for (media in medias!!) {
                var uri: Uri?
                uri = if (media.isCut()) {
                    Uri.fromFile(File(media.getCutPath()))
                } else {
                    if (PictureMimeType.isContent(media.getPath())) Uri.parse(media.getPath()) else Uri.fromFile(File(media.getPath()))
                }
                BitmapLoadTask(context, uri, uri, maxBitmapSize, maxBitmapSize, BitmapLoadCallbackImpl(context, container, bitmaps, media)).execute()
            }
            FinishLoadBitmapTask(container, bitmaps).execute()
        }

        init {
            mContainerWeakReference = WeakReference(container)
            mBitmaps = bitmaps
            mMedias = medias
            mMaxBitmapSize = maxBitmapSize
        }
    }

    class FinishLoadBitmapTask(container: InstagramMediaMultiImageContainer, bitmaps: List<Bitmap>) : AsyncTask<Void?, Void?, Void?>() {
        private val mContainerWeakReference: WeakReference<InstagramMediaMultiImageContainer>
        private val mBitmaps: List<Bitmap>
        protected override fun doInBackground(vararg voids: Void): Void {
            return null
        }

        protected override fun onPostExecute(aVoid: Void) {
            val container = mContainerWeakReference.get()
            if (container != null) {
                container.mApplyFilters = IntArray(mBitmaps.size)
                container.mMediaLoadingView.visibility = View.INVISIBLE
                container.mMediaAdapter.setBitmaps(mBitmaps)
                container.mMediaRecyclerView.setAdapter(container.mMediaAdapter)
                container.isLoadingBitmap = false
            }
        }

        init {
            mContainerWeakReference = WeakReference(container)
            mBitmaps = bitmaps
        }
    }

    private class BitmapLoadCallbackImpl(private val mContext: Context, container: InstagramMediaMultiImageContainer, bitmaps: MutableList<Bitmap>, media: LocalMedia) : BitmapLoadCallback {
        private val mBitmaps: MutableList<Bitmap>
        private val mMedia: LocalMedia
        private val mContainerWeakReference: WeakReference<InstagramMediaMultiImageContainer>
        fun onBitmapLoaded(bitmap: Bitmap, exifInfo: ExifInfo, imageInputPath: String, imageOutputPath: String?) {
            mBitmaps.add(bitmap)
            val container = mContainerWeakReference.get()
            if (container != null) {
                container.mLoadedMedias.add(mMedia)
                if (mBitmaps.size == 1) {
                    LoadFilterBitmapTask(mContext, container, bitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
        }

        fun onFailure(bitmapWorkerException: Exception) {
            ToastUtils.s(mContext, bitmapWorkerException.message)
        }

        init {
            mContainerWeakReference = WeakReference(container)
            mBitmaps = bitmaps
            mMedia = media
        }
    }

    init {
        mContext = activity
        mActivity = activity
        mConfig = config
        mMedias = medias
        mIsAspectRatio = isAspectRatio
        mBitmaps = ArrayList<Bitmap>()
        mLoadedMedias = ArrayList<LocalMedia>()
        mMediaRecyclerView = RecyclerView(mContext)
        mMediaRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER)
        mMediaRecyclerView.setHasFixedSize(true)
        mMediaRecyclerView.addItemDecoration(InstagramFilterItemDecoration(ScreenUtils.dip2px(mContext, 9), 3))
        mMediaRecyclerView.setLayoutManager(LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false))
        mMediaAdapter = MediaAdapter(mContext, medias)
        mMediaAdapter.setOnItemClickListener(this)
        addView(mMediaRecyclerView)
        mFilterRecyclerView = RecyclerView(mContext)
        mFilterRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER)
        mFilterRecyclerView.setHasFixedSize(true)
        mFilterRecyclerView.addItemDecoration(InstagramFilterItemDecoration(ScreenUtils.dip2px(mContext, 9)))
        mFilterRecyclerView.setLayoutManager(LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false))
        mFilterAdapter = InstagramFilterAdapter(mContext, config)
        mFilterAdapter.setOnItemClickListener(this)
        addView(mFilterRecyclerView)
        mMediaLoadingView = LayoutInflater.from(mContext).inflate(R.layout.picture_alert_dialog, this, false)
        addView(mMediaLoadingView)
        mFilterLoadingView = LayoutInflater.from(mContext).inflate(R.layout.picture_alert_dialog, this, false)
        addView(mFilterLoadingView)
        val maxBitmapSize: Int = BitmapLoadUtils.calculateMaxBitmapSize(getContext())
        LoadBitmapTask(mContext, this, mBitmaps, medias, maxBitmapSize).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}