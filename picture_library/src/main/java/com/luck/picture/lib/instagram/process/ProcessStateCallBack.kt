package com.luck.picture.lib.instagram.process

import android.content.Intent
import android.widget.*

/**
 * ================================================
 * Created by JessYan on 2020/6/11 17:04
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
interface ProcessStateCallBack {
    fun onBack(activity: InstagramMediaProcessActivity?)
    fun onCenterFeature(activity: InstagramMediaProcessActivity?, view: ImageView?)
    fun onProcess(activity: InstagramMediaProcessActivity?)
    fun onActivityResult(activity: InstagramMediaProcessActivity?, requestCode: Int, resultCode: Int, data: Intent?)
}