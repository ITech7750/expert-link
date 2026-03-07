package org.expert.link.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IdentityDao {
    @Query("SELECT * FROM local_profile WHERE singletonId = 0")
    suspend fun getLocalProfile(): LocalProfileRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalProfile(record: LocalProfileRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeer(record: PairedPeerRecord)

    @Query("SELECT * FROM paired_peer WHERE peerId = :peerId")
    suspend fun findPeer(peerId: String): PairedPeerRecord?

    @Query("SELECT * FROM paired_peer ORDER BY pairedAt ASC")
    suspend fun listPeers(): List<PairedPeerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPairingSession(record: PairingSessionRecord)

    @Query("SELECT * FROM pairing_session WHERE sessionId = :sessionId")
    suspend fun findPairingSession(sessionId: String): PairingSessionRecord?

    @Query(
        """
        SELECT * FROM pairing_session
        WHERE inviteSecret = :inviteSecret AND used = 0 AND expiresAt > :now
        ORDER BY createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun findActivePairingSession(inviteSecret: String, now: String): PairingSessionRecord?

    @Query("SELECT * FROM pairing_session WHERE used = 0 AND expiresAt > :now ORDER BY createdAt DESC")
    suspend fun listActivePairingSessions(now: String): List<PairingSessionRecord>

    @Query("DELETE FROM pairing_session WHERE expiresAt <= :now")
    suspend fun removeExpiredPairingSessions(now: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedPeer(record: BlockedPeerRecord)

    @Query("DELETE FROM blocked_peer WHERE peerId = :peerId")
    suspend fun removeBlockedPeer(peerId: String)

    @Query("SELECT * FROM blocked_peer WHERE peerId = :peerId")
    suspend fun findBlockedPeer(peerId: String): BlockedPeerRecord?

    @Query("SELECT * FROM blocked_peer ORDER BY blockedAt ASC")
    suspend fun listBlockedPeers(): List<BlockedPeerRecord>
}
