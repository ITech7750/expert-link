package org.expert.link.app.android

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Android QR-сканер на базе zxing-embedded.
 *
 * Служит bridge между `AppPlatformServices` и Activity Result API.
 */
internal object AndroidQrScanner {
    private val mutex = Mutex()
    private var launcher: ActivityResultLauncher<ScanOptions>? = null
    private var pending: CompletableDeferred<String?>? = null

    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ScanContract()) { result ->
            val deferred = pending
            pending = null
            deferred?.complete(result.contents)
        }
    }

    suspend fun scan(): String? = mutex.withLock {
        val currentLauncher = launcher ?: error("QR scanner launcher is not initialized")
        check(pending == null) { "QR scanning is already in progress" }
        val deferred = CompletableDeferred<String?>()
        pending = deferred
        withContext(Dispatchers.Main) {
            currentLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                    setPrompt("Наведите камеру на QR-код или штрихкод")
                    setBeepEnabled(false)
                    setOrientationLocked(false)
                },
            )
        }
        return@withLock deferred.await()
    }
}
