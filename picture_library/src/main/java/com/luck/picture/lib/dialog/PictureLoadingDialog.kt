package com.luck.picture.lib.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.luck.picture.lib.R

class PictureLoadingDialog(context: Context?) : Dialog(context!!, R.style.Picture_Theme_AlertDialog) {
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.picture_alert_dialog)
    }

    init {
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        val window = window
        window!!.setWindowAnimations(R.style.PictureThemeDialogWindowStyle)
    }
}