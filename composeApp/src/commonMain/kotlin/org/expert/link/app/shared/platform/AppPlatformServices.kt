package org.expert.link.app.shared.platform

import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshNodeMode
import org.expert.link.mesh.contract.config.MeshRelayConfig
import org.expert.link.mesh.contract.config.MeshRetryConfig
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import kotlin.random.Random

/** Возможности host-платформы, доступные shared UI. */
data class PlatformCapabilities(
    val canCopyText: Boolean = false,
    val canShareText: Boolean = false,
    val canShareFiles: Boolean = false,
    val canSaveFiles: Boolean = false,
    val canPrintFiles: Boolean = false,
    val canOpenFiles: Boolean = false,
    val canRenderQr: Boolean = false,
    val canRenderBarcode: Boolean = false,
    val canScanQr: Boolean = false,
    val canSwitchCamera: Boolean = false,
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

/** Простое представление линейного штрихкода для отрисовки в Compose. */
data class BarcodeMatrix(
    val width: Int,
    val height: Int,
    val darkModules: List<Boolean>,
) {
    init {
        require(darkModules.size == width * height) { "Размер barcode-матрицы не совпадает с количеством модулей" }
    }

    fun isDark(x: Int, y: Int): Boolean = darkModules[(y * width) + x]
}

data class InventoryExportDocument(
    val title: String,
    val subtitle: String? = null,
    val summary: List<Pair<String, String>> = emptyList(),
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
)

data class LocalArtifact(
    val path: String,
    val descriptor: MeshFileDescriptor,
)

/** Платформенные сервисы, которые shared UI получает от host-модуля. */
expect class AppPlatformServices(args: List<String> = emptyList()) {
    val platformName: String
    val transportHint: String
    val capabilities: PlatformCapabilities

    fun defaultConfig(): MeshNodeConfig
    fun persistConfig(config: MeshNodeConfig)

    suspend fun launchNode(config: MeshNodeConfig): MeshNode

    /** Копирует текст в системный буфер обмена. */
    suspend fun copyText(label: String, text: String): Result<Unit>

    /** Пытается передать текст через системный share flow. */
    suspend fun shareText(label: String, text: String): Result<Unit>

    /** Пытается передать файл через системный share flow. */
    suspend fun shareFile(label: String, path: String): Result<Unit>

    /** Копирует файл в системную папку загрузок. */
    suspend fun saveFileToDownloads(path: String, fileName: String): Result<String?>

    /** Открывает локальный файл через системное приложение. */
    suspend fun openFile(path: String): Result<Unit>

    /** Сохраняет локальную копию файла в управляемое хранилище приложения. */
    suspend fun cacheLocalArtifact(path: String, fileName: String): Result<String>

    /** Формирует локальный файл отчёта/экспорта по данным инвентаризации. */
    suspend fun createInventoryExportArtifact(
        document: InventoryExportDocument,
        format: MeshInventoryExportFormat,
        suggestedFileName: String,
    ): Result<LocalArtifact>

    /** Пытается найти локальный artifact по имени файла в рабочих каталогах приложения. */
    suspend fun resolveLocalArtifactPath(fileName: String): Result<String?>

    /** Печатает файл через системный print dialog, если поддерживается платформой. */
    suspend fun printFile(path: String): Result<Unit>

    /** Открывает системный выбор файла. */
    suspend fun pickFile(): Result<String?>

    /** Описывает локальный файл для последующего вложения. */
    suspend fun describeFile(path: String): Result<MeshFileDescriptor>

    /** Запускает сканирование QR-кода и возвращает считанную строку. */
    suspend fun scanQr(): Result<String?>

    /** Строит QR-код для строки invite, если платформа это поддерживает. */
    fun buildQrCode(text: String): QrCodeMatrix?

    /** Строит barcode-представление для строки маркировки. */
    fun buildBarcode(text: String): BarcodeMatrix?

    /** Делится QR-кодом как изображением PNG. */
    suspend fun shareQrImage(label: String, text: String): Result<Unit>
}

/** Строит базовую конфигурацию узла для demo-клиента. */
fun createDefaultNodeConfig(
    displayNamePrefix: String,
    realTransport: Boolean,
    realDiscovery: Boolean,
): MeshNodeConfig {
    val defaultDiscoveryPort = 19_100
    val defaultMulticastGroup = "239.60.60.60"
    val httpPort = Random.nextInt(18100, 18999)
    return MeshNodeConfig(
        displayName = "$displayNamePrefix-${httpPort.toString().takeLast(3)}",
        bindHost = "0.0.0.0",
        httpPort = httpPort,
        discoveryPort = defaultDiscoveryPort,
        multicastGroup = defaultMulticastGroup,
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
