package org.expert.link.app.shared.platform

import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshNodeMode
import org.expert.link.mesh.contract.config.MeshRelayConfig
import org.expert.link.mesh.contract.config.MeshRetryConfig
import kotlin.random.Random

/** Возможности host-платформы, доступные shared UI. */
data class PlatformCapabilities(
    val canCopyText: Boolean = false,
    val canShareText: Boolean = false,
    val canRenderQr: Boolean = false,
    val canScanQr: Boolean = false,
    val canPickFile: Boolean = false,
    val prefersWideLayout: Boolean = false,
)

/** Простое представление QR-кода для отрисовки в Compose. */
data class QrCodeMatrix(
    val size: Int,
    val darkModules: List<Boolean>,
) {
    init {
        require(darkModules.size == size * size) { "Размер QR-матрицы не совпадает с количеством модулей" }
    }

    fun isDark(x: Int, y: Int): Boolean = darkModules[(y * size) + x]
}

/** Платформенные сервисы, которые shared UI получает от host-модуля. */
expect class AppPlatformServices(args: List<String> = emptyList()) {
    val platformName: String
    val transportHint: String
    val capabilities: PlatformCapabilities

    fun defaultConfig(): MeshNodeConfig

    suspend fun launchNode(config: MeshNodeConfig): MeshNode

    /** Копирует текст в системный буфер обмена. */
    suspend fun copyText(label: String, text: String): Result<Unit>

    /** Пытается передать текст через системный share flow. */
    suspend fun shareText(label: String, text: String): Result<Unit>

    /** Открывает системный выбор файла. */
    suspend fun pickFile(): Result<String?>

    /** Строит QR-код для строки invite, если платформа это поддерживает. */
    fun buildQrCode(text: String): QrCodeMatrix?
}

/** Строит базовую конфигурацию узла для demo-клиента. */
fun createDefaultNodeConfig(
    displayNamePrefix: String,
    realTransport: Boolean,
    realDiscovery: Boolean,
): MeshNodeConfig {
    val httpPort = Random.nextInt(18100, 18999)
    val discoveryPort = Random.nextInt(19100, 19999)
    return MeshNodeConfig(
        displayName = "$displayNamePrefix-${httpPort.toString().takeLast(3)}",
        bindHost = "127.0.0.1",
        httpPort = httpPort,
        discoveryPort = discoveryPort,
        multicastGroup = "239.10.10.10",
        nodeMode = MeshNodeMode.LOCAL_ONLY,
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = true,
            relayEnabled = true,
            inMemoryTransport = !realTransport,
            inMemoryDiscovery = !realDiscovery,
        ),
        retry = MeshRetryConfig(),
        fileTransfer = MeshFileTransferConfig(),
        relay = MeshRelayConfig(
            enabled = true,
            forceRelayLookup = false,
            relayEligible = true,
        ),
    )
}
