package org.expert.link.app.shared.platform

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.expert.link.app.desktop.DesktopWebRtcMediaEngineAdapter
import org.expert.link.app.desktop.buildDesktopQrCode
import org.expert.link.database.createPersistentRepositoryBundle
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import kotlinx.serialization.json.Json

/** Desktop actual-реализация платформенных сервисов client-модуля. */
actual class AppPlatformServices actual constructor(
    private val args: List<String>,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val memoryMode: Boolean = args.any { it == "--memory" }
    private val mediaEngine = DesktopWebRtcMediaEngineAdapter()
    private val storageDirectory = File(System.getProperty("user.home"), ".expert-link").apply { mkdirs() }
    private val configFile = File(storageDirectory, "node-config.json")
    private val persistentRepositories by lazy {
        createPersistentRepositoryBundle(
            path = File(storageDirectory, "expert-link.db").absolutePath,
        )
    }

    actual val platformName: String = "Рабочий стол"
    actual val transportHint: String = if (memoryMode) "Локальная проверка" else "Полная сеть"
    actual val capabilities: PlatformCapabilities = PlatformCapabilities(
        canCopyText = true,
        canShareText = true,
        canRenderQr = true,
        canScanQr = false,
        canPickFile = true,
        prefersWideLayout = true,
    )

    actual fun defaultConfig(): MeshNodeConfig {
        val fallback = createDefaultNodeConfig(
            displayNamePrefix = "desktop",
            realTransport = true,
            realDiscovery = true,
        ).copy(
            fileTransfer = MeshFileTransferConfig(
                downloadDirectory = File(storageDirectory, "downloads").absolutePath,
            ),
        )
        val base = loadPersistedConfig()?.let { persisted ->
            persisted.copy(
                fileTransfer = persisted.fileTransfer.copy(
                    downloadDirectory = File(storageDirectory, "downloads").absolutePath,
                ),
            )
        } ?: fallback
        return args.fold(base) { config, arg ->
            when {
                arg.startsWith("--name=") -> config.copy(displayName = arg.substringAfter('='))
                arg.startsWith("--bind=") -> config.copy(bindHost = arg.substringAfter('='))
                arg.startsWith("--http=") -> arg.substringAfter('=').toIntOrNull()?.let { config.copy(httpPort = it) } ?: config
                arg.startsWith("--discovery=") -> arg.substringAfter('=').toIntOrNull()?.let { config.copy(discoveryPort = it) } ?: config
                arg.startsWith("--multicast=") -> config.copy(multicastGroup = arg.substringAfter('='))
                arg == "--memory" -> config.copy(
                    featureFlags = config.featureFlags.copy(
                        inMemoryTransport = true,
                        inMemoryDiscovery = true,
                    ),
                )
                else -> config
            }
        }
    }

    actual fun persistConfig(config: MeshNodeConfig) {
        configFile.parentFile?.mkdirs()
        configFile.writeText(json.encodeToString(MeshNodeConfig.serializer(), config))
    }

    actual suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(
        configuration = config,
        mediaEngine = mediaEngine,
        persistentRepositories = persistentRepositories,
    )

    actual suspend fun copyText(label: String, text: String): Result<Unit> = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    actual suspend fun shareText(label: String, text: String): Result<Unit> = runCatching {
        val encodedSubject = URLEncoder.encode(label, StandardCharsets.UTF_8.toString())
        val encodedBody = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val uri = URI("mailto:?subject=$encodedSubject&body=$encodedBody")
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            Desktop.getDesktop().mail(uri)
        } else {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
        }
    }

    actual suspend fun pickFile(): Result<String?> = runCatching {
        val dialog = FileDialog(null as Frame?, "Выберите файл", FileDialog.LOAD)
        dialog.isVisible = true
        val selected = dialog.file ?: return@runCatching null
        File(dialog.directory, selected).absolutePath
    }

    actual suspend fun scanQr(): Result<String?> = Result.failure(
        UnsupportedOperationException("Сканирование QR на desktop не поддерживается"),
    )

    actual fun buildQrCode(text: String): QrCodeMatrix? = buildDesktopQrCode(text)

    private fun loadPersistedConfig(): MeshNodeConfig? {
        if (!configFile.exists()) return null
        return try {
            json.decodeFromString(MeshNodeConfig.serializer(), configFile.readText())
        } catch (_: Exception) {
            null
        }
    }
}
