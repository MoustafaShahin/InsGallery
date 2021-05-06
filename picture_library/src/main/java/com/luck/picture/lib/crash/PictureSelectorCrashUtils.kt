package com.luck.picture.lib.crash

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.*
import android.os.Build.VERSION
import com.luck.picture.lib.app.PictureAppMaster
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author：luck
 * @date：2019-12-03 14:53
 * @describe：PictureSelector Crash Log collection class
 */
class PictureSelectorCrashUtils private constructor() {
    companion object {
        private var mInitialized = false
        private var defaultDir: String? = null
        private var dir: String? = null
        private var versionName: String? = null
        private var versionCode = 0
        private val FILE_SEP = System.getProperty("file.separator")
        private val FORMAT: Format = SimpleDateFormat("MM-dd HH-mm-ss", Locale.getDefault())
        private val CRASH_HEAD: String? = null
        private val UNCAUGHT_EXCEPTION_HANDLER: Thread.UncaughtExceptionHandler? = null

        /**
         * 初始化
         *
         * 需添加权限 `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>`
         *
         * @return `true`: 初始化成功<br></br>`false`: 初始化失败
         */
        fun init(listener: CrashAppListener?): Boolean {
            return init("", listener)
        }

        /**
         * 初始化
         *
         * 需添加权限 `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>`
         *
         * @param crashDir 崩溃文件存储目录
         * @return `true`: 初始化成功<br></br>`false`: 初始化失败
         */
        fun init(crashDir: File): Boolean {
            return init(crashDir.absolutePath + FILE_SEP, null)
        }
        /**
         * 初始化
         *
         * 需添加权限 `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>`
         *
         * @param crashDir 崩溃文件存储目录
         * @return `true`: 初始化成功<br></br>`false`: 初始化失败
         */
        /**
         * 初始化
         *
         * 需添加权限 `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>`
         *
         * @return `true`: 初始化成功<br></br>`false`: 初始化失败
         */
        @JvmOverloads
        fun init(crashDir: String = "", listener: CrashAppListener? = null): Boolean {
            mFinishAppListener = listener
            if (isSpace(crashDir)) {
                dir = null
            } else {
                dir = if (crashDir.endsWith(FILE_SEP!!)) dir else dir + FILE_SEP
            }
            if (mInitialized) {
                return true
            }
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() && PictureAppMaster.getInstance().getAppContext().getExternalCacheDir() != null) {
                defaultDir = PictureAppMaster.getInstance().getAppContext().getExternalCacheDir().toString() + FILE_SEP + "crash" + FILE_SEP
            } else {
                defaultDir = PictureAppMaster.getInstance().getAppContext().getCacheDir().toString() + FILE_SEP + "crash" + FILE_SEP
            }
            Thread.setDefaultUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER)
            return true.also { mInitialized = it }
        }

        private fun createOrExistsFile(filePath: String): Boolean {
            val file = File(filePath)
            if (file.exists()) {
                return file.isFile
            }
            return if (!createOrExistsDir(file.parentFile)) {
                false
            } else try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        private fun createOrExistsDir(file: File?): Boolean {
            return file != null && if (file.exists()) file.isDirectory else file.mkdirs()
        }

        private fun isSpace(s: String?): Boolean {
            if (s == null) {
                return true
            }
            var i = 0
            val len = s.length
            while (i < len) {
                if (!Character.isWhitespace(s[i])) {
                    return false
                }
                ++i
            }
            return true
        }

        private var mFinishAppListener: CrashAppListener? = null
        fun setCrashListener(crashListener: CrashAppListener?) {
            mFinishAppListener = crashListener
        }

        init {
            try {
                val pi: PackageInfo = PictureAppMaster.getInstance().getAppContext()
                        .getPackageManager()
                        .getPackageInfo(PictureAppMaster.getInstance().getAppContext().getPackageName(), 0)
                if (com.luck.picture.lib.crash.pi != null) {
                    versionName = com.luck.picture.lib.crash.pi.versionName
                    versionCode = com.luck.picture.lib.crash.pi.versionCode
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            CRASH_HEAD = """
                
                ************* Crash Log Head ****************
                Device Manufacturer: ${Build.MANUFACTURER}
                Device Model       : ${Build.MODEL}
                Android Version    : ${VERSION.RELEASE}
                Android SDK        : ${VERSION.SDK_INT}
                App VersionName    : $versionName
                App VersionCode    : $versionCode
                ************* Crash Log Head ****************
                
                
                """.trimIndent()
            UNCAUGHT_EXCEPTION_HANDLER = Thread.UncaughtExceptionHandler { t, e ->
                if (mFinishAppListener != null) {
                    mFinishAppListener!!.onFinishApp(t, e)
                }
                val now = Date(System.currentTimeMillis())
                val fileName = FORMAT.format(now) + ".txt"
                val fullPath = (if (dir == null) defaultDir else dir) + fileName
                if (createOrExistsFile(fullPath)) {
                    var pw: PrintWriter? = null
                    try {
                        pw = PrintWriter(FileWriter(fullPath, false))
                        pw.write(CRASH_HEAD)
                        e.printStackTrace(pw)
                        var cause = e.cause
                        while (cause != null) {
                            cause.printStackTrace(pw)
                            cause = cause.cause
                        }
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    } finally {
                        pw?.close()
                    }
                }
                Process.killProcess(Process.myPid())
                System.exit(0)
            }
        }
    }

    interface CrashAppListener {
        fun onFinishApp(t: Thread?, e: Throwable?)
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}