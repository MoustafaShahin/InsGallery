package com.luck.picture.lib.instagram

import android.R
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.StateListDrawable
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils

/**
 * ================================================
 * Created by JessYan on 2020/4/1 17:59
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramUtils {
    companion object {
        fun createCircleDrawable(size: Int, color: Int): Drawable {
            val ovalShape = OvalShape()
            ovalShape.resize(size.toFloat(), size.toFloat())
            val defaultDrawable = ShapeDrawable(ovalShape)
            defaultDrawable.getPaint().setColor(color)
            return defaultDrawable
        }

        fun createCircleDrawableWithIcon(context: Context?, size: Int, iconRes: Int): CombinedDrawable {
            return createCircleDrawableWithIcon(context, size, iconRes, 0)
        }

        fun createCircleDrawableWithIcon(context: Context, size: Int, iconRes: Int, stroke: Int): CombinedDrawable {
            val drawable: Drawable?
            drawable = if (iconRes != 0) {
                context.resources.getDrawable(iconRes).mutate()
            } else {
                null
            }
            return createCircleDrawableWithIcon(context, size, drawable, stroke)
        }

        fun createCircleDrawableWithIcon(context: Context?, size: Int, drawable: Drawable?, stroke: Int): CombinedDrawable {
            val ovalShape = OvalShape()
            ovalShape.resize(size.toFloat(), size.toFloat())
            val defaultDrawable = ShapeDrawable(ovalShape)
            val paint: Paint = defaultDrawable.getPaint()
            paint.color = -0x1
            if (stroke == 1) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = ScreenUtils.dip2px(context!!, 2).toFloat()
            } else if (stroke == 2) {
                paint.alpha = 0
            }
            val combinedDrawable = CombinedDrawable(defaultDrawable, drawable)
            combinedDrawable.setCustomSize(size, size)
            return combinedDrawable
        }

        fun createRoundRectDrawable(rad: Int, defaultColor: Int): Drawable {
            val defaultDrawable = ShapeDrawable(RoundRectShape(floatArrayOf(rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat(), rad.toFloat()), null, null))
            defaultDrawable.getPaint().setColor(defaultColor)
            return defaultDrawable
        }

        fun setCombinedDrawableColor(combinedDrawable: Drawable, color: Int, isIcon: Boolean) {
            if (combinedDrawable !is CombinedDrawable) {
                return
            }
            val drawable: Drawable
            drawable = if (isIcon) {
                (combinedDrawable as CombinedDrawable).getIcon()
            } else {
                (combinedDrawable as CombinedDrawable).getBackground()
            }
            if (drawable is ColorDrawable) {
                (drawable as ColorDrawable).setColor(color)
            } else {
                drawable.setColorFilter(PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY))
            }
        }

        fun createSimpleSelectorCircleDrawable(size: Int, defaultColor: Int, pressedColor: Int): Drawable {
            val ovalShape = OvalShape()
            ovalShape.resize(size.toFloat(), size.toFloat())
            val defaultDrawable = ShapeDrawable(ovalShape)
            defaultDrawable.getPaint().setColor(defaultColor)
            val pressedDrawable = ShapeDrawable(ovalShape)
            return if (Build.VERSION.SDK_INT >= 21) {
                pressedDrawable.getPaint().setColor(-0x1)
                val colorStateList = ColorStateList(arrayOf<IntArray>(StateSet.WILD_CARD), intArrayOf(pressedColor))
                RippleDrawable(colorStateList, defaultDrawable, pressedDrawable)
            } else {
                pressedDrawable.getPaint().setColor(pressedColor)
                val stateListDrawable = StateListDrawable()
                stateListDrawable.addState(intArrayOf(R.attr.state_pressed), pressedDrawable)
                stateListDrawable.addState(intArrayOf(R.attr.state_focused), pressedDrawable)
                stateListDrawable.addState(StateSet.WILD_CARD, defaultDrawable)
                stateListDrawable
            }
        }

        fun setViewVisibility(view: View?, visibility: Int) {
            if (view != null) {
                view.visibility = visibility
            }
        }
    }

    init {
        throw IllegalStateException("you can't instantiate me!")
    }
}