package com.luck.picture.lib.language

import java.util.*

/**
 * @author：luck
 * @date：2019-11-25 21:58
 * @describe：语言转换
 */
object LocaleTransform {
    fun getLanguage(language: Int): Locale {
        return when (language) {
            LanguageConfig.ENGLISH ->                 // 英语-美国
                Locale.ENGLISH
            LanguageConfig.TRADITIONAL_CHINESE ->                 // 繁体中文
                Locale.TRADITIONAL_CHINESE
            LanguageConfig.KOREA ->                 // 韩语
                Locale.KOREA
            LanguageConfig.GERMANY ->                 // 德语
                Locale.GERMANY
            LanguageConfig.FRANCE ->                 // 法语
                Locale.FRANCE
            LanguageConfig.JAPAN ->                 // 日语
                Locale.JAPAN
            LanguageConfig.VIETNAM ->                 // 越南语
                Locale("vi")
            else ->                 // 简体中文
                Locale.CHINESE
        }
    }
}