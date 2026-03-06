package org.expert.link.mesh.bootstrap.config

import kotlinx.serialization.Serializable

/** Режим работы узла. */
@Serializable
enum class NodeMode {
    LOCAL_ONLY,
    HYBRID,
}

/** Флаги включения подсистем. */
@Serializable
data class FeatureFlags(
    val discoveryEnabled: Boolean = true,
    val relayEnabled: Boolean = false,
    val inMemoryTransport: Boolean = false,
    val inMemoryDiscovery: Boolean = false,
)

/** Настройки повтора отправки. */
@Serializable
data class RetrySettings(
    val pollIntervalMillis: Long = 1_000,
)

/** Настройки передачи файлов. */
@Serializable
data class FileTransferSettings(
    val chunkSizeBytes: Int = 65_536,
    val downloadDirectory: String = "build/secure-mesh/downloads",
)

/** Настройки rendezvous/relay-клиента. */
@Serializable
data class RelayClientSettings(
    val enabled: Boolean = false,
    val forceRelayLookup: Boolean = false,
    val relayEligible: Boolean = false,
)

/** Статический endpoint узла. */
@Serializable
data class StaticPeerConfig(
    val peerId: String,
    val host: String,
    val port: Int,
)

/** Конфигурация runtime одного узла. */
@Serializable
data class NodeConfiguration(
    val displayName: String,
    val bindHost: String,
    val httpPort: Int,
    val discoveryPort: Int,
    val multicastGroup: String,
    val nodeMode: NodeMode = NodeMode.LOCAL_ONLY,
    val featureFlags: FeatureFlags = FeatureFlags(),
    val retrySettings: RetrySettings = RetrySettings(),
    val fileTransferSettings: FileTransferSettings = FileTransferSettings(),
    val relayClientSettings: RelayClientSettings? = null,
    val capabilities: Set<String> = setOf("chat", "file", "call"),
    val staticPeers: List<StaticPeerConfig> = emptyList(),
)
