package com.luck.picture.lib.dialog

import android.R
import android.view.View
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.luck.picture.lib.listener.OnItemClickListener

/**
 * @author：luck
 * @date：2019-12-12 16:39
 * @describe：PhotoSelectedDialog
 */
class PhotoItemSelectedDialog : DialogFragment(), View.OnClickListener {
    private var tvPicturePhoto: TextView? = null
    private var tvPictureVideo: TextView? = null
    private var tvPictureCancel: TextView? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (dialog != null) {
            dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if (dialog!!.window != null) {
                dialog!!.window!!.setBackgroundDrawableResource(R.color.transparent)
            }
        }
        return inflater.inflate(R.layout.picture_dialog_camera_selected, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvPicturePhoto = view.findViewById<TextView>(R.id.picture_tv_photo)
        tvPictureVideo = view.findViewById<TextView>(R.id.picture_tv_video)
        tvPictureCancel = view.findViewById<TextView>(R.id.picture_tv_cancel)
        tvPictureVideo.setOnClickListener(this)
        tvPicturePhoto.setOnClickListener(this)
        tvPictureCancel.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        initDialogStyle()
    }

    /**
     * DialogFragment Style
     */
    private fun initDialogStyle() {
        val dialog = dialog
        if (dialog != null) {
            val window = dialog.window
            if (window != null) {
                window.setLayout(ScreenUtils.getScreenWidth(context), RelativeLayout.LayoutParams.WRAP_CONTENT)
                window.setGravity(Gravity.BOTTOM)
                window.setWindowAnimations(R.style.PictureThemeDialogFragmentAnim)
            }
        }
    }

    private var onItemClickListener: OnItemClickListener? = null
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onClick(v: View) {
        val id = v.id
        if (onItemClickListener != null) {
            if (id == R.id.picture_tv_photo) {
                onItemClickListener.onItemClick(v, IMAGE_CAMERA)
            }
            if (id == R.id.picture_tv_video) {
                onItemClickListener.onItemClick(v, VIDEO_CAMERA)
            }
        }
        dismissAllowingStateLoss()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    companion object {
        const val IMAGE_CAMERA = 0
        const val VIDEO_CAMERA = 1
        fun newInstance(): PhotoItemSelectedDialog {
            return PhotoItemSelectedDialog()
        }
    }
}