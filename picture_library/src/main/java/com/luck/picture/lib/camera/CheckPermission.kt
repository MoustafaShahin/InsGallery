package com.luck.picture.lib.camera

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/6/8
 * 描    述：
 * =====================================
 */
object CheckPermission {
    const val STATE_RECORDING = -1
    const val STATE_NO_PERMISSION = -2
    const val STATE_SUCCESS = 1//检测是否可以获取录音结果//6.0以下机型都会返回此状态，故使用时需要判断bulid版本
    //检测是否在录音中
//检测是否可以进入初始化状态
    /**
     * 用于检测是否具有录音权限
     *
     * @return
     */
    val recordState: Int
        get() {
            val minBuffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            var audioRecord: AudioRecord? = AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer * 100)
            val point = ShortArray(minBuffer)
            var readSize = 0
            try {
                audioRecord!!.startRecording() //检测是否可以进入初始化状态
            } catch (e: Exception) {
                if (audioRecord != null) {
                    audioRecord.release()
                    audioRecord = null
                }
                return STATE_NO_PERMISSION
            }
            return if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                //6.0以下机型都会返回此状态，故使用时需要判断bulid版本
                //检测是否在录音中
                if (audioRecord != null) {
                    audioRecord.stop()
                    audioRecord.release()
                    audioRecord = null
                }
                STATE_RECORDING
            } else {
                //检测是否可以获取录音结果
                readSize = audioRecord.read(point, 0, point.size)
                if (readSize <= 0) {
                    if (audioRecord != null) {
                        audioRecord.stop()
                        audioRecord.release()
                        audioRecord = null
                    }
                    STATE_NO_PERMISSION
                } else {
                    if (audioRecord != null) {
                        audioRecord.stop()
                        audioRecord.release()
                        audioRecord = null
                    }
                    STATE_SUCCESS
                }
            }
        }
}