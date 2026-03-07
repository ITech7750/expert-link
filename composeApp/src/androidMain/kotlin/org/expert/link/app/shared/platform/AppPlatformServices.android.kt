package org.expert.link.app.shared.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.expert.link.app.android.AndroidFilePicker
import org.expert.link.app.android.AndroidMulticastSupport
import org.expert.link.app.android.AndroidQrScanner
import org.expert.link.app.android.AndroidWebRtcMediaEngineAdapter
import org.expert.link.app.android.buildAndroidQrCode
import org.expert.link.database.createPersistentRepositoryBundle
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import kotlinx.serialization.json.Json

private object AndroidPlatformContextHolder {
    @Volatile
    private var context: Context? = null

    fun set(value: Context) {
        context = value.applicationContext
    }

    fun require(): Context {
        return requireNotNull(context) { "Android context is not initialized. Call initializeAppPlatformContext() before AppPlatformServices." }
    }
}

/** Инициализирует Android context для actual-сервисов платформы. */
fun initializeAppPlatformContext(context: Context) {
    AndroidPlatformContextHolder.set(context)
}

/** Android actual-реализация платформенных сервисов client-модуля. */
actual class AppPlatformServices actual constructor(
    @Suppress("UNUSED_PARAMETER")
    private val args: List<String>,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val context: Context
        get() = AndroidPlatformContextHolder.require()

    private val mediaEngine by lazy { AndroidWebRtcMediaEngineAdapter(context) }
    private val multicastSupport by lazy { AndroidMulticastSupport { context } }
    private val persistentRepositories by lazy {
        createPersistentRepositoryBundle(
            context = context,
            name = context.getDatabasePath("expert-link.db").absolutePath,
        )
    }

    actual val platformName: String = "Android"
    actual val transportHint: String = "Полная сеть"
    actual val capabilities: PlatformCapabilities = PlatformCapabilities(
        canCopyText = true,
        canShareText = true,
        canRenderQr = true,
        canScanQr = true,
        canPickFile = true,
        prefersWideLayout = false,
    )

    actual fun defaultConfig(): MeshNodeConfig {
        val base = createDefaultNodeConfig(
            displayNamePrefix = "android",
            realTransport = true,
            realDiscovery = true,
        )
        val persisted = context
            .getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getString(CONFIG_KEY, null)
            ?.let { encoded ->
                runCatching { json.decodeFromString(MeshNodeConfig.serializer(), encoded) }.getOrNull()
            }
        val resolved = persisted ?: base
        return resolved.copy(
            fileTransfer = resolved.fileTransfer.copy(
                downloadDirectory = context.filesDir.resolve("expert-link/downloads").absolutePath,
            ),
        )
    }

    actual fun persistConfig(config: MeshNodeConfig) {
        val encoded = json.encodeToString(MeshNodeConfig.serializer(), config)
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CONFIG_KEY, encoded)
            .apply()
    }

    actual suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(
        configuration = config,
        mediaEngine = mediaEngine,
        multicastSupport = multicastSupport,
        persistentRepositories = persistentRepositories,
    )

    actual suspend fun copyText(label: String, text: String): Result<Unit> = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    actual suspend fun shareText(label: String, text: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    actual suspend fun pickFile(): Result<String?> = runCatching {
        AndroidFilePicker.pick(context)
    }

    actual suspend fun scanQr(): Result<String?> = runCatching {
        AndroidQrScanner.scan()
    }

    actual fun buildQrCode(text: String): QrCodeMatrix? = buildAndroidQrCode(text)

    private companion object {
        const val CONFIG_PREFS = "expert-link-config"
        const val CONFIG_KEY = "mesh-node-config"
    }
}
