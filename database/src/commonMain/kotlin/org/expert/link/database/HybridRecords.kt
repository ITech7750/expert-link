package org.expert.link.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "central_auth_session")
data class CentralAuthSessionRecord(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAt: String?,
    val activeOrganizationId: String?,
    val lastAuthenticatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "central_organization_access",
    indices = [
        Index("updatedAt"),
        Index("active"),
    ],
)
data class CentralOrganizationAccessRecord(
    @PrimaryKey val organizationId: String,
    val name: String,
    val active: Boolean,
    val updatedAt: String,
    val payloadJson: String,
)

@Entity(
    tableName = "central_sync_state",
    indices = [
        Index("lastSyncAt"),
        Index("runState"),
    ],
)
data class CentralSyncStateRecord(
    @PrimaryKey val organizationId: String,
    val connectivityMode: String,
    val runState: String,
    val lastUploadedSequence: Long,
    val lastPulledCursor: Long,
    val pendingChanges: Int,
    val conflicts: Int,
    val lastSyncAt: String?,
    val lastError: String?,
    val payloadJson: String,
)

@Entity(
    tableName = "central_organization_workspace",
    indices = [
        Index("fetchedAt"),
    ],
)
data class CentralOrganizationWorkspaceRecord(
    @PrimaryKey val organizationId: String,
    val fetchedAt: String,
    val payloadJson: String,
)
