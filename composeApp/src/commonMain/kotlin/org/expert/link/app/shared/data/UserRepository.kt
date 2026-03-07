package org.expert.link.app.shared.data

import kotlinx.coroutines.flow.Flow
import org.expert.link.mesh.contract.model.MeshPairedPeer

interface UserRepository {
    fun observeCurrentUser(): Flow<MeshPairedPeer?>
    fun observeContacts(): Flow<List<MeshPairedPeer>>
    suspend fun getUserInfo(peerId: String): Result<MeshPairedPeer>
}