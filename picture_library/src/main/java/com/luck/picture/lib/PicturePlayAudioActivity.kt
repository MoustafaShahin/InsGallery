package com.luck.picture.lib

import android.os.Handler
import android.view.View
import com.luck.picture.lib.config.PictureConfig

/**
 * # No longer maintain audio related functions,
 * but can continue to use but there will be phone compatibility issues.
 *
 *
 * 不再维护音频相关功能，但可以继续使用但会有机型兼容性问题
 */
@Deprecated("")
class PicturePlayAudioActivity : PictureBaseActivity(), View.OnClickListener {
    private var audio_path: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var musicSeekBar: SeekBar? = null
    private var isPlayAudio = false
    private var tv_PlayPause: TextView? = null
    private var tv_Stop: TextView? = null
    private var tv_Quit: TextView? = null
    private var tv_musicStatus: TextView? = null
    private var tv_musicTotal: TextView? = null
    private var tv_musicTime: TextView? = null
    protected override fun onCreate(savedInstanceState: Bundle?) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
    }

    override val resourceId: Int
        get() = R.layout.picture_play_audio

    protected override fun initWidgets() {
        super.initWidgets()
        audio_path = getIntent().getStringExtra(PictureConfig.EXTRA_AUDIO_PATH)
        tv_musicStatus = findViewById(R.id.tv_musicStatus)
        tv_musicTime = findViewById(R.id.tv_musicTime)
        musicSeekBar = findViewById(R.id.musicSeekBar)
        tv_musicTotal = findViewById(R.id.tv_musicTotal)
        tv_PlayPause = findViewById(R.id.tv_PlayPause)
        tv_Stop = findViewById(R.id.tv_Stop)
        tv_Quit = findViewById(R.id.tv_Quit)
        handler!!.postDelayed({ initPlayer(audio_path) }, 30)
        tv_PlayPause.setOnClickListener(this)
        tv_Stop.setOnClickListener(this)
        tv_Quit.setOnClickListener(this)
        musicSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    //  通过 Handler 更新 UI 上的组件状态
    var handler: Handler? = Handler()
    var runnable: Runnable = object : Runnable {
        override fun run() {
            try {
                if (mediaPlayer != null) {
                    tv_musicTime.setText(DateUtils.formatDurationTime(mediaPlayer.getCurrentPosition()))
                    musicSeekBar.setProgress(mediaPlayer.getCurrentPosition())
                    musicSeekBar.setMax(mediaPlayer.getDuration())
                    tv_musicTotal.setText(DateUtils.formatDurationTime(mediaPlayer.getDuration()))
                    handler!!.postDelayed(this, 200)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 初始化音频播放组件
     *
     * @param path
     */
    private fun initPlayer(path: String?) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            mediaPlayer.setLooping(true)
            playAudio()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View) {
        val i = v.id
        if (i == R.id.tv_PlayPause) {
            playAudio()
        }
        if (i == R.id.tv_Stop) {
            tv_musicStatus.setText(getString(R.string.picture_stop_audio))
            tv_PlayPause.setText(getString(R.string.picture_play_audio))
            stop(audio_path)
        }
        if (i == R.id.tv_Quit) {
            handler!!.removeCallbacks(runnable)
            Handler().postDelayed({ stop(audio_path) }, 30)
            try {
                closeActivity()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 播放音频
     */
    private fun playAudio() {
        if (mediaPlayer != null) {
            musicSeekBar.setProgress(mediaPlayer.getCurrentPosition())
            musicSeekBar.setMax(mediaPlayer.getDuration())
        }
        val ppStr: String = tv_PlayPause.getText().toString()
        if (ppStr == getString(R.string.picture_play_audio)) {
            tv_PlayPause.setText(getString(R.string.picture_pause_audio))
            tv_musicStatus.setText(getString(R.string.picture_play_audio))
            playOrPause()
        } else {
            tv_PlayPause.setText(getString(R.string.picture_play_audio))
            tv_musicStatus.setText(getString(R.string.picture_pause_audio))
            playOrPause()
        }
        if (!isPlayAudio) {
            handler!!.post(runnable)
            isPlayAudio = true
        }
    }

    /**
     * 停止播放
     *
     * @param path
     */
    fun stop(path: String?) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepare()
                mediaPlayer.seekTo(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 暂停播放
     */
    fun playOrPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause()
                } else {
                    mediaPlayer.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onBackPressed() {
        super.onBackPressed()
        closeActivity()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null && handler != null) {
            handler!!.removeCallbacks(runnable)
            mediaPlayer.release()
            mediaPlayer = null
        }
    }
}