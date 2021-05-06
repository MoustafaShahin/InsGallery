package com.luck.picture.lib.instagram

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.luck.picture.lib.tools.ScreenUtils
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/4/14 11:05
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class InstagramTabLayout(context: Context, items: List<Page>, config: PictureSelectionConfig) : FrameLayout(context) {
    private val titles: MutableList<String> = ArrayList()
    private val tabViews: MutableList<View> = ArrayList()
    private val selectedIndicatorHeight: Int
    private var layoutHeight = 0
    private val selectedIndicatorPaint: Paint?
    private val defaultSelectionIndicator: GradientDrawable
    private var indicatorLeft = -1
    private var indicatorRight = -1
    private var tabWidth = 0
    private val config: PictureSelectionConfig
    private fun installTabView(context: Context, titles: List<String>) {
        for (i in titles.indices) {
            val tabView = TextView(context)
            tabView.setTextSize(15f)
            if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
                tabView.setTextColor(Color.parseColor("#9B9B9D"))
            } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
                tabView.setTextColor(Color.parseColor("#7E93A0"))
            } else {
                tabView.setTextColor(Color.parseColor("#92979F"))
            }
            tabView.setGravity(Gravity.CENTER)
            tabView.setText(titles[i])
            addView(tabView)
            tabViews.add(tabView)
        }
    }

    private fun fillTitles(items: List<Page>) {
        for (item in items) {
            if (item != null) {
                val title: String = item.getTitle(getContext())
                if (!TextUtils.isEmpty(title)) {
                    titles.add(title)
                } else {
                    throw IllegalStateException("getTitle(Context) is null!")
                }
            }
        }
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = if (layoutHeight > 0) layoutHeight else ScreenUtils.dip2px(getContext(), 44)
        if (!tabViews.isEmpty()) {
            tabWidth = width / tabViews.size
            if (indicatorLeft == -1) {
                indicatorLeft = 0
                indicatorRight = tabWidth
            }
            for (view in tabViews) {
                view.measure(MeasureSpec.makeMeasureSpec(tabWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(measuredHeight - selectedIndicatorHeight, MeasureSpec.EXACTLY))
            }
        }
        setMeasuredDimension(width, measuredHeight)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val viewTop = 0
        var viewLeft: Int
        if (!tabViews.isEmpty()) {
            for (i in tabViews.indices) {
                viewLeft = i * tabWidth
                val view = tabViews[i]
                view.layout(viewLeft, viewTop, viewLeft + view.measuredWidth, viewTop + view.measuredHeight)
            }
        }
    }

    protected override fun onDraw(canvas: Canvas) {
        if (indicatorLeft >= 0 && indicatorRight > indicatorLeft) {
            val selectedIndicator: Drawable
            selectedIndicator = DrawableCompat.wrap(defaultSelectionIndicator)
            selectedIndicator.setBounds(indicatorLeft, getMeasuredHeight() - selectedIndicatorHeight, indicatorRight, getMeasuredHeight())
            if (selectedIndicatorPaint != null) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    // Drawable doesn't implement setTint in API 21
                    selectedIndicator.setColorFilter(
                            selectedIndicatorPaint.color, PorterDuff.Mode.SRC_IN)
                } else {
                    DrawableCompat.setTint(selectedIndicator, selectedIndicatorPaint.color)
                }
            }
            selectedIndicator.draw(canvas)
        }
    }

    fun getTabSize(): Int {
        return tabViews.size
    }

    fun setIndicatorPosition(position: Int, positionOffset: Float) {
        var left = position * tabWidth
        if (positionOffset > 0) {
            val offset = positionOffset * tabWidth
            left += offset.toInt()
        }
        setIndicatorPosition(left, left + tabWidth)
    }

    fun setIndicatorPosition(left: Int) {
        setIndicatorPosition(left, left + tabWidth)
    }

    fun setIndicatorPosition(left: Int, right: Int) {
        if (left != indicatorLeft || right != indicatorRight) {
            // If the indicator's left/right has changed, invalidate
            indicatorLeft = left
            indicatorRight = right
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun selectTab(position: Int) {
        if (position < 0 || position >= tabViews.size) {
            return
        }
        for (i in tabViews.indices) {
            val tabView = tabViews[i]
            if (position == i) {
                if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
                    (tabView as TextView).setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
                    (tabView as TextView).setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_white))
                } else {
                    (tabView as TextView).setTextColor(ContextCompat.getColor(getContext(), R.color.picture_color_black))
                }
            } else {
                if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
                    (tabView as TextView).setTextColor(Color.parseColor("#9B9B9D"))
                } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
                    (tabView as TextView).setTextColor(Color.parseColor("#7E93A0"))
                } else {
                    (tabView as TextView).setTextColor(Color.parseColor("#92979F"))
                }
            }
        }
    }

    fun setLayoutHeight(layoutHeight: Int) {
        this.layoutHeight = layoutHeight
    }

    init {
        this.config = config
        fillTitles(items)
        installTabView(context, titles)
        setWillNotDraw(false)
        selectedIndicatorPaint = Paint()
        defaultSelectionIndicator = GradientDrawable()
        selectedIndicatorHeight = ScreenUtils.dip2px(context, 1)
        if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK) {
            selectedIndicatorPaint.setColor(ContextCompat.getColor(context, R.color.picture_color_white))
        } else if (config.instagramSelectionConfig.getCurrentTheme() === InsGallery.THEME_STYLE_DARK_BLUE) {
            selectedIndicatorPaint.setColor(Color.parseColor("#2FA6FF"))
        } else {
            selectedIndicatorPaint.setColor(ContextCompat.getColor(context, R.color.picture_color_black))
        }
        selectTab(0)
    }
}