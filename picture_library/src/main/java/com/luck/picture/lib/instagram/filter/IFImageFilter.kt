package com.luck.picture.lib.instagram.filter

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils
import java.util.*

/**
 * Created by sam on 14-8-9.
 */
class IFImageFilter(private val mContext: Context, fragmentShaderString: String?) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, fragmentShaderString) {
    private var filterInputTextureUniform2 = 0
    private var filterInputTextureUniform3 = 0
    private var filterInputTextureUniform4 = 0
    private var filterInputTextureUniform5 = 0
    private var filterInputTextureUniform6 = 0
    var filterSourceTexture2 = OpenGlUtils.NO_TEXTURE
    var filterSourceTexture3 = OpenGlUtils.NO_TEXTURE
    var filterSourceTexture4 = OpenGlUtils.NO_TEXTURE
    var filterSourceTexture5 = OpenGlUtils.NO_TEXTURE
    var filterSourceTexture6 = OpenGlUtils.NO_TEXTURE
    private var mResIds: MutableList<Int>? = null
    override fun onInit() {
        super.onInit()
        filterInputTextureUniform2 = GLES20.glGetUniformLocation(program, "inputImageTexture2")
        filterInputTextureUniform3 = GLES20.glGetUniformLocation(program, "inputImageTexture3")
        filterInputTextureUniform4 = GLES20.glGetUniformLocation(program, "inputImageTexture4")
        filterInputTextureUniform5 = GLES20.glGetUniformLocation(program, "inputImageTexture5")
        filterInputTextureUniform6 = GLES20.glGetUniformLocation(program, "inputImageTexture6")
        initInputTexture()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (filterSourceTexture2 != OpenGlUtils.NO_TEXTURE) {
            val arrayOfInt1 = IntArray(1)
            arrayOfInt1[0] = filterSourceTexture2
            GLES20.glDeleteTextures(1, arrayOfInt1, 0)
            filterSourceTexture2 = OpenGlUtils.NO_TEXTURE
        }
        if (filterSourceTexture3 != OpenGlUtils.NO_TEXTURE) {
            val arrayOfInt2 = IntArray(1)
            arrayOfInt2[0] = filterSourceTexture3
            GLES20.glDeleteTextures(1, arrayOfInt2, 0)
            filterSourceTexture3 = OpenGlUtils.NO_TEXTURE
        }
        if (filterSourceTexture4 != OpenGlUtils.NO_TEXTURE) {
            val arrayOfInt3 = IntArray(1)
            arrayOfInt3[0] = filterSourceTexture4
            GLES20.glDeleteTextures(1, arrayOfInt3, 0)
            filterSourceTexture4 = OpenGlUtils.NO_TEXTURE
        }
        if (filterSourceTexture5 != OpenGlUtils.NO_TEXTURE) {
            val arrayOfInt4 = IntArray(1)
            arrayOfInt4[0] = filterSourceTexture5
            GLES20.glDeleteTextures(1, arrayOfInt4, 0)
            filterSourceTexture5 = OpenGlUtils.NO_TEXTURE
        }
        if (filterSourceTexture6 != OpenGlUtils.NO_TEXTURE) {
            val arrayOfInt5 = IntArray(1)
            arrayOfInt5[0] = filterSourceTexture6
            GLES20.glDeleteTextures(1, arrayOfInt5, 0)
            filterSourceTexture6 = OpenGlUtils.NO_TEXTURE
        }
    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        if (filterSourceTexture2 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, filterSourceTexture2)
            GLES20.glUniform1i(filterInputTextureUniform2, 3)
        }
        if (filterSourceTexture3 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE4)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, filterSourceTexture3)
            GLES20.glUniform1i(filterInputTextureUniform3, 4)
        }
        if (filterSourceTexture4 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE5)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, filterSourceTexture4)
            GLES20.glUniform1i(filterInputTextureUniform4, 5)
        }
        if (filterSourceTexture5 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE6)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, filterSourceTexture5)
            GLES20.glUniform1i(filterInputTextureUniform5, 6)
        }
        if (filterSourceTexture6 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE7)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, filterSourceTexture6)
            GLES20.glUniform1i(filterInputTextureUniform6, 7)
        }
    }

    fun addInputTexture(resId: Int) {
        if (mResIds == null) {
            mResIds = ArrayList()
        }
        mResIds!!.add(resId)
    }

    fun initInputTexture() {
        if (mResIds == null) {
            return
        }
        if (mResIds!!.size > 0) {
            runOnDraw {
                val b = BitmapFactory.decodeResource(mContext.resources, mResIds!![0])
                filterSourceTexture2 = OpenGlUtils.loadTexture(b, OpenGlUtils.NO_TEXTURE, true)
            }
        }
        if (mResIds!!.size > 1) {
            runOnDraw {
                val b = BitmapFactory.decodeResource(mContext.resources, mResIds!![1])
                filterSourceTexture3 = OpenGlUtils.loadTexture(b, OpenGlUtils.NO_TEXTURE, true)
            }
        }
        if (mResIds!!.size > 2) {
            runOnDraw {
                val b = BitmapFactory.decodeResource(mContext.resources, mResIds!![2])
                filterSourceTexture4 = OpenGlUtils.loadTexture(b, OpenGlUtils.NO_TEXTURE, true)
            }
        }
        if (mResIds!!.size > 3) {
            runOnDraw {
                val b = BitmapFactory.decodeResource(mContext.resources, mResIds!![3])
                filterSourceTexture5 = OpenGlUtils.loadTexture(b, OpenGlUtils.NO_TEXTURE, true)
            }
        }
        if (mResIds!!.size > 4) {
            runOnDraw {
                val b = BitmapFactory.decodeResource(mContext.resources, mResIds!![4])
                filterSourceTexture6 = OpenGlUtils.loadTexture(b, OpenGlUtils.NO_TEXTURE, true)
            }
        }
    }
}