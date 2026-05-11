package org.expert.link.mesh.domain.port.external

import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.inventory.InventoryLabel

data class InventoryLabelArtifact(
    val descriptor: FileDescriptor,
    val localPath: String,
)

interface InventoryLabelRendererPort {
    suspend fun renderPdf(label: InventoryLabel, fileName: String): InventoryLabelArtifact
    suspend fun renderPdfBatch(labels: List<InventoryLabel>, fileName: String): InventoryLabelArtifact
}
