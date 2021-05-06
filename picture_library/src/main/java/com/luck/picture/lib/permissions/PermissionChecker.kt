package com.luck.picture.lib.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * @author：luck
 * @date：2019-11-20 19:07
 * @describe：权限检查
 */
object PermissionChecker {
    /**
     * 检查是否有某个权限
     *
     * @param ctx
     * @param permission
     * @return
     */
    fun checkSelfPermission(ctx: Context, permission: String?): Boolean {
        return (ContextCompat.checkSelfPermission(ctx.applicationContext, permission!!)
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * 动态申请多个权限
     *
     * @param activity
     * @param code
     */
    fun requestPermissions(activity: Activity?, permissions: Array<String?>, code: Int) {
        ActivityCompat.requestPermissions(activity!!, permissions, code)
    }

    /**
     * Launch the application's details settings.
     */
    fun launchAppDetailsSettings(context: Context) {
        val applicationContext = context.applicationContext
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + applicationContext.packageName)
        if (!isIntentAvailable(context, intent)) return
        applicationContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return context.applicationContext
                .packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .size > 0
    }
}