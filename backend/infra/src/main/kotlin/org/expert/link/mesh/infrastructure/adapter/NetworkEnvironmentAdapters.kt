package org.expert.link.mesh.infrastructure.adapter

import org.expert.link.mesh.domain.port.external.MulticastSupportPort
import org.expert.link.mesh.domain.port.external.NetworkEnvironmentPort
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

/** JVM-адаптер сетевого окружения. */
class JvmNetworkEnvironmentAdapter : NetworkEnvironmentPort {
    override suspend fun localAddresses(): List<String> {
        return Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { networkInterface ->
                runCatching { networkInterface.isUp && !networkInterface.isLoopback && !networkInterface.isVirtual }
                    .getOrDefault(false)
            }
            .flatMap { networkInterface -> Collections.list(networkInterface.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .filter(::isRoutableAddress)
            .map { it.hostAddress }
            .distinct()
            .sorted()
    }

    private fun isRoutableAddress(address: InetAddress): Boolean {
        val host = address.hostAddress ?: return false
        return !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isMulticastAddress &&
            !host.startsWith("169.254.") &&
            !host.startsWith("198.18.") &&
            !host.startsWith("198.19.")
    }
}

/** JVM-адаптер поддержки multicast. */
class JvmMulticastSupportAdapter : MulticastSupportPort {
    override suspend fun isMulticastSupported(): Boolean = true

    override suspend fun prepareForMulticast() {
        // В JVM-симуляторе дополнительная подготовка не нужна.
    }
}

/** Заглушка Android-сетевого адаптера. */
class AndroidNetworkEnvironmentAdapter : NetworkEnvironmentPort, MulticastSupportPort {
    override suspend fun localAddresses(): List<String> = emptyList()

    override suspend fun isMulticastSupported(): Boolean = false

    override suspend fun prepareForMulticast() {
        // TODO: На Android здесь нужен MulticastLock на время discovery.
    }
}
