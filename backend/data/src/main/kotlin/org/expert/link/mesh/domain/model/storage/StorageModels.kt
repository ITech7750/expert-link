package org.expert.link.mesh.domain.model.storage

import kotlinx.serialization.Serializable

/** Описание локального файла или платформенного file handle. */
@Serializable
data class LocalFileResource(
    val handle: String,
    val fileName: String,
    val sizeBytes: Long,
    val contentType: String? = null,
)
