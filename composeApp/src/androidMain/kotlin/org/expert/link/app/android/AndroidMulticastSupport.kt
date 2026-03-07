package org.expert.link.app.android

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import org.expert.link.mesh.contract.api.MeshMulticastSupport

/** Android-реализация multicast boundary для backend discovery. */
class AndroidMulticastSupport(
    private val contextProvider: () -> Context,
) : MeshMulticastSupport {
    private val tag = "AndroidMulticastSupport"
    @Volatile
    private var lock: WifiManager.MulticastLock? = null

    override suspend fun isMulticastSupported(): Boolean {
        return wifiManager() != null
    }

    override suspend fun prepareForMulticast() {
        val manager = wifiManager() ?: return
        val preparedLock = lock ?: synchronized(this) {
            lock ?: manager.createMulticastLock("expert-link-discovery").apply {
                setReferenceCounted(false)
            }.also { created -> lock = created }
        }
        if (preparedLock.isHeld) {
            return
        }
        runCatching { preparedLock.acquire() }
            .onFailure { error -> Log.w(tag, "Failed to acquire Android MulticastLock", error) }
    }

    override suspend fun releaseMulticast() {
        val current = lock ?: return
        if (!current.isHeld) {
            return
        }
        runCatching { current.release() }
            .onFailure { error -> Log.w(tag, "Failed to release Android MulticastLock", error) }
    }

    private fun wifiManager(): WifiManager? {
        return contextProvider().applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }
}
