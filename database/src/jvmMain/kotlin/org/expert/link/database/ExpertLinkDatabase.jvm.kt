package org.expert.link.database

import androidx.room.Room
import androidx.room.RoomDatabase
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle

internal fun createExpertLinkDatabaseBuilder(
    path: String,
): RoomDatabase.Builder<ExpertLinkDatabase> = Room.databaseBuilder<ExpertLinkDatabase>(
    name = path,
)

fun createPersistentRepositoryBundle(
    path: String,
): PersistentRepositoryBundle = buildPersistentRepositoryBundle(
    createExpertLinkDatabaseBuilder(path),
)
