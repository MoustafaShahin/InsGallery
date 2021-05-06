package com.luck.picture.lib.dialog

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.luck.picture.lib.R

class PictureCustomDialog : Dialog {
    constructor(context: Context?, layout: Int) : super(context!!, R.style.Picture_Theme_Dialog) {
        setContentView(layout)
        initParams()
    }

    constructor(context: Context?, layout: View?) : super(context!!, R.style.Picture_Theme_Dialog) {
        setContentView(layout!!)
        initParams()
    }

    private fun initParams() {
        val window = window
        val params = window!!.attributes
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.CENTER
        window.attributes = params
    }
}