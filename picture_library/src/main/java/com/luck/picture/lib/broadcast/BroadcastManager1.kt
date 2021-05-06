package com.luck.picture.lib.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*

/**
 * @author：luck
 * @date：2019-11-20 13:45
 * @describe：本地广播
 */
class BroadcastManager {
    private var localBroadcastManager: LocalBroadcastManager? = null
    private var intent: Intent? = null
    private var action: String? = null
    fun intent(intent: Intent?): BroadcastManager {
        this.intent = intent
        return this
    }

    fun action(action: String?): BroadcastManager {
        this.action = action
        return this
    }

    fun extras(bundle: Bundle?): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtras(bundle!!)
        return this
    }

    fun put(key: String?, value: ArrayList<out Parcelable?>?): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Array<Parcelable?>?): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Parcelable?): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Float): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Double): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Long): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Boolean): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, value: Int): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, value)
        return this
    }

    fun put(key: String?, str: String?): BroadcastManager {
        createIntent()
        if (intent == null) {
            Log.e(TAG, "intent create failed")
            return this
        }
        intent!!.putExtra(key, str)
        return this
    }

    private fun createIntent() {
        if (intent == null) {
            Log.d(TAG, "intent is not created")
        }
        if (intent == null) {
            if (!TextUtils.isEmpty(action)) {
                intent = Intent(action)
            }
            Log.d(TAG, "intent created with action")
        }
    }

    fun broadcast() {
        createIntent()
        if (intent == null) {
            return
        }
        if (action == null) {
            return
        }
        intent!!.action = action
        if (null != localBroadcastManager) {
            localBroadcastManager!!.sendBroadcast(intent!!)
        }
    }

    fun registerReceiver(br: BroadcastReceiver?, actions: List<String?>?) {
        if (null == br || null == actions) {
            return
        }
        val iFilter = IntentFilter()
        if (actions != null) {
            for (action in actions) {
                iFilter.addAction(action)
            }
        }
        if (null != localBroadcastManager) {
            localBroadcastManager!!.registerReceiver(br, iFilter)
        }
    }

    fun registerReceiver(br: BroadcastReceiver?, vararg actions: String?) {
        if (actions == null || actions.size <= 0) {
            return
        }
        registerReceiver(br, Arrays.asList(*actions))
    }

    /**
     * @param br
     */
    fun unregisterReceiver(br: BroadcastReceiver?) {
        if (null == br) {
            return
        }
        try {
            localBroadcastManager!!.unregisterReceiver(br)
        } catch (e: Exception) {
        }
    }

    /**
     * @param br
     * @param actions 至少传入一个
     */
    fun unregisterReceiver(br: BroadcastReceiver?, vararg actions: String) {
        unregisterReceiver(br)
    }

    companion object {
        private val TAG = BroadcastManager::class.java.simpleName
        fun getInstance(ctx: Context): BroadcastManager {
            val broadcastManager = BroadcastManager()
            broadcastManager.localBroadcastManager = LocalBroadcastManager.getInstance(ctx.applicationContext)
            return broadcastManager
        }
    }
}