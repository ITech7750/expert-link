package org.expert.link.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.expert.link.mesh.domain.port.repository.PersistentRepositoryBundle

internal fun createExpertLinkDatabaseBuilder(
    context: Context,
    name: String,
): RoomDatabase.Builder<ExpertLinkDatabase> = Room.databaseBuilder<ExpertLinkDatabase>(
    context = context,
    name = name,
)

fun createPersistentRepositoryBundle(
    context: Context,
    name: String,
): PersistentRepositoryBundle = buildPersistentRepositoryBundle(
    createExpertLinkDatabaseBuilder(
        context = context,
        name = name,
    ),
)
