package org.expert.link.mesh.contract.config

import kotlinx.serialization.Serializable

/** Режим работы встроенного mesh-узла. */
@Serializable
enum class MeshNodeMode {
    LOCAL_ONLY,
    HYBRID,
}

/** Флаги включения подсистем узла. */
@Serializable
data class MeshFeatureFlags(
    val discoveryEnabled: Boolean = true,
    val relayEnabled: Boolean = false,
    val inMemoryTransport: Boolean = false,
    val inMemoryDiscovery: Boolean = false,
)

/** Настройки повторов отправки. */
@Serializable
data class MeshRetryConfig(
    val pollIntervalMillis: Long = 1_000,
)

/** Настройки передачи файлов. */
@Serializable
data class MeshFileTransferConfig(
    val chunkSizeBytes: Int = 65_536,
    val downloadDirectory: String = "build/secure-mesh/downloads",
)

/** Настройки rendezvous/relay-клиента. */
@Serializable
data class MeshRelayConfig(
    val enabled: Boolean = false,
    val forceRelayLookup: Boolean = false,
    val relayEligible: Boolean = false,
)

/** Статическая подсказка об узле. */
@Serializable
data class MeshStaticPeer(
    val peerId: String,
    val host: String,
    val port: Int,
)

/** Публичная конфигурация запуска встроенного узла. */
@Serializable
data class MeshNodeConfig(
    val displayName: String,
    val bindHost: String,
    val httpPort: Int,
    val discoveryPort: Int,
    val multicastGroup: String,
    val nodeMode: MeshNodeMode = MeshNodeMode.LOCAL_ONLY,
    val featureFlags: MeshFeatureFlags = MeshFeatureFlags(),
    val retry: MeshRetryConfig = MeshRetryConfig(),
    val fileTransfer: MeshFileTransferConfig = MeshFileTransferConfig(),
    val relay: MeshRelayConfig? = null,
    val capabilities: Set<String> = setOf("chat", "file", "call"),
    val staticPeers: List<MeshStaticPeer> = emptyList(),
)
