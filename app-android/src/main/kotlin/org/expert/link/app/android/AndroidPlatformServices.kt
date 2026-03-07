package org.expert.link.app.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.expert.link.app.shared.platform.AppPlatformServices
import org.expert.link.app.shared.platform.PlatformCapabilities
import org.expert.link.app.shared.platform.QrCodeMatrix
import org.expert.link.app.shared.platform.createDefaultNodeConfig
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshNodeConfig

/** Android host для Compose клиента. */
class AndroidPlatformServices(
    private val context: Context,
) : AppPlatformServices {
    private val mediaEngine by lazy { AndroidWebRtcMediaEngineAdapter(context) }

    override val platformName: String = "Android"
    override val transportHint: String = "Безопасный режим"
    override val capabilities: PlatformCapabilities = PlatformCapabilities(
        canCopyText = true,
        canShareText = true,
        canRenderQr = true,
        canScanQr = false,
        canPickFile = false,
        prefersWideLayout = false,
    )

    override fun defaultConfig(): MeshNodeConfig = createDefaultNodeConfig(
        displayNamePrefix = "android",
        realTransport = false,
        realDiscovery = false,
    )

    override suspend fun launchNode(config: MeshNodeConfig): MeshNode = MeshBackend.launch(config, mediaEngine = mediaEngine)

    override suspend fun copyText(label: String, text: String): Result<Unit> = runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override suspend fun shareText(label: String, text: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun buildQrCode(text: String): QrCodeMatrix? = buildAndroidQrCode(text)
}
