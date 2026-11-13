package com.caseopener.newtwo

import android.content.Context
import coil.imageLoader
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persist an allowlist of server image URLs that have appeared in the catalog
 * or the player's inventory. Coil caches the bytes on disk after first display.
 * When the server replaces or deletes a URL, invalidate stale disk+memory keys.
 *
 * A maximum cache size is enforced by Coil's own disk LRU. This code does not
 * invent an image catalog or request image bytes outside server-approved URLs.
 */
internal object DeviceImageCache {
    suspend fun reconcile(context: Context, cases: List<GameCase>, state: PlayerState?) {
        if(state==null) return
        val active=buildSet {
            cases.forEach { gameCase ->
                mediaUrl(gameCase.imageUrl)?.let(::add)
                gameCase.skins.forEach{skin->mediaUrl(skin.imageUrl)?.let(::add)}
            }
            state.inventory.forEach{item->mediaUrl(item.imageUrl)?.let(::add)}
        }
        withContext(Dispatchers.IO) {
            val prefs=context.applicationContext
                .getSharedPreferences("caseopener_image_allowlist_v1",Context.MODE_PRIVATE)
            val previous=prefs.getStringSet("paths",emptySet())?.toSet().orEmpty()
            val stale=previous-active
            val loader=context.imageLoader
            for(url in stale) {
                try {
                    loader.diskCache?.remove(url)
                    loader.memoryCache?.remove(MemoryCache.Key(url))
                } catch (_:Exception) {
                    // A transient cache lock must never prevent the game from loading.
                }
            }
            prefs.edit().putStringSet("paths",active).apply()
        }
    }
}
