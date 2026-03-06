package org.expert.link.mesh.infrastructure.adapter

import org.expert.link.mesh.domain.port.external.MulticastSupportPort
import org.expert.link.mesh.domain.port.external.NetworkEnvironmentPort
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/** JVM-адаптер сетевого окружения. */
class JvmNetworkEnvironmentAdapter : NetworkEnvironmentPort {
    override suspend fun localAddresses(): List<String> {
        return Collections.list(NetworkInterface.getNetworkInterfaces())
            .flatMap { Collections.list(it.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress }
            .distinct()
            .sorted()
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
