package com.luck.picture.lib.widget

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.adapter.PictureAlbumDirectoryAdapter

/**
 * @author：luck
 * @date：2017-5-25 17:02
 * @describe：文件目录PopupWindow
 */
class FolderPopWindow(private val context: Context, config: PictureSelectionConfig) : PopupWindow() {
    private val window: View
    private var rootView: View? = null
    private var mRecyclerView: RecyclerView? = null
    private var adapter: PictureAlbumDirectoryAdapter? = null
    private var isDismiss = false
    private var ivArrowView: ImageView? = null
    private var drawableUp: Drawable? = null
    private var drawableDown: Drawable? = null
    private val chooseMode: Int
    private val config: PictureSelectionConfig
    private val maxHeight: Int
    private var rootViewBg: View? = null
    fun initView() {
        rootViewBg = window.findViewById(R.id.rootViewBg)
        adapter = PictureAlbumDirectoryAdapter(config)
        mRecyclerView = window.findViewById(R.id.folder_list)
        mRecyclerView.setLayoutManager(LinearLayoutManager(context))
        mRecyclerView.setAdapter(adapter)
        rootView = window.findViewById(R.id.rootView)
        rootViewBg.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            rootView.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })
        }
    }

    fun bindFolder(folders: List<LocalMediaFolder?>?) {
        adapter!!.setChooseMode(chooseMode)
        adapter!!.bindFolderData(folders)
        val lp: ViewGroup.LayoutParams = mRecyclerView.getLayoutParams()
        lp.height = if (folders != null && folders.size > 8) maxHeight else ViewGroup.LayoutParams.WRAP_CONTENT
    }

    fun getFolderData(): List<LocalMediaFolder> {
        return adapter.getFolderData()
    }

    fun isEmpty(): Boolean {
        return adapter.getFolderData().size() === 0
    }

    fun getFolder(position: Int): LocalMediaFolder? {
        return if (adapter.getFolderData().size() > 0
                && position < adapter.getFolderData().size()) adapter.getFolderData().get(position) else null
    }

    fun setArrowImageView(ivArrowView: ImageView?) {
        this.ivArrowView = ivArrowView
    }

    override fun showAsDropDown(anchor: View) {
        try {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                val location = IntArray(2)
                anchor.getLocationInWindow(location)
                showAtLocation(anchor, Gravity.NO_GRAVITY, 0, location[1] + anchor.height)
            } else {
                super.showAsDropDown(anchor)
            }
            isDismiss = false
            ivArrowView!!.setImageDrawable(drawableUp)
            AnimUtils.rotateArrow(ivArrowView, true)
            rootViewBg!!.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .setStartDelay(250).start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnAlbumItemClickListener(listener: OnAlbumItemClickListener?) {
        adapter!!.setOnAlbumItemClickListener(listener)
    }

    override fun dismiss() {
        if (isDismiss) {
            return
        }
        rootViewBg!!.animate()
                .alpha(0f)
                .setDuration(50)
                .start()
        ivArrowView!!.setImageDrawable(drawableDown)
        AnimUtils.rotateArrow(ivArrowView, false)
        isDismiss = true
        super@FolderPopWindow.dismiss()
        isDismiss = false
    }

    /**
     * 设置选中状态
     */
    fun updateFolderCheckStatus(result: List<LocalMedia>) {
        try {
            val folders: List<LocalMediaFolder> = adapter.getFolderData()
            val size = folders.size
            val resultSize = result.size
            for (i in 0 until size) {
                val folder: LocalMediaFolder = folders[i]
                folder.setCheckedNum(0)
                for (j in 0 until resultSize) {
                    val media: LocalMedia = result[j]
                    if (folder.getName().equals(media.getParentFolderName())
                            || folder.getBucketId() === -1) {
                        folder.setCheckedNum(1)
                        break
                    }
                }
            }
            adapter!!.bindFolderData(folders)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        this.config = config
        chooseMode = config.chooseMode
        window = LayoutInflater.from(context).inflate(R.layout.picture_window_folder, null)
        this.setContentView(window)
        this.setWidth(RelativeLayout.LayoutParams.MATCH_PARENT)
        this.setHeight(RelativeLayout.LayoutParams.WRAP_CONTENT)
        this.setAnimationStyle(R.style.PictureThemeWindowStyle)
        this.setFocusable(true)
        this.setOutsideTouchable(true)
        this.update()
        if (config.style != null) {
            if (config.style.pictureTitleUpResId !== 0) {
                drawableUp = ContextCompat.getDrawable(context, config.style.pictureTitleUpResId)
            }
            if (config.style.pictureTitleDownResId !== 0) {
                drawableDown = ContextCompat.getDrawable(context, config.style.pictureTitleDownResId)
            }
        } else {
            if (config.isWeChatStyle) {
                drawableUp = ContextCompat.getDrawable(context, R.drawable.picture_icon_wechat_up)
                drawableDown = ContextCompat.getDrawable(context, R.drawable.picture_icon_wechat_down)
            } else {
                if (config.upResId != 0) {
                    drawableUp = ContextCompat.getDrawable(context, config.upResId)
                } else {
                    // 兼容老的Theme方式
                    drawableUp = AttrsUtils.getTypeValueDrawable(context, R.attr.picture_arrow_up_icon)
                }
                if (config.downResId != 0) {
                    drawableDown = ContextCompat.getDrawable(context, config.downResId)
                } else {
                    // 兼容老的Theme方式 picture.arrow_down.icon
                    drawableDown = AttrsUtils.getTypeValueDrawable(context, R.attr.picture_arrow_down_icon)
                }
            }
        }
        maxHeight = (ScreenUtils.getScreenHeight(context) * 0.6)
        initView()
    }
}