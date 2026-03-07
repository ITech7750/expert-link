package org.expert.link.app.android

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object AndroidFilePicker {
    private val mutex = Mutex()
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<Uri?>? = null

    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val deferred = pending
            pending = null
            deferred?.complete(uri)
        }
    }

    suspend fun pick(context: Context): String? = mutex.withLock {
        val currentLauncher = launcher ?: error("File picker launcher is not initialized")
        check(pending == null) { "File picking is already in progress" }
        val deferred = CompletableDeferred<Uri?>()
        pending = deferred
        withContext(Dispatchers.Main) {
            currentLauncher.launch(arrayOf("*/*"))
        }
        val uri = deferred.await() ?: return@withLock null
        return@withLock withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val fileName = resolver.getType(uri)
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: "shared-${UUID.randomUUID()}"
            val target = File(context.cacheDir, "expert-link-picker/$fileName")
            target.parentFile?.mkdirs()
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use(input::copyTo)
            } ?: error("Unable to open selected file")
            target.absolutePath
        }
    }
}
