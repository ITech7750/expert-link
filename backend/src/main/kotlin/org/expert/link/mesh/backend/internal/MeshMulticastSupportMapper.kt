package org.expert.link.mesh.backend.internal

import org.expert.link.mesh.contract.api.MeshMulticastSupport
import org.expert.link.mesh.domain.port.external.MulticastSupportPort

/** Преобразует контрактный boundary multicast в domain port. */
internal fun MeshMulticastSupport.toDomainPort(): MulticastSupportPort = object : MulticastSupportPort {
    override suspend fun isMulticastSupported(): Boolean = this@toDomainPort.isMulticastSupported()

    override suspend fun prepareForMulticast() = this@toDomainPort.prepareForMulticast()

    override suspend fun releaseMulticast() = this@toDomainPort.releaseMulticast()
}
