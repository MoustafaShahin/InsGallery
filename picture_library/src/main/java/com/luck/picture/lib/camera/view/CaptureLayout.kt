package com.luck.picture.lib.camera.view

import android.animation.Animator
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.luck.picture.lib.camera.listener.CaptureListener

/**
 * =====================================
 * 作    者: 陈嘉桐 445263848@qq.com
 * 版    本：1.0.4
 * 创建日期：2017/4/26
 * 描    述：集成各个控件的布局
 * =====================================
 */
class CaptureLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var captureListener //拍照按钮监听
            : CaptureListener? = null
    private var typeListener //拍照或录制后接结果按钮监听
            : TypeListener? = null
    private var leftClickListener //左边按钮监听
            : ClickListener? = null
    private var rightClickListener //右边按钮监听
            : ClickListener? = null

    fun setTypeListener(typeListener: TypeListener?) {
        this.typeListener = typeListener
    }

    fun setCaptureListener(captureListener: CaptureListener?) {
        this.captureListener = captureListener
    }

    private var btn_capture //拍照按钮
            : CaptureButton? = null
    private var btn_confirm //确认按钮
            : TypeButton? = null
    private var btn_cancel //取消按钮
            : TypeButton? = null
    private var btn_return //返回按钮
            : ReturnButton? = null
    private var iv_custom_left //左边自定义按钮
            : ImageView? = null
    private var iv_custom_right //右边自定义按钮
            : ImageView? = null
    private var txt_tip //提示文本
            : TextView? = null
    private var layout_width = 0
    private val layout_height: Int
    private val button_size: Int
    private var iconLeft = 0
    private var iconRight = 0
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(layout_width, layout_height)
    }

    fun initEvent() {
        //默认TypeButton为隐藏
        iv_custom_right!!.visibility = View.GONE
        btn_cancel!!.setVisibility(View.GONE)
        btn_confirm!!.setVisibility(View.GONE)
    }

    fun startTypeBtnAnimator() {
        //拍照录制结果后的动画
        if (iconLeft != 0) iv_custom_left!!.visibility = View.GONE else btn_return!!.setVisibility(View.GONE)
        if (iconRight != 0) iv_custom_right!!.visibility = View.GONE
        btn_capture!!.setVisibility(View.GONE)
        btn_cancel!!.setVisibility(View.VISIBLE)
        btn_confirm!!.setVisibility(View.VISIBLE)
        btn_cancel!!.setClickable(false)
        btn_confirm!!.setClickable(false)
        iv_custom_left!!.visibility = View.GONE
        val animator_cancel: ObjectAnimator = ObjectAnimator.ofFloat(btn_cancel, "translationX", layout_width / 4, 0)
        val animator_confirm: ObjectAnimator = ObjectAnimator.ofFloat(btn_confirm, "translationX", -layout_width / 4, 0)
        val set = AnimatorSet()
        set.playTogether(animator_cancel, animator_confirm)
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                btn_cancel!!.setClickable(true)
                btn_confirm!!.setClickable(true)
            }
        })
        set.setDuration(500)
        set.start()
    }

    private fun initView() {
        setWillNotDraw(false)
        //拍照按钮
        btn_capture = CaptureButton(getContext(), button_size)
        val btn_capture_param: FrameLayout.LayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        btn_capture_param.gravity = Gravity.CENTER
        btn_capture.setLayoutParams(btn_capture_param)
        btn_capture.setCaptureListener(object : CaptureListener() {
            override fun takePictures() {
                if (captureListener != null) {
                    captureListener.takePictures()
                }
                startAlphaAnimation()
            }

            override fun recordShort(time: Long) {
                if (captureListener != null) {
                    captureListener.recordShort(time)
                }
            }

            override fun recordStart() {
                if (captureListener != null) {
                    captureListener.recordStart()
                }
                startAlphaAnimation()
            }

            override fun recordEnd(time: Long) {
                if (captureListener != null) {
                    captureListener.recordEnd(time)
                }
                startTypeBtnAnimator()
            }

            override fun recordZoom(zoom: Float) {
                if (captureListener != null) {
                    captureListener.recordZoom(zoom)
                }
            }

            override fun recordError() {
                if (captureListener != null) {
                    captureListener.recordError()
                }
            }
        })

        //取消按钮
        btn_cancel = TypeButton(getContext(), TypeButton.TYPE_CANCEL, button_size)
        val btn_cancel_param: FrameLayout.LayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        btn_cancel_param.gravity = Gravity.CENTER_VERTICAL
        btn_cancel_param.setMargins(layout_width / 4 - button_size / 2, 0, 0, 0)
        btn_cancel.setLayoutParams(btn_cancel_param)
        btn_cancel.setOnClickListener { view ->
            if (typeListener != null) {
                typeListener.cancel()
            }
        }

        //确认按钮
        btn_confirm = TypeButton(getContext(), TypeButton.TYPE_CONFIRM, button_size)
        val btn_confirm_param: FrameLayout.LayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        btn_confirm_param.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        btn_confirm_param.setMargins(0, 0, layout_width / 4 - button_size / 2, 0)
        btn_confirm.setLayoutParams(btn_confirm_param)
        btn_confirm.setOnClickListener { view ->
            if (typeListener != null) {
                typeListener.confirm()
            }
        }

        //返回按钮
        btn_return = ReturnButton(getContext(), (button_size / 2.5f).toInt())
        val btn_return_param: FrameLayout.LayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        btn_return_param.gravity = Gravity.CENTER_VERTICAL
        btn_return_param.setMargins(layout_width / 6, 0, 0, 0)
        btn_return.setLayoutParams(btn_return_param)
        btn_return.setOnClickListener { v ->
            if (leftClickListener != null) {
                leftClickListener.onClick()
            }
        }
        //左边自定义按钮
        iv_custom_left = ImageView(getContext())
        val iv_custom_param_left: FrameLayout.LayoutParams = FrameLayout.LayoutParams((button_size / 2.5f).toInt(), (button_size / 2.5f).toInt())
        iv_custom_param_left.gravity = Gravity.CENTER_VERTICAL
        iv_custom_param_left.setMargins(layout_width / 6, 0, 0, 0)
        iv_custom_left!!.layoutParams = iv_custom_param_left
        iv_custom_left!!.setOnClickListener { v: View? ->
            if (leftClickListener != null) {
                leftClickListener.onClick()
            }
        }

        //右边自定义按钮
        iv_custom_right = ImageView(getContext())
        val iv_custom_param_right: FrameLayout.LayoutParams = FrameLayout.LayoutParams((button_size / 2.5f).toInt(), (button_size / 2.5f).toInt())
        iv_custom_param_right.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        iv_custom_param_right.setMargins(0, 0, layout_width / 6, 0)
        iv_custom_right!!.layoutParams = iv_custom_param_right
        iv_custom_right!!.setOnClickListener { v: View? ->
            if (rightClickListener != null) {
                rightClickListener.onClick()
            }
        }
        txt_tip = TextView(getContext())
        val txt_param: FrameLayout.LayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        txt_param.gravity = Gravity.CENTER_HORIZONTAL
        txt_param.setMargins(0, 0, 0, 0)
        txt_tip.setText(captureTip)
        txt_tip.setTextColor(-0x1)
        txt_tip.setGravity(Gravity.CENTER)
        txt_tip.setLayoutParams(txt_param)
        this.addView(btn_capture)
        this.addView(btn_cancel)
        this.addView(btn_confirm)
        this.addView(btn_return)
        this.addView(iv_custom_left)
        this.addView(iv_custom_right)
        this.addView(txt_tip)
    }

    private val captureTip: String
        private get() {
            val buttonFeatures: Int = btn_capture.getButtonFeatures()
            return when (buttonFeatures) {
                BUTTON_STATE_ONLY_CAPTURE -> getContext().getString(R.string.picture_photo_pictures)
                BUTTON_STATE_ONLY_RECORDER -> getContext().getString(R.string.picture_photo_recording)
                else -> getContext().getString(R.string.picture_photo_camera)
            }
        }

    fun resetCaptureLayout() {
        btn_capture!!.resetState()
        btn_cancel!!.setVisibility(View.GONE)
        btn_confirm!!.setVisibility(View.GONE)
        btn_capture!!.setVisibility(View.VISIBLE)
        txt_tip.setText(captureTip)
        txt_tip.setVisibility(View.VISIBLE)
        if (iconLeft != 0) iv_custom_left!!.visibility = View.VISIBLE else btn_return!!.setVisibility(View.VISIBLE)
        if (iconRight != 0) iv_custom_right!!.visibility = View.VISIBLE
    }

    fun startAlphaAnimation() {
        txt_tip.setVisibility(View.INVISIBLE)
    }

    fun setTextWithAnimation(tip: String?) {
        txt_tip.setText(tip)
        val animator_txt_tip: ObjectAnimator = ObjectAnimator.ofFloat(txt_tip, "alpha", 0f, 1f, 1f, 0f)
        animator_txt_tip.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                txt_tip.setText(captureTip)
                txt_tip.setAlpha(1f)
            }
        })
        animator_txt_tip.setDuration(2500)
        animator_txt_tip.start()
    }

    fun setDuration(duration: Int) {
        btn_capture!!.setDuration(duration)
    }

    fun setMinDuration(duration: Int) {
        btn_capture!!.setMinDuration(duration)
    }

    fun setButtonFeatures(state: Int) {
        btn_capture.setButtonFeatures(state)
        txt_tip.setText(captureTip)
    }

    fun setTip(tip: String?) {
        txt_tip.setText(tip)
    }

    fun showTip() {
        txt_tip.setVisibility(View.VISIBLE)
    }

    fun setIconSrc(iconLeft: Int, iconRight: Int) {
        this.iconLeft = iconLeft
        this.iconRight = iconRight
        if (this.iconLeft != 0) {
            iv_custom_left!!.setImageResource(iconLeft)
            iv_custom_left!!.visibility = View.VISIBLE
            btn_return!!.setVisibility(View.GONE)
        } else {
            iv_custom_left!!.visibility = View.GONE
            btn_return!!.setVisibility(View.VISIBLE)
        }
        if (this.iconRight != 0) {
            iv_custom_right!!.setImageResource(iconRight)
            iv_custom_right!!.visibility = View.VISIBLE
        } else {
            iv_custom_right!!.visibility = View.GONE
        }
    }

    fun setLeftClickListener(leftClickListener: ClickListener?) {
        this.leftClickListener = leftClickListener
    }

    fun setRightClickListener(rightClickListener: ClickListener?) {
        this.rightClickListener = rightClickListener
    }

    init {
        val manager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.getDefaultDisplay().getMetrics(outMetrics)
        layout_width = if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            outMetrics.widthPixels
        } else {
            outMetrics.widthPixels / 2
        }
        button_size = (layout_width / 4.5f).toInt()
        layout_height = button_size + button_size / 5 * 2 + 100
        initView()
        initEvent()
    }
}