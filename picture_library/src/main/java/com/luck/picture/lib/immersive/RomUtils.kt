package com.luck.picture.lib.immersive

import com.luck.picture.lib.tools.StringUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * @author：luck
 * @data：2018/3/28 下午1:02
 * @描述: Rom版本管理
 */
object RomUtils {
    private var romType: Int? = null
    fun getLightStatausBarAvailableRomType(): Int {
        if (romType != null) {
            return romType!!
        }
        if (isMIUIV6OrAbove()) {
            romType = AvailableRomType.MIUI
            return romType!!
        }
        if (isFlymeV4OrAbove()) {
            romType = AvailableRomType.FLYME
            return romType!!
        }
        if (isAndroid5OrAbove()) {
            romType = AvailableRomType.ANDROID_NATIVE
            return romType!!
        }
        romType = AvailableRomType.NA
        return romType!!
    }

    //Flyme V4的displayId格式为 [Flyme OS 4.x.x.xA]
    //Flyme V5的displayId格式为 [Flyme 5.x.x.x beta]
    private fun isFlymeV4OrAbove(): Boolean {
        return getFlymeVersion() >= 4
    }

    //Flyme V4的displayId格式为 [Flyme OS 4.x.x.xA]
    //Flyme V5的displayId格式为 [Flyme 5.x.x.x beta]
    fun getFlymeVersion(): Int {
        var displayId: String = Build.DISPLAY
        if (!TextUtils.isEmpty(displayId) && displayId.contains("Flyme")) {
            displayId = displayId.replace("Flyme".toRegex(), "")
            displayId = displayId.replace("OS".toRegex(), "")
            displayId = displayId.replace(" ".toRegex(), "")
            val version = displayId.substring(0, 1)
            if (version != null) {
                return StringUtils.stringToInt(version)
            }
        }
        return 0
    }

    //MIUI V6对应的versionCode是4
    //MIUI V7对应的versionCode是5
    private fun isMIUIV6OrAbove(): Boolean {
        val miuiVersionCodeStr = getSystemProperty("ro.miui.ui.version.code")
        if (!TextUtils.isEmpty(miuiVersionCodeStr)) {
            try {
                val miuiVersionCode = miuiVersionCodeStr!!.toInt()
                if (miuiVersionCode >= 4) {
                    return true
                }
            } catch (e: Exception) {
            }
        }
        return false
    }

    fun getMIUIVersionCode(): Int {
        val miuiVersionCodeStr = getSystemProperty("ro.miui.ui.version.code")
        var miuiVersionCode = 0
        if (!TextUtils.isEmpty(miuiVersionCodeStr)) {
            try {
                miuiVersionCode = miuiVersionCodeStr!!.toInt()
                return miuiVersionCode
            } catch (e: Exception) {
            }
        }
        return miuiVersionCode
    }

    //Android Api 23以上
    private fun isAndroid5OrAbove(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            true
        } else false
    }

    fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                }
            }
        }
        return line
    }

    object AvailableRomType {
        const val MIUI = 1
        const val FLYME = 2
        const val ANDROID_NATIVE = 3
        const val NA = 4
    }
}