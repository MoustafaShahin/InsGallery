package com.luck.pictureselector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.broadcast.BroadcastAction
import com.luck.picture.lib.broadcast.BroadcastManager
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.decoration.GridSpacingItemDecoration
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.instagram.InsGallery
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.luck.picture.lib.permissions.PermissionChecker
import com.luck.picture.lib.tools.PictureFileUtils
import com.luck.picture.lib.tools.ScreenUtils
import com.luck.picture.lib.tools.ToastUtils
import com.luck.picture.lib.tools.ValueOf
import com.luck.pictureselector.MainActivity
import com.luck.pictureselector.adapter.GridImageAdapter
import com.luck.pictureselector.adapter.GridImageAdapter.onAddPicClickListener
import com.luck.pictureselector.listener.DragListener
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author：luck
 * @data：2019/12/20 晚上 23:12
 * @描述: Demo
 */
class MainActivity : AppCompatActivity(), View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: GridImageAdapter
    private var maxSelectNum = 9
    private lateinit var tv_select_num: TextView
    private lateinit var tvDeleteText: TextView
    private lateinit var left_back: ImageView
    private lateinit var minus: ImageView
    private lateinit var plus: ImageView
    private lateinit var rgb_style: RadioGroup
    private var isUpward = false
    private var needScaleBig = true
    private var needScaleSmall = true
    private var mItemTouchHelper: ItemTouchHelper? = null
    private var mDragListener: DragListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // 被回收
        } else {
            clearCache()
        }
        setContentView(R.layout.activity_main)
        minus = findViewById(R.id.minus)
        plus = findViewById(R.id.plus)
        tvDeleteText = findViewById(R.id.tv_delete_text)
        tv_select_num = findViewById(R.id.tv_select_num)
        rgb_style = findViewById(R.id.rgb_style)
        rgb_style.setOnCheckedChangeListener(this)
        mRecyclerView = findViewById(R.id.recycler)
        left_back = findViewById(R.id.left_back)
        left_back.setOnClickListener(this)
        minus.setOnClickListener(this)
        plus.setOnClickListener(this)
        tv_select_num.setText(ValueOf.toString(maxSelectNum))
        val manager = FullyGridLayoutManager(this,
                4, GridLayoutManager.VERTICAL, false)
        mRecyclerView.setLayoutManager(manager)
        mRecyclerView.addItemDecoration(GridSpacingItemDecoration(4,
                ScreenUtils.dip2px(this, 8f), false))
        mAdapter = GridImageAdapter(context, onAddPicClickListener)
        if (savedInstanceState != null && savedInstanceState.getParcelableArrayList<Parcelable>("selectorList") != null) {
            mAdapter!!.setList(savedInstanceState.getParcelableArrayList("selectorList"))
        }
        // Test Arabic changes
//        List<LocalMedia> list = new ArrayList<>();
//        LocalMedia m = new LocalMedia();
//        m.setPath("https://wx1.sinaimg.cn/mw690/006e0i7xly1gaxqq5m7t8j31311g2ao6.jpg");
//        LocalMedia m1 = new LocalMedia();
//        m1.setPath("https://ww1.sinaimg.cn/bmiddle/bcd10523ly1g96mg4sfhag20c806wu0x.gif");
//        list.add(m);
//        list.add(m1);
//        mAdapter.setList(list);
        mAdapter!!.setSelectMax(maxSelectNum)
        mRecyclerView.setAdapter(mAdapter)
        mAdapter!!.setOnItemClickListener { v: View?, position: Int ->
            val selectList = mAdapter!!.data
            if (selectList.size > 0) {
                val media = selectList[position]
                val mimeType = media.mimeType
                val mediaType = PictureMimeType.getMimeType(mimeType)
                when (mediaType) {
                    PictureConfig.TYPE_VIDEO ->                         // 预览视频
                        PictureSelector.create(this@MainActivity)
                                .themeStyle(R.style.picture_default_style)
                                .externalPictureVideo(media.path)
                    PictureConfig.TYPE_AUDIO ->                         // 预览音频
                        PictureSelector.create(this@MainActivity)
                                .externalPictureAudio(if (PictureMimeType.isContent(media.path)) media.androidQToPath else media.path)
                    else ->                         // 预览图片 可自定长按保存路径
//                        PictureWindowAnimationStyle animationStyle = new PictureWindowAnimationStyle();
//                        animationStyle.activityPreviewEnterAnimation = R.anim.picture_anim_up_in;
//                        animationStyle.activityPreviewExitAnimation = R.anim.picture_anim_down_out;
                        PictureSelector.create(this@MainActivity)
                                .themeStyle(R.style.picture_default_style) // xml设置主题
                                //.setPictureWindowAnimationStyle(animationStyle)// 自定义页面启动动画
                                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) // 设置相册Activity方向，不设置默认使用系统
                                .isNotPreviewDownload(true) // 预览图片长按是否可以下载
                                //.bindCustomPlayVideoCallback(callback)// 自定义播放回调控制，用户可以使用自己的视频播放界面
                                .loadImageEngine(GlideEngine.createGlideEngine()) // 外部传入图片加载引擎，必传项
                                .openExternalPreview(position, selectList)
                }
            }
        }
        mAdapter!!.setItemLongClickListener { holder: RecyclerView.ViewHolder, position: Int, v: View? ->
            //如果item不是最后一个，则执行拖拽
            needScaleBig = true
            needScaleSmall = true
            val size = mAdapter!!.data.size
            if (size != maxSelectNum) {
                mItemTouchHelper!!.startDrag(holder)
                return@setItemLongClickListener
            }
            if (holder.layoutPosition != size - 1) {
                mItemTouchHelper!!.startDrag(holder)
            }
        }
        mDragListener = object : DragListener {
            override fun deleteState(isDelete: Boolean) {
                if (isDelete) {
                    tvDeleteText.setText(getString(R.string.app_let_go_drag_delete))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        tvDeleteText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_let_go_delete, 0, 0)
                    }
                } else {
                    tvDeleteText.setText(getString(R.string.app_drag_delete))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        tvDeleteText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.picture_icon_delete, 0, 0)
                    }
                }
            }

            override fun dragState(isStart: Boolean) {
                val visibility = tvDeleteText.getVisibility()
                if (isStart) {
                    if (visibility == View.GONE) {
                        tvDeleteText.animate().alpha(1f).setDuration(300).interpolator = AccelerateInterpolator()
                        tvDeleteText.setVisibility(View.VISIBLE)
                    }
                } else {
                    if (visibility == View.VISIBLE) {
                        tvDeleteText.animate().alpha(0f).setDuration(300).interpolator = AccelerateInterpolator()
                        tvDeleteText.setVisibility(View.GONE)
                    }
                }
            }
        }
        mItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val itemViewType = viewHolder.itemViewType
                if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                    viewHolder.itemView.alpha = 0.7f
                }
                return makeMovementFlags(ItemTouchHelper.DOWN or ItemTouchHelper.UP
                        or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //得到item原来的position
                try {
                    val fromPosition = viewHolder.adapterPosition
                    //得到目标position
                    val toPosition = target.adapterPosition
                    val itemViewType = target.itemViewType
                    if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                        if (fromPosition < toPosition) {
                            for (i in fromPosition until toPosition) {
                                Collections.swap(mAdapter!!.data, i, i + 1)
                            }
                        } else {
                            for (i in fromPosition downTo toPosition + 1) {
                                Collections.swap(mAdapter!!.data, i, i - 1)
                            }
                        }
                        mAdapter!!.notifyItemMoved(fromPosition, toPosition)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView,
                                     viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemViewType = viewHolder.itemViewType
                if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                    if (null == mDragListener) {
                        return
                    }
                    if (needScaleBig) {
                        //如果需要执行放大动画
                        viewHolder.itemView.animate().scaleXBy(0.1f).scaleYBy(0.1f).duration = 100
                        //执行完成放大动画,标记改掉
                        needScaleBig = false
                        //默认不需要执行缩小动画，当执行完成放大 并且松手后才允许执行
                        needScaleSmall = false
                    }
                    val sh = recyclerView.height + tvDeleteText.getHeight()
                    val ry = tvDeleteText.getTop() - sh
                    if (dY >= ry) {
                        //拖到删除处
                        (mDragListener as DragListener).deleteState(true)
                        if (isUpward) {
                            //在删除处放手，则删除item
                            viewHolder.itemView.visibility = View.INVISIBLE
                            mAdapter!!.delete(viewHolder.adapterPosition)
                            resetState()
                            return
                        }
                    } else { //没有到删除处
                        if (View.INVISIBLE == viewHolder.itemView.visibility) {
                            //如果viewHolder不可见，则表示用户放手，重置删除区域状态
                            (mDragListener as DragListener).dragState(false)
                        }
                        if (needScaleSmall) { //需要松手后才能执行
                            viewHolder.itemView.animate().scaleXBy(1f).scaleYBy(1f).duration = 100
                        }
                        (mDragListener as DragListener).deleteState(false)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                val itemViewType = viewHolder?.itemViewType ?: GridImageAdapter.TYPE_CAMERA
                if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                    if (ItemTouchHelper.ACTION_STATE_DRAG == actionState && mDragListener != null) {
                        (mDragListener as DragListener).dragState(true)
                    }
                    super.onSelectedChanged(viewHolder, actionState)
                }
            }

            override fun getAnimationDuration(recyclerView: RecyclerView, animationType: Int, animateDx: Float, animateDy: Float): Long {
                needScaleSmall = true
                isUpward = true
                return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                val itemViewType = viewHolder.itemViewType
                if (itemViewType != GridImageAdapter.TYPE_CAMERA) {
                    viewHolder.itemView.alpha = 1.0f
                    super.clearView(recyclerView, viewHolder)
                    mAdapter!!.notifyDataSetChanged()
                    resetState()
                }
            }
        })

        // 绑定拖拽事件
        mItemTouchHelper!!.attachToRecyclerView(mRecyclerView)

        // 注册外部预览图片删除按钮回调
        BroadcastManager.getInstance(context).registerReceiver(broadcastReceiver,
                BroadcastAction.ACTION_DELETE_PREVIEW_POSITION)
    }

    /**
     * 重置
     */
    private fun resetState() {
        if (mDragListener != null) {
            mDragListener!!.deleteState(false)
            mDragListener!!.dragState(false)
        }
        isUpward = false
    }

    /**
     * 清空缓存包括裁剪、压缩、AndroidQToPath所生成的文件，注意调用时机必须是处理完本身的业务逻辑后调用；非强制性
     */
    private fun clearCache() {
        // 清空图片缓存，包括裁剪、压缩后的图片 注意:必须要在上传完成后调用 必须要获取权限
        if (PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //PictureFileUtils.deleteCacheDirFile(this, PictureMimeType.ofImage());
            PictureFileUtils.deleteAllCacheDirFile(context)
        } else {
            PermissionChecker.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE)
        }
    }

    private val onAddPicClickListener = onAddPicClickListener { //第一种方式可通过自定义监听器的方式拿到选择的图片，第二种方式可通过官方的 onActivityResult 的方式拿到选择的图片
//            InsGallery.openGallery(MainActivity.this, GlideEngine.createGlideEngine(), GlideCacheEngine.createCacheEngine(), mAdapter.getData(), new OnResultCallbackListenerImpl(mAdapter));
        InsGallery.openGallery(this@MainActivity, GlideEngine.createGlideEngine(), GlideCacheEngine.createCacheEngine(), mAdapter!!.data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    // 图片选择结果回调
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    // 例如 LocalMedia 里面返回五种path
                    // 1.media.getPath(); 原图path
                    // 2.media.getCutPath();裁剪后path，需判断media.isCut();切勿直接使用
                    // 3.media.getCompressPath();压缩后path，需判断media.isCompressed();切勿直接使用
                    // 4.media.getOriginalPath()); media.isOriginal());为true时此字段才有值
                    // 5.media.getAndroidQToPath();Android Q版本特有返回的字段，但如果开启了压缩或裁剪还是取裁剪或压缩路径；注意：.isAndroidQTransform 为false 此字段将返回空
                    // 如果同时开启裁剪和压缩，则取压缩路径为准因为是先裁剪后压缩
                    for (media in selectList) {
                        Log.i(TAG, "是否压缩:" + media.isCompressed)
                        Log.i(TAG, "压缩:" + media.compressPath)
                        Log.i(TAG, "原图:" + media.path)
                        Log.i(TAG, "是否裁剪:" + media.isCut)
                        Log.i(TAG, "裁剪:" + media.cutPath)
                        Log.i(TAG, "是否开启原图:" + media.isOriginal)
                        Log.i(TAG, "原图路径:" + media.originalPath)
                        Log.i(TAG, "Android Q 特有Path:" + media.androidQToPath)
                        Log.i(TAG, "Size: " + media.size)
                    }
                    mAdapter!!.setList(selectList)
                    mAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.left_back -> finish()
            R.id.minus -> {
                if (maxSelectNum > 1) {
                    maxSelectNum--
                }
                tv_select_num!!.text = maxSelectNum.toString() + ""
                mAdapter!!.setSelectMax(maxSelectNum)
            }
            R.id.plus -> {
                maxSelectNum++
                tv_select_num!!.text = maxSelectNum.toString() + ""
                mAdapter!!.setSelectMax(maxSelectNum)
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup, @IdRes checkedId: Int) {
        when (checkedId) {
            R.id.rb_default_style -> InsGallery.setCurrentTheme(InsGallery.THEME_STYLE_DEFAULT)
            R.id.rb_dark_style -> InsGallery.setCurrentTheme(InsGallery.THEME_STYLE_DARK)
            R.id.rb_dark_blue_style -> InsGallery.setCurrentTheme(InsGallery.THEME_STYLE_DARK_BLUE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE ->                 // 存储权限
            {
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        PictureFileUtils.deleteCacheDirFile(context, PictureMimeType.ofImage())
                    } else {
                        Toast.makeText(this@MainActivity,
                                getString(R.string.picture_jurisdiction), Toast.LENGTH_SHORT).show()
                    }
                    i++
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mAdapter != null && mAdapter!!.data != null && mAdapter!!.data.size > 0) {
            outState.putParcelableArrayList("selectorList",
                    mAdapter!!.data as ArrayList<out Parcelable?>)
        }
    }

    private val broadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val extras: Bundle?
            when (action) {
                BroadcastAction.ACTION_DELETE_PREVIEW_POSITION -> {
                    // 外部预览删除按钮回调
                    extras = intent.extras
                    val position = extras!!.getInt(PictureConfig.EXTRA_PREVIEW_DELETE_POSITION)
                    ToastUtils.s(context, "delete image index:$position")
                    if (position < mAdapter!!.data.size) {
                        mAdapter!!.remove(position)
                        mAdapter!!.notifyItemRemoved(position)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            BroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver,
                    BroadcastAction.ACTION_DELETE_PREVIEW_POSITION)
        }
    }

    val context: Context
        get() = this

    private class OnResultCallbackListenerImpl(adapter: GridImageAdapter) : OnResultCallbackListener<LocalMedia?> {
        private val mAdapter: WeakReference<GridImageAdapter>


        override fun onCancel() {
            Log.i(TAG, "PictureSelector Cancel")
        }

        init {
            mAdapter = WeakReference(adapter)
        }

        override fun onResult(result: MutableList<LocalMedia?>?) {
            for (media in result!!) {
                Log.i(TAG, "是否压缩:" + media?.isCompressed)
                Log.i(TAG, "压缩:" + media?.compressPath)
                Log.i(TAG, "原图:" + media?.path)
                Log.i(TAG, "是否裁剪:" + media?.isCut)
                Log.i(TAG, "裁剪:" + media?.cutPath)
                Log.i(TAG, "是否开启原图:" + media?.isOriginal)
                Log.i(TAG, "原图路径:" + media?.originalPath)
                Log.i(TAG, "Android Q 特有Path:" + media?.androidQToPath)
                Log.i(TAG, "Size: " + media?.size)
            }
            val adapter = mAdapter.get()
            if (adapter != null) {
                adapter.setList(result)
                adapter.notifyDataSetChanged()
            }        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}