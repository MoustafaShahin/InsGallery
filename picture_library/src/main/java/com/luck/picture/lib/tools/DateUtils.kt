package com.luck.picture.lib.tools

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author：luck
 * @date：2017-5-25 23:30
 * @describe：DateUtils
 */
object DateUtils {
    private val sf = SimpleDateFormat("yyyyMMdd_HHmmssSS")

    /**
     * 判断两个时间戳相差多少秒
     *
     * @param d
     * @return
     */
    fun dateDiffer(d: Long): Int {
        return try {
            val l1: Long = ValueOf.toLong(System.currentTimeMillis().toString().substring(0, 10))
            val interval = l1 - d
            Math.abs(interval).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * 时间戳转换成时间格式
     *
     * @param duration
     * @return
     */
    fun formatDurationTime(duration: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))
    }

    /**
     * 根据时间戳创建文件名
     *
     * @param prefix 前缀名
     * @return
     */
    fun getCreateFileName(prefix: String): String {
        val millis = System.currentTimeMillis()
        return prefix + sf.format(millis)
    }

    /**
     * 根据时间戳创建文件名
     *
     * @return
     */
    fun getCreateFileName(): String {
        val millis = System.currentTimeMillis()
        return sf.format(millis)
    }

    /**
     * 计算两个时间间隔
     *
     * @param sTime
     * @param eTime
     * @return
     */
    fun cdTime(sTime: Long, eTime: Long): String {
        val diff = eTime - sTime
        return if (diff > 1000) diff / 1000.toString() + "秒" else diff.toString() + "毫秒"
    }
}