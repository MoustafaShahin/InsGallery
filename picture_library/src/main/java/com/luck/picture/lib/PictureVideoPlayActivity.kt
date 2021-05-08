package com.luck.picture.lib

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.tools.SdkVersionUtils
import java.util.*

/**
 * @author：luck
 * @data：2017/8/28 下午11:00
 * @描述: 视频播放类
 */
class PictureVideoPlayActivity : PictureBaseActivity(), MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, View.OnClickListener {
    private  var videoPath: String? = null
    private  var ibLeftBack: ImageButton? = null
    private  var mMediaController: MediaController? = null
    private  var mVideoView: VideoView? = null
    private  var tvConfirm: TextView? = null
    private  var iv_play: ImageView? = null
    private  var mPositionWhenPaused = -1
     override val isImmersive: Boolean
        get() = false
    override val isRequestedOrientation: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        super.onCreate(savedInstanceState)
    }

    override val resourceId: Int
        get() = R.layout.picture_activity_video_play

    override fun initPictureSelectorStyle() {
        if (config.style != null) {
            if (config.style.pictureLeftBackIcon !== 0) {
                ibLeftBack?.setImageResource(config.style.pictureLeftBackIcon)
            }
        }
    }

    protected override fun initWidgets() {
        super.initWidgets()
        videoPath = getIntent().getStringExtra(PictureConfig.EXTRA_VIDEO_PATH)
        val isExternalPreview: Boolean = getIntent().getBooleanExtra(PictureConfig.EXTRA_PREVIEW_VIDEO, false)
        if (TextUtils.isEmpty(videoPath)) {
            val media: LocalMedia = getIntent().getParcelableExtra(PictureConfig.EXTRA_MEDIA_KEY)
            if (media == null || TextUtils.isEmpty(media.getPath())) {
                finish()
                return
            }
            videoPath = media.getPath()
        }
        if (TextUtils.isEmpty(videoPath)) {
            closeActivity()
            return
        }
        ibLeftBack = findViewById(R.id.pictureLeftBack)
        mVideoView = findViewById(R.id.video_view)
        tvConfirm = findViewById(R.id.tv_confirm)
        mVideoView.setBackgroundColor(Color.BLACK)
        iv_play = findViewById(R.id.iv_play)
        mMediaController = MediaController(this)
        mVideoView.setOnCompletionListener(this)
        mVideoView.setOnPreparedListener(this)
        mVideoView.setMediaController(mMediaController)
        ibLeftBack.setOnClickListener(this)
        iv_play!!.setOnClickListener(this)
        tvConfirm.setOnClickListener(this)
        tvConfirm.setVisibility(if ((config.selectionMode
                        === PictureConfig.SINGLE) && config.enPreviewVideo && !isExternalPreview) View.VISIBLE else View.GONE)
    }

    override fun onStart() {
        // Play Video
        if (SdkVersionUtils.checkedAndroid_Q() && videoPath?.let { PictureMimeType.isContent(it) } == true) {
            mVideoView.setVideoURI(Uri.parse(videoPath))
        } else {
            mVideoView.setVideoPath(videoPath)
        }
        mVideoView.start()
        super.onStart()
    }

    override fun onPause() {
        // Stop video when the activity is pause.
        mPositionWhenPaused = mVideoView?.getCurrentPosition()!!
        mVideoView?.stopPlayback()
        super.onPause()
    }

    protected override fun onDestroy() {
        mMediaController = null
        mVideoView = null
        iv_play = null
        super.onDestroy()
    }

    override fun onResume() {
        // Resume video player
        if (mPositionWhenPaused >= 0) {
            mVideoView.seekTo(mPositionWhenPaused)
            mPositionWhenPaused = -1
        }
        super.onResume()
    }

    override fun onError(player: MediaPlayer, arg1: Int, arg2: Int): Boolean {
        return false
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (null != iv_play) {
            iv_play!!.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.pictureLeftBack) {
            onBackPressed()
        } else if (id == R.id.iv_play) {
            mVideoView.start()
            iv_play!!.visibility = View.INVISIBLE
        } else if (id == R.id.tv_confirm) {
            val result: MutableList<LocalMedia?> = ArrayList<LocalMedia?>()
            result.add(getIntent().getParcelableExtra(PictureConfig.EXTRA_MEDIA_KEY))
            setResult(RESULT_OK, Intent()
                    .putParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST,
                            result as ArrayList<out Parcelable?>))
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (config.windowAnimationStyle != null
                && config.windowAnimationStyle.activityPreviewExitAnimation !== 0) {
            finish()
            overridePendingTransition(0, if (config.windowAnimationStyle != null
                    && config.windowAnimationStyle.activityPreviewExitAnimation !== 0) config.windowAnimationStyle.activityPreviewExitAnimation else R.anim.picture_anim_exit)
        } else {
            closeActivity()
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.setOnInfoListener(MediaPlayer.OnInfoListener setOnInfoListener@{ mp1: MediaPlayer?, what: Int, extra: Int ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // video started
                mVideoView?.setBackgroundColor(Color.TRANSPARENT)
                return@setOnInfoListener true
            }
            false
        })
    }
}