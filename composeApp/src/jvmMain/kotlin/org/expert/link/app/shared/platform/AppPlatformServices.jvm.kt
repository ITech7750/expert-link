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
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshNodeConfig

/** Desktop actual-реализация платформенных сервисов client-модуля. */
actual class AppPlatformServices actual constructor(
    private val args: List<String>,
) {
    private val memoryMode: Boolean = args.any { it == "--memory" }
    private val mediaEngine = DesktopWebRtcMediaEngineAdapter()

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
        val base = createDefaultNodeConfig(
            displayNamePrefix = "desktop",
            realTransport = true,
            realDiscovery = true,
        )
        return args.fold(base) { config, arg ->
            when {
                arg.startsWith("--name=") -> config.copy(displayName = arg.substringAfter('='))
                arg.startsWith("--http=") -> arg.substringAfter('=').toIntOrNull()?.let { config.copy(httpPort = it) } ?: config
                arg.startsWith("--discovery=") -> arg.substringAfter('=').toIntOrNull()?.let { config.copy(discoveryPort = it) } ?: config
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

    actual suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(config, mediaEngine = mediaEngine)

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

    actual fun buildQrCode(text: String): QrCodeMatrix? = buildDesktopQrCode(text)
}
