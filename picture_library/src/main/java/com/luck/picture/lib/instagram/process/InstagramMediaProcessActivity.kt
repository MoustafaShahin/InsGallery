package com.luck.picture.lib.instagram.process

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.config.PictureConfig
import java.io.File
import java.io.IOException
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/5/29 11:39
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramMediaProcessActivity : PictureBaseActivity() {
    private var mSelectMedia: List<LocalMedia>? = null
    private var mTitleBar: InstagramTitleBar? = null
    private var mMediaType: MediaType? = null
    private var isAspectRatio = false

    enum class MediaType {
        SINGLE_IMAGE, SINGLE_VIDEO, MULTI_IMAGE
    }

    override fun getResourceId(): Int {
        return 0
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mSelectMedia = PictureSelector.obtainSelectorList(savedInstanceState)
        }
        super.onCreate(savedInstanceState)
    }

    protected override fun onStart() {
        super.onStart()
        if (container != null && (container as ViewGroup).getChildAt(0) is LifecycleCallBack) {
            ((container as ViewGroup).getChildAt(0) as LifecycleCallBack).onStart(this@InstagramMediaProcessActivity)
        }
    }

    protected override fun onResume() {
        super.onResume()
        if (container != null && (container as ViewGroup).getChildAt(0) is LifecycleCallBack) {
            ((container as ViewGroup).getChildAt(0) as LifecycleCallBack).onResume(this@InstagramMediaProcessActivity)
        }
    }

    protected override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
        if (container != null && (container as ViewGroup).getChildAt(0) is LifecycleCallBack) {
            ((container as ViewGroup).getChildAt(0) as LifecycleCallBack).onPause(this@InstagramMediaProcessActivity)
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (container != null && (container as ViewGroup).getChildAt(0) is LifecycleCallBack) {
            ((container as ViewGroup).getChildAt(0) as LifecycleCallBack).onDestroy(this@InstagramMediaProcessActivity)
        }
    }

    protected override fun initWidgets() {
        if (mSelectMedia == null && getIntent() != null) {
            mSelectMedia = getIntent().getParcelableArrayListExtra<LocalMedia>(PictureConfig.EXTRA_SELECT_LIST)
        }
        if (mSelectMedia == null || mSelectMedia!!.isEmpty()) {
            finish()
        }
        val contentView: FrameLayout = object : FrameLayout(this) {
            protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val width: Int = MeasureSpec.getSize(widthMeasureSpec)
                val height: Int = MeasureSpec.getSize(heightMeasureSpec)
                measureChild(mTitleBar, widthMeasureSpec, heightMeasureSpec)
                getChildAt(0).measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height - mTitleBar.getMeasuredHeight(), MeasureSpec.EXACTLY))
                setMeasuredDimension(width, height)
            }

            protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                mTitleBar.layout(0, 0, mTitleBar.getMeasuredWidth(), mTitleBar.getMeasuredHeight())
                val child: View = getChildAt(0)
                child.layout(0, mTitleBar.getMeasuredHeight(), child.measuredWidth, mTitleBar.getMeasuredHeight() + child.measuredHeight)
            }
        }
        container = contentView
        setContentView(contentView)
        if (PictureMimeType.isHasVideo(mSelectMedia!![0].getMimeType())) {
            mMediaType = MediaType.SINGLE_VIDEO
            createSingleVideoContainer(contentView, mSelectMedia)
        } else if (mSelectMedia!!.size > 1) {
            mMediaType = MediaType.MULTI_IMAGE
            createMultiImageContainer(contentView, mSelectMedia)
        } else {
            mMediaType = MediaType.SINGLE_IMAGE
            createSingleImageContainer(contentView)
        }
        mTitleBar = InstagramTitleBar(this, config, mMediaType)
        contentView.addView(mTitleBar)
        if (getIntent() != null && getIntent().getIntExtra(EXTRA_SINGLE_IMAGE_FILTER, -1) != -1) {
            mTitleBar.setRightViewText(getString(R.string.done))
        }
        mTitleBar.setClickListener(object : OnTitleBarItemOnClickListener() {
            fun onLeftViewClick() {
                if (contentView != null && contentView.getChildAt(0) is ProcessStateCallBack) {
                    (contentView.getChildAt(0) as ProcessStateCallBack).onBack(this@InstagramMediaProcessActivity)
                }
            }

            fun onCenterViewClick(view: ImageView?) {
                if (contentView != null && contentView.getChildAt(0) is ProcessStateCallBack) {
                    (contentView.getChildAt(0) as ProcessStateCallBack).onCenterFeature(this@InstagramMediaProcessActivity, view)
                }
            }

            fun onRightViewClick() {
                if (contentView != null && contentView.getChildAt(0) is ProcessStateCallBack) {
                    (contentView.getChildAt(0) as ProcessStateCallBack).onProcess(this@InstagramMediaProcessActivity)
                }
            }
        })
    }

    override fun initPictureSelectorStyle() {
        container.setBackgroundColor(colorPrimary)
        mTitleBar.setBackgroundColor(colorPrimary)
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mSelectMedia!!.size > 0) {
            PictureSelector.saveSelectorList(outState, mSelectMedia)
        }
    }

    override fun onBackPressed() {
        if (container != null && (container as ViewGroup).getChildAt(0) is ProcessStateCallBack) {
            ((container as ViewGroup).getChildAt(0) as ProcessStateCallBack).onBack(this@InstagramMediaProcessActivity)
        }
    }

    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (container != null && (container as ViewGroup).getChildAt(0) is ProcessStateCallBack) {
            ((container as ViewGroup).getChildAt(0) as ProcessStateCallBack).onActivityResult(this@InstagramMediaProcessActivity, requestCode, resultCode, data)
        }
    }

    private fun createMultiImageContainer(contentView: FrameLayout, selectMedia: List<LocalMedia>?) {
        if (getIntent() != null) {
            isAspectRatio = getIntent().getBooleanExtra(EXTRA_ASPECT_RATIO, false)
        }
        val multiImageContainer = InstagramMediaMultiImageContainer(this, config, selectMedia, isAspectRatio)
        contentView.addView(multiImageContainer, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun createSingleVideoContainer(contentView: FrameLayout, selectMedia: List<LocalMedia>?) {
        if (getIntent() != null) {
            isAspectRatio = getIntent().getBooleanExtra(EXTRA_ASPECT_RATIO, false)
        }
        val singleVideoContainer = InstagramMediaSingleVideoContainer(this, config, selectMedia!![0], isAspectRatio)
        contentView.addView(singleVideoContainer, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun createSingleImageContainer(contentView: FrameLayout) {
        var selectionFilter = -1
        if (getIntent() != null) {
            isAspectRatio = getIntent().getBooleanExtra(EXTRA_ASPECT_RATIO, false)
            selectionFilter = getIntent().getIntExtra(EXTRA_SINGLE_IMAGE_FILTER, -1)
        }
        try {
            val uri: Uri
            val media: LocalMedia = mSelectMedia!![0]
            uri = if (media.isCut()) {
                Uri.fromFile(File(media.getCutPath()))
            } else {
                if (PictureMimeType.isContent(media.getPath())) Uri.parse(media.getPath()) else Uri.fromFile(File(media.getPath()))
            }
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri)
            val singleImageContainer = InstagramMediaSingleImageContainer(this, config, bitmap, isAspectRatio, selectionFilter)
            contentView.addView(singleImageContainer, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun showLoadingView(isShow: Boolean) {
        if (isShow) {
            showPleaseDialog()
        } else {
            dismissDialog()
        }
    }

    companion object {
        const val EXTRA_ASPECT_RATIO = "extra_aspect_ratio"
        const val EXTRA_SINGLE_IMAGE_FILTER = "extra_single_image_filter"
        const val EXTRA_SINGLE_IMAGE_SELECTION_FILTER = "extra_single_image_selection_filter"
        const val REQUEST_SINGLE_IMAGE_PROCESS = 339
        const val REQUEST_MULTI_IMAGE_PROCESS = 440
        const val REQUEST_SINGLE_VIDEO_PROCESS = 441
        const val RESULT_MEDIA_PROCESS_CANCELED = 501
        fun launchActivity(activity: Activity, config: PictureSelectionConfig?, images: List<LocalMedia?>?, extras: Bundle?, requestCode: Int) {
            val intent = Intent(activity.getApplicationContext(), InstagramMediaProcessActivity::class.java)
            intent.putExtra(PictureConfig.EXTRA_CONFIG, config)
            intent.putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST,
                    images as ArrayList<out Parcelable?>?)
            if (extras != null) {
                intent.putExtras(extras)
            }
            activity.startActivityForResult(intent, requestCode)
            activity.overridePendingTransition(0, 0)
        }
    }
}