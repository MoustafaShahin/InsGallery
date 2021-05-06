package com.luck.pictureselector.listener

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author：luck
 * @date：2020-01-13 17:58
 * @describe：长按事件
 */
interface OnItemLongClickListener {
    fun onItemLongClick(holder: RecyclerView.ViewHolder?, position: Int, v: View?)
}