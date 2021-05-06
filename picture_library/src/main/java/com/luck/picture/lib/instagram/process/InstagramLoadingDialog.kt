package com.luck.picture.lib.instagram.process

import android.app.Dialog
import android.content.*
import android.os.Bundle
import com.luck.picture.lib.R

class InstagramLoadingDialog(context: Context?) : Dialog(context!!, R.style.Picture_Theme_AlertDialog) {
    private var mContentView: InstagramLoadingView? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        mContentView = InstagramLoadingView(context)
        setContentView(mContentView)
    }

    fun updateProgress(progress: Double) {
        if (mContentView != null) {
            mContentView.updateProgress(progress)
        }
    }

    init {
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        val window = window
        window!!.setWindowAnimations(R.style.PictureThemeDialogWindowStyle)
    }
}