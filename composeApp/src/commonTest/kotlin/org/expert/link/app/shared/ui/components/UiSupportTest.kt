package org.expert.link.app.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import org.expert.link.mesh.contract.model.MeshCentralConnectivityMode
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryPrintStatus
import org.expert.link.mesh.contract.model.MeshInventoryReminderStatus
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus
import org.expert.link.mesh.contract.model.MeshInventorySyncStatus

class UiSupportTest {
    @Test
    fun `sync status uses russian labels`() {
        assertEquals("Синхронизировано", MeshInventorySyncStatus.SYNCED.asUiText())
        assertEquals("Ожидает", MeshInventorySyncStatus.PENDING.asUiText())
        assertEquals("Конфликт", MeshInventorySyncStatus.CONFLICTED.asUiText())
    }

    @Test
    fun `reminder status uses russian labels`() {
        assertEquals("Ожидание", MeshInventoryReminderStatus.PENDING.asUiText())
        assertEquals("Отправлено", MeshInventoryReminderStatus.SENT.asUiText())
        assertEquals("Подтверждено", MeshInventoryReminderStatus.ACKNOWLEDGED.asUiText())
        assertEquals("Отменено", MeshInventoryReminderStatus.DISMISSED.asUiText())
    }

    @Test
    fun `incident severity maps to tones`() {
        assertEquals(ChipTone.INFO, MeshInventoryIncidentSeverity.INFO.asTone())
        assertEquals(ChipTone.SUCCESS, MeshInventoryIncidentSeverity.LOW.asTone())
        assertEquals(ChipTone.WARNING, MeshInventoryIncidentSeverity.MEDIUM.asTone())
        assertEquals(ChipTone.ERROR, MeshInventoryIncidentSeverity.HIGH.asTone())
        assertEquals(ChipTone.ERROR, MeshInventoryIncidentSeverity.CRITICAL.asTone())
    }

    @Test
    fun `scan statuses use russian labels`() {
        assertEquals("Объект найден", MeshInventoryScanResultStatus.RESOLVED.asUiText())
        assertEquals("Код неактивен", MeshInventoryScanResultStatus.INACTIVE.asUiText())
        assertEquals("Не найдено", MeshInventoryScanResultStatus.NOT_FOUND.asUiText())
        assertEquals("Неверный код", MeshInventoryScanResultStatus.INVALID.asUiText())
        assertEquals("Ошибка", MeshInventoryScanResultStatus.ERROR.asUiText())
    }

    @Test
    fun `print status maps to ui label and tone`() {
        assertEquals("Запрошено", MeshInventoryPrintStatus.REQUESTED.asUiText())
        assertEquals("PDF готов", MeshInventoryPrintStatus.GENERATED.asUiText())
        assertEquals("Напечатано", MeshInventoryPrintStatus.PRINTED.asUiText())
        assertEquals("Ошибка", MeshInventoryPrintStatus.FAILED.asUiText())
        assertEquals(ChipTone.INFO, MeshInventoryPrintStatus.REQUESTED.asTone())
        assertEquals(ChipTone.SUCCESS, MeshInventoryPrintStatus.GENERATED.asTone())
        assertEquals(ChipTone.SUCCESS, MeshInventoryPrintStatus.PRINTED.asTone())
        assertEquals(ChipTone.ERROR, MeshInventoryPrintStatus.FAILED.asTone())
    }

    @Test
    fun `central connectivity modes use russian labels`() {
        assertEquals("Оффлайн", MeshCentralConnectivityMode.OFFLINE.asUiText())
        assertEquals("Только mesh", MeshCentralConnectivityMode.MESH_ONLY.asUiText())
        assertEquals("Central доступен", MeshCentralConnectivityMode.CENTRAL_AVAILABLE.asUiText())
        assertEquals("Central недоступен", MeshCentralConnectivityMode.CENTRAL_DEGRADED.asUiText())
        assertEquals("Переподключение", MeshCentralConnectivityMode.RECONNECTING.asUiText())
        assertEquals(
            "Нужен разбор конфликта",
            MeshCentralConnectivityMode.CONFLICT_REVIEW_REQUIRED.asUiText(),
        )
    }
}
