package com.luck.picture.lib.tools

import android.content.Context
import com.luck.picture.lib.config.PictureMimeType
import java.util.regex.Pattern

/**
 * @author：luck
 * @data：2017/5/25 19:12
 * @描述: String Utils
 */
object StringUtils {
    fun tempTextFont(tv: TextView, mimeType: Int) {
        val text: String = tv.getText().toString().trim { it <= ' ' }
        val str: String = if (mimeType == PictureMimeType.ofAudio()) tv.getContext().getString(R.string.picture_empty_audio_title) else tv.getContext().getString(R.string.picture_empty_title)
        val sumText = str + text
        val placeSpan: Spannable = SpannableString(sumText)
        placeSpan.setSpan(RelativeSizeSpan(0.8f), str.length, sumText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.setText(placeSpan)
    }

    /**
     * 匹配数值
     *
     * @param str
     * @return
     */
    fun stringToInt(str: String?): Int {
        val pattern = Pattern.compile("^[-\\+]?[\\d]+$")
        return if (pattern.matcher(str).matches()) Integer.valueOf(str!!) else 0
    }

    /**
     * 根据类型获取相应的Toast文案
     *
     * @param context
     * @param mimeType
     * @param maxSelectNum
     * @return
     */
    @SuppressLint("StringFormatMatches")
    fun getMsg(context: Context, mimeType: String?, maxSelectNum: Int): String {
        return if (PictureMimeType.isHasVideo(mimeType)) {
            context.getString(R.string.picture_message_video_max_num, maxSelectNum)
        } else if (PictureMimeType.isHasAudio(mimeType)) {
            context.getString(R.string.picture_message_audio_max_num, maxSelectNum)
        } else {
            context.getString(R.string.picture_message_max_num, maxSelectNum)
        }
    }

    /**
     * 重命名相册拍照
     *
     * @param fileName
     * @return
     */
    fun rename(fileName: String): String {
        val temp = fileName.substring(0, fileName.lastIndexOf("."))
        val suffix = fileName.substring(fileName.lastIndexOf("."))
        return temp + "_" + DateUtils.getCreateFileName() + suffix
    }

    /**
     * 重命名后缀
     *
     * @param fileName
     * @return
     */
    fun renameSuffix(fileName: String, suffix: String): String {
        val temp = fileName.substring(0, fileName.lastIndexOf("."))
        return temp + suffix
    }

    /**
     * getEncryptionValue
     *
     * @param url
     * @param width
     * @param height
     * @return
     */
    fun getEncryptionValue(url: String?, width: Int, height: Int): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(url).append("_").append(width).append("x").append(height)
        return ValueOf.toString(Math.abs(hash(stringBuilder.hashCode())))
    }

    /**
     * hash
     *
     * @param key
     * @return
     */
    fun hash(key: Any?): Int {
        var h: Int
        return if (key == null) 0 else key.hashCode().also { h = it } xor (h ushr 16)
    }
}