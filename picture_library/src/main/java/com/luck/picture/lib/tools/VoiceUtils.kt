package com.luck.picture.lib.tools

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import com.luck.picture.lib.R

/**
 * @author：luck
 * @data：2017/5/25 19:12
 * @描述: voice utils
 */
class VoiceUtils {
    private var soundPool: SoundPool? = null

    /**
     * 创建某个声音对应的音频ID
     */
    private var soundID = 0
    fun init(context: Context) {
        initPool(context)
    }

    private fun initPool(context: Context) {
        if (soundPool == null) {
            soundPool = SoundPool(1, AudioManager.STREAM_ALARM, 0)
            soundID = soundPool!!.load(context.applicationContext, R.raw.picture_music, 1)
        }
    }

    /**
     * 播放音频
     */
    fun play() {
        if (soundPool != null) {
            soundPool!!.play(soundID, 0.1f, 0.5f, 0, 1, 1f)
        }
    }

    /**
     * 释放资源
     */
    fun releaseSoundPool() {
        try {
            if (soundPool != null) {
                soundPool!!.release()
                soundPool = null
            }
            instance = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private var instance: VoiceUtils? = null
        fun getInstance(): VoiceUtils? {
            if (instance == null) {
                synchronized(VoiceUtils::class.java) {
                    if (instance == null) {
                        instance = VoiceUtils()
                    }
                }
            }
            return instance
        }
    }
}