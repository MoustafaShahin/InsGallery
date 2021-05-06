package com.luck.picture.lib.instagram.process

/**
 * ================================================
 * Created by JessYan on 2020/6/18 16:52
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
interface LifecycleCallBack {
    fun onStart(activity: InstagramMediaProcessActivity?)
    fun onResume(activity: InstagramMediaProcessActivity?)
    fun onPause(activity: InstagramMediaProcessActivity?)
    fun onDestroy(activity: InstagramMediaProcessActivity?)
}