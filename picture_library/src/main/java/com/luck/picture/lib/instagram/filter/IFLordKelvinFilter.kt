package com.luck.picture.lib.instagram.filter

import android.content.*
import com.luck.picture.lib.R

/**
 * Created by sam on 14-8-9.
 */
class IFLordKelvinFilter(paramContext: Context) : IFImageFilter(paramContext, SHADER) {
    private fun setRes() {
        addInputTexture(R.drawable.kelvin_map)
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
                "     \n" +
                "     vec2 lookup;\n" +
                "     lookup.y = .5;\n" +
                "     \n" +
                "     lookup.x = texel.r;\n" +
                "     texel.r = texture2D(inputImageTexture2, lookup).r;\n" +
                "     \n" +
                "     lookup.x = texel.g;\n" +
                "     texel.g = texture2D(inputImageTexture2, lookup).g;\n" +
                "     \n" +
                "     lookup.x = texel.b;\n" +
                "     texel.b = texture2D(inputImageTexture2, lookup).b;\n" +
                "     \n" +
                "     gl_FragColor = vec4(texel, 1.0);\n" +
                " }\n"
    }

    init {
        setRes()
    }
}