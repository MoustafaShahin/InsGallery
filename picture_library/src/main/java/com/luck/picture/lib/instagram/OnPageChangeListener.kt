package com.luck.picture.lib.instagram

/**
 * ================================================
 * Created by JessYan on 2020/4/15 18:07
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
interface OnPageChangeListener {
    fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
    fun onPageSelected(position: Int)
}