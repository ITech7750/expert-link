package org.expert.link.mesh.domain.entity

import kotlinx.serialization.Serializable

/** Сущность хранения звонка. */
@Serializable
data class CallSessionEntity(
    val callId: String,
    val conversationId: String? = null,
    val initiatorPeerId: String,
    val recipientPeerId: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val lastSignalAt: String? = null,
    val qualitySnapshotJson: String? = null,
)
