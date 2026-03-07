package org.expert.link.app.desktop

import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.platform.PlatformCapabilities
import org.expert.link.app.shared.platform.QrCodeMatrix
import org.expert.link.app.shared.platform.createDefaultNodeConfig
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshNodeConfig

/** Desktop host для запуска backend-узла и desktop UX действий. */
class DesktopPlatformServices(
    private val args: List<String>,
) : AppPlatformServices {
    private val memoryMode: Boolean = args.any { it == "--memory" }

    override val platformName: String = "Рабочий стол"
    override val transportHint: String = if (memoryMode) "Локальная проверка" else "Полная сеть"
    override val capabilities: PlatformCapabilities = PlatformCapabilities(
        canCopyText = true,
        canShareText = false,
        canRenderQr = true,
        canScanQr = false,
        canPickFile = true,
        prefersWideLayout = true,
    )

    override fun defaultConfig(): MeshNodeConfig {
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

    override suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(config)

    override suspend fun copyText(label: String, text: String): Result<Unit> = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    override suspend fun pickFile(): Result<String?> = runCatching {
        val dialog = FileDialog(null as Frame?, "Выберите файл", FileDialog.LOAD)
        dialog.isVisible = true
        val selected = dialog.file ?: return@runCatching null
        File(dialog.directory, selected).absolutePath
    }

    override fun buildQrCode(text: String): QrCodeMatrix? = buildDesktopQrCode(text)
}
