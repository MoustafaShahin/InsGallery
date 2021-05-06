package com.luck.pictureselector

import android.content.Context
import com.luck.picture.lib.engine.CacheResourcesEngine
import com.luck.pictureselector.ImageCacheUtils.getCacheFileTo3x
import com.luck.pictureselector.ImageCacheUtils.getCacheFileTo4x
import java.io.File

/**
 * @author：luck
 * @date：2020-03-24 09:48
 * @describe：GlideCacheEngine
 */
class GlideCacheEngine private constructor() : CacheResourcesEngine {
    override fun onCachePath(context: Context, url: String): String {
        val cacheFile: File?
        cacheFile = if (GLIDE_VERSION >= 4) {
            // Glide 4.x
            getCacheFileTo4x(context, url)
        } else {
            // Glide 3.x
            getCacheFileTo3x(context, url)
        }
        return if (cacheFile != null) cacheFile.absolutePath else ""
    }

    companion object {
        /**
         * glide版本号,请根据用户集成为准 这里只是模拟
         */
        private const val GLIDE_VERSION = 4
        private var instance: GlideCacheEngine? = null
        fun createCacheEngine(): GlideCacheEngine? {
            if (null == instance) {
                synchronized(GlideCacheEngine::class.java) {
                    if (null == instance) {
                        instance = GlideCacheEngine()
                    }
                }
            }
            return instance
        }
    }
}