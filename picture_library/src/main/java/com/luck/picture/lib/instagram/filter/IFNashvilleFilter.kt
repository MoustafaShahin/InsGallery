package com.luck.picture.lib.instagram.filter

import android.content.*
import com.luck.picture.lib.R

/**
 * Created by sam on 14-8-9.
 */
class IFNashvilleFilter(paramContext: Context) : IFImageFilter(paramContext, SHADER) {
    private fun setRes() {
        addInputTexture(R.drawable.nashville_map)
    }

    companion object {
        private const val SHADER = "precision lowp float;\n" +
                " \n" +
                " varying highp vec2 textureCoordinate;\n" +
                " \n" +
                " uniform sampler2D inputImageTexture;\n" +
                " uniform sampler2D inputImageTexture2;\n" +
                " \n" +
                " void main()\n" +
                " {\n" +
                "     vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
                "     texel = vec3(\n" +
                "                  texture2D(inputImageTexture2, vec2(texel.r, .16666)).r,\n" +
                "                  texture2D(inputImageTexture2, vec2(texel.g, .5)).g,\n" +
                "                  texture2D(inputImageTexture2, vec2(texel.b, .83333)).b);\n" +
                "     gl_FragColor = vec4(texel, 1.0);\n" +
                " }\n"
    }

    init {
        setRes()
    }
}