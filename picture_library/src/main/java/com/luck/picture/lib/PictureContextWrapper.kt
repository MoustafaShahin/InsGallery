package com.luck.picture.lib

import android.content.Context
import com.luck.picture.lib.language.PictureLanguageUtils

/**
 * @author：luck
 * @date：2019-12-15 19:34
 * @describe：ContextWrapper
 */
class PictureContextWrapper(base: Context?) : ContextWrapper(base) {
    companion object {
        fun wrap(context: Context?, language: Int): ContextWrapper {
            PictureLanguageUtils.setAppLanguage(context, language)
            return PictureContextWrapper(context)
        }
    }
}