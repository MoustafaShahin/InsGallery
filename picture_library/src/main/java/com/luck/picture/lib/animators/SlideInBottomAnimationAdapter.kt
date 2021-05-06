package com.luck.picture.lib.animators

import android.animation.*
import android.view.*
import androidx.recyclerview.widget.RecyclerView

/**
 * @author：luck
 * @date：2020-04-18 14:19
 * @describe：SlideInBottomAnimationAdapter
 */
class SlideInBottomAnimationAdapter(adapter: RecyclerView.Adapter<*>) : BaseAnimationAdapter(adapter) {
    protected fun getAnimators(view: View): Array<Animator> {
        return arrayOf(
                ObjectAnimator.ofFloat(view, "translationY", view.measuredHeight.toFloat(), 0f)
        )
    }
}