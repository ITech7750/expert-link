package org.expert.link.app.shared.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLConnection
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
        canShareFiles = true,
        canSaveFiles = true,
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

    actual suspend fun shareFile(label: String, path: String): Result<Unit> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val uri = context.shareableUri(source)
        val mimeType = source.mimeType()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, source.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    actual suspend fun saveFileToDownloads(path: String, fileName: String): Result<String?> = runCatching {
        val source = File(path)
        require(source.exists()) { "Файл не найден" }
        val mimeType = source.mimeType()
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
            ) { "Не удалось создать запись в загрузках" }
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Не удалось открыть файл загрузки")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: error("Папка загрузок недоступна")
            downloadsDir.mkdirs()
            val target = uniqueTarget(downloadsDir, fileName)
            source.copyTo(target)
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mimeType), null)
            target.absolutePath
        }
    }

    actual suspend fun pickFile(): Result<String?> = runCatching {
        AndroidFilePicker.pick(context)
    }

    actual suspend fun scanQr(): Result<String?> = runCatching {
        AndroidQrScanner.scan()
    }

    actual fun buildQrCode(text: String): QrCodeMatrix? = buildAndroidQrCode(text)

    private fun Context.shareableUri(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun File.mimeType(): String =
        URLConnection.guessContentTypeFromName(name)?.ifBlank { null } ?: "application/octet-stream"

    private fun uniqueTarget(directory: File, fileName: String): File {
        return generateSequence(0) { it + 1 }
            .map { index ->
                if (index == 0) {
                    File(directory, fileName)
                } else {
                    val extension = fileName.substringAfterLast('.', "")
                    val baseName = if (extension.isBlank()) fileName else fileName.removeSuffix(".$extension")
                    val resolvedName = if (extension.isBlank()) "$baseName ($index)" else "$baseName ($index).$extension"
                    File(directory, resolvedName)
                }
            }
            .first { !it.exists() }
    }

    private companion object {
        const val CONFIG_PREFS = "expert-link-config"
        const val CONFIG_KEY = "mesh-node-config"
    }
}
