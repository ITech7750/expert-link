package org.expert.link.app.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.expert.link.app.shared.platform.BarcodeMatrix
import org.expert.link.app.shared.platform.QrCodeMatrix
import org.expert.link.mesh.contract.model.MeshCallStatus
import org.expert.link.mesh.contract.model.MeshCentralConnectivityMode
import org.expert.link.mesh.contract.model.MeshConnectivityMode
import org.expert.link.mesh.contract.model.MeshEndpointSource
import org.expert.link.mesh.contract.model.MeshEventCategory
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshFileTransferStatus
import org.expert.link.mesh.contract.model.MeshHostRole
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryAcceptanceStatus
import org.expert.link.mesh.contract.model.MeshInventoryConfirmationStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryIncidentStatus
import org.expert.link.mesh.contract.model.MeshInventoryPresenceStatus
import org.expert.link.mesh.contract.model.MeshInventoryReminderStatus
import org.expert.link.mesh.contract.model.MeshInventoryExportStatus
import org.expert.link.mesh.contract.model.MeshInventoryPrintStatus
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus
import org.expert.link.mesh.contract.model.MeshInventorySessionReviewStatus
import org.expert.link.mesh.contract.model.MeshInventorySessionStatus
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventorySyncStatus
import org.expert.link.mesh.contract.model.MeshInventoryWorkflowStatus
import org.expert.link.mesh.contract.model.MeshMessageDeliveryStatus
import org.expert.link.mesh.contract.model.MeshRelayMode
import org.expert.link.mesh.contract.model.MeshRouteMode
import org.expert.link.mesh.contract.model.MeshRouteHealthState
import org.expert.link.mesh.contract.model.MeshTrustState

/** Короткий вид длинного идентификатора. */
fun String.shortId(length: Int = 8): String = if (this.length <= length) this else take(length)

/** Формат времени для UI. */
fun Instant?.asUiTime(): String = this?.toLocalDateTime(TimeZone.currentSystemDefault())?.let {
    "%02d:%02d:%02d".format(it.hour, it.minute, it.second)
} ?: "-"

fun MeshTrustState.asUiText(): String = when (this) {
    MeshTrustState.INVITED -> "Приглашён"
    MeshTrustState.PENDING -> "Ждёт"
    MeshTrustState.TRUSTED -> "Доверен"
    MeshTrustState.BLOCKED -> "Заблокирован"
    MeshTrustState.REVOKED -> "Отозван"
}

fun MeshEndpointSource.asUiText(): String = when (this) {
    MeshEndpointSource.DISCOVERY_MULTICAST,
    MeshEndpointSource.DISCOVERY_BROADCAST,
    -> "Найден в сети"
    MeshEndpointSource.MANUAL_HINT -> "Добавлен вручную"
    MeshEndpointSource.ROUTE_LEARNING -> "Узнан по маршруту"
    MeshEndpointSource.RENDEZVOUS -> "Найден через relay"
}

fun MeshMessageDeliveryStatus.asUiText(): String = when (this) {
    MeshMessageDeliveryStatus.NEW -> "Новый"
    MeshMessageDeliveryStatus.QUEUED -> "В очереди"
    MeshMessageDeliveryStatus.SENT -> "Отправлен"
    MeshMessageDeliveryStatus.ACK_PENDING -> "Ждём"
    MeshMessageDeliveryStatus.DELIVERED -> "Доставлен"
    MeshMessageDeliveryStatus.FAILED -> "Ошибка"
}

fun MeshFileTransferStatus.asUiText(): String = when (this) {
    MeshFileTransferStatus.OFFERED -> "Предложен"
    MeshFileTransferStatus.ACCEPTED -> "Принят"
    MeshFileTransferStatus.IN_PROGRESS -> "Идёт"
    MeshFileTransferStatus.PAUSED -> "Пауза"
    MeshFileTransferStatus.COMPLETED -> "Готово"
    MeshFileTransferStatus.FAILED -> "Ошибка"
    MeshFileTransferStatus.CANCELLED -> "Отменён"
}

fun MeshCallStatus.asUiText(): String = when (this) {
    MeshCallStatus.NEW -> "Новый"
    MeshCallStatus.INVITED -> "Приглашение"
    MeshCallStatus.OUTGOING -> "Исходящий"
    MeshCallStatus.INCOMING -> "Входящий"
    MeshCallStatus.RINGING -> "Звонит"
    MeshCallStatus.ACCEPTED -> "Принят"
    MeshCallStatus.CONNECTING -> "Подключение"
    MeshCallStatus.ACTIVE -> "Разговор"
    MeshCallStatus.CONNECTED -> "Подключён"
    MeshCallStatus.RECONNECTING -> "Переподключение"
    MeshCallStatus.ENDED -> "Завершён"
    MeshCallStatus.REJECTED -> "Отклонён"
    MeshCallStatus.MISSED -> "Пропущен"
    MeshCallStatus.LEFT -> "Вышел"
    MeshCallStatus.FAILED -> "Ошибка"
}

fun MeshRouteMode.asUiText(): String = when (this) {
    MeshRouteMode.LOCAL_DIRECT -> "Напрямую"
    MeshRouteMode.RELAY_FLOOD -> "Через узлы"
    MeshRouteMode.OVERLAY_DIRECT -> "Overlay напрямую"
    MeshRouteMode.RENDEZVOUS_DIRECT_CANDIDATE -> "Кандидат"
    MeshRouteMode.RENDEZVOUS_RELAY -> "Через relay"
}

fun MeshHostRole.asUiText(): String = when (this) {
    MeshHostRole.HOST -> "Хост"
    MeshHostRole.MEMBER -> "Участник"
    MeshHostRole.CANDIDATE -> "Кандидат"
    MeshHostRole.UNKNOWN -> "Не определено"
}

fun MeshConnectivityMode.asUiText(): String = when (this) {
    MeshConnectivityMode.LOCAL_MESH -> "Локальная mesh"
    MeshConnectivityMode.HOST_ROUTED -> "Через хост"
    MeshConnectivityMode.RELAY_PROXY -> "Через relay/proxy"
    MeshConnectivityMode.DEGRADED -> "Деградация"
}

fun MeshCentralConnectivityMode.asUiText(): String = when (this) {
    MeshCentralConnectivityMode.OFFLINE -> "Оффлайн"
    MeshCentralConnectivityMode.MESH_ONLY -> "Только mesh"
    MeshCentralConnectivityMode.CENTRAL_AVAILABLE -> "Central доступен"
    MeshCentralConnectivityMode.CENTRAL_DEGRADED -> "Central недоступен"
    MeshCentralConnectivityMode.RECONNECTING -> "Переподключение"
    MeshCentralConnectivityMode.CONFLICT_REVIEW_REQUIRED -> "Нужен разбор конфликта"
}

fun MeshRelayMode.asUiText(): String = when (this) {
    MeshRelayMode.DISABLED -> "Выключен"
    MeshRelayMode.STANDBY -> "Ожидание"
    MeshRelayMode.ACTIVE_FALLBACK -> "Fallback"
    MeshRelayMode.FORCED -> "Принудительный"
}

fun MeshRouteHealthState.asUiText(): String = when (this) {
    MeshRouteHealthState.HEALTHY -> "Нормально"
    MeshRouteHealthState.DEGRADED -> "Деградация"
    MeshRouteHealthState.STALE -> "Устарел"
    MeshRouteHealthState.FAILED -> "Ошибка"
}

fun MeshEventCategory.asUiText(): String = when (this) {
    MeshEventCategory.DISCOVERY -> "Поиск"
    MeshEventCategory.PAIRING -> "Сопряжение"
    MeshEventCategory.MESSAGING -> "Сообщения"
    MeshEventCategory.FILE_TRANSFER -> "Передачи"
    MeshEventCategory.CALL -> "Звонки"
    MeshEventCategory.SECURITY -> "Безопасность"
    MeshEventCategory.ROUTING -> "Маршруты"
    MeshEventCategory.INVENTORY -> "Инвентаризация"
    MeshEventCategory.SYSTEM -> "Система"
}

fun MeshInventoryStatus.asUiText(): String = when (this) {
    MeshInventoryStatus.DRAFT -> "Черновик"
    MeshInventoryStatus.ADDED -> "Добавлен"
    MeshInventoryStatus.UNDER_REVIEW -> "На проверке"
    MeshInventoryStatus.CONFIRMED -> "Подтверждён"
    MeshInventoryStatus.REJECTED -> "Отклонён"
    MeshInventoryStatus.REQUIRES_UPDATE -> "Нужны правки"
    MeshInventoryStatus.ARCHIVED -> "Архив"
}

fun MeshInventoryStatus.asTone(): ChipTone = when (this) {
    MeshInventoryStatus.CONFIRMED -> ChipTone.SUCCESS
    MeshInventoryStatus.REJECTED -> ChipTone.ERROR
    MeshInventoryStatus.REQUIRES_UPDATE -> ChipTone.WARNING
    MeshInventoryStatus.UNDER_REVIEW -> ChipTone.INFO
    MeshInventoryStatus.DRAFT,
    MeshInventoryStatus.ADDED,
    MeshInventoryStatus.ARCHIVED,
    -> ChipTone.INFO
}

fun MeshInventorySyncStatus.asUiText(): String = when (this) {
    MeshInventorySyncStatus.SYNCED -> "Синхронизировано"
    MeshInventorySyncStatus.PENDING -> "Ожидает"
    MeshInventorySyncStatus.CONFLICTED -> "Конфликт"
}

fun MeshInventorySyncStatus.asTone(): ChipTone = when (this) {
    MeshInventorySyncStatus.SYNCED -> ChipTone.SUCCESS
    MeshInventorySyncStatus.PENDING -> ChipTone.WARNING
    MeshInventorySyncStatus.CONFLICTED -> ChipTone.ERROR
}

fun MeshInventoryIncidentSeverity.asUiText(): String = when (this) {
    MeshInventoryIncidentSeverity.INFO -> "Инфо"
    MeshInventoryIncidentSeverity.LOW -> "Низкая"
    MeshInventoryIncidentSeverity.MEDIUM -> "Средняя"
    MeshInventoryIncidentSeverity.HIGH -> "Высокая"
    MeshInventoryIncidentSeverity.CRITICAL -> "Критичная"
}

fun MeshInventoryIncidentSeverity.asTone(): ChipTone = when (this) {
    MeshInventoryIncidentSeverity.INFO -> ChipTone.INFO
    MeshInventoryIncidentSeverity.LOW -> ChipTone.SUCCESS
    MeshInventoryIncidentSeverity.MEDIUM -> ChipTone.WARNING
    MeshInventoryIncidentSeverity.HIGH -> ChipTone.ERROR
    MeshInventoryIncidentSeverity.CRITICAL -> ChipTone.ERROR
}

fun MeshInventoryIncidentStatus.asUiText(): String = when (this) {
    MeshInventoryIncidentStatus.OPEN -> "Открыт"
    MeshInventoryIncidentStatus.UNDER_REVIEW -> "На проверке"
    MeshInventoryIncidentStatus.RESOLVED -> "Решён"
    MeshInventoryIncidentStatus.DISMISSED -> "Отклонён"
}

fun MeshInventoryReminderStatus.asUiText(): String = when (this) {
    MeshInventoryReminderStatus.PENDING -> "Ожидание"
    MeshInventoryReminderStatus.SENT -> "Отправлено"
    MeshInventoryReminderStatus.ACKNOWLEDGED -> "Подтверждено"
    MeshInventoryReminderStatus.DISMISSED -> "Отменено"
}

fun MeshInventorySessionStatus.asUiText(): String = when (this) {
    MeshInventorySessionStatus.DRAFT -> "Черновик"
    MeshInventorySessionStatus.ACTIVE -> "Активна"
    MeshInventorySessionStatus.UNDER_REVIEW -> "Инвентаризация"
    MeshInventorySessionStatus.CLOSED -> "Закрыта"
    MeshInventorySessionStatus.ARCHIVED -> "Архив"
}

fun MeshInventorySessionReviewStatus.asUiText(): String = when (this) {
    MeshInventorySessionReviewStatus.PENDING -> "Ожидание"
    MeshInventorySessionReviewStatus.IN_PROGRESS -> "В работе"
    MeshInventorySessionReviewStatus.COMPLETED -> "Завершена"
    MeshInventorySessionReviewStatus.REJECTED -> "Отклонена"
}

fun MeshInventoryReviewStatus.asUiText(): String = when (this) {
    MeshInventoryReviewStatus.APPROVED -> "Подтверждено"
    MeshInventoryReviewStatus.REJECTED -> "Отклонено"
    MeshInventoryReviewStatus.REQUIRES_UPDATE -> "Нужны правки"
}

fun MeshInventoryWorkflowStatus.asUiText(): String = when (this) {
    MeshInventoryWorkflowStatus.CREATED -> "Создана"
    MeshInventoryWorkflowStatus.IN_PROGRESS -> "В процессе"
    MeshInventoryWorkflowStatus.PASSED -> "Пройдена"
    MeshInventoryWorkflowStatus.FAILED -> "Не пройдена"
    MeshInventoryWorkflowStatus.REQUIRES_CORRECTION -> "Требует исправления"
    MeshInventoryWorkflowStatus.SENT_TO_COMMISSION -> "Отправлена в комиссию"
    MeshInventoryWorkflowStatus.COMPLETED -> "Завершена"
}

fun MeshInventoryWorkflowStatus.asTone(): ChipTone = when (this) {
    MeshInventoryWorkflowStatus.PASSED,
    MeshInventoryWorkflowStatus.COMPLETED,
    -> ChipTone.SUCCESS
    MeshInventoryWorkflowStatus.FAILED -> ChipTone.ERROR
    MeshInventoryWorkflowStatus.REQUIRES_CORRECTION,
    MeshInventoryWorkflowStatus.SENT_TO_COMMISSION,
    -> ChipTone.WARNING
    MeshInventoryWorkflowStatus.CREATED,
    MeshInventoryWorkflowStatus.IN_PROGRESS,
    -> ChipTone.INFO
}

fun MeshInventoryPresenceStatus.asUiText(): String = when (this) {
    MeshInventoryPresenceStatus.UNCHECKED -> "Не отмечено"
    MeshInventoryPresenceStatus.PRESENT -> "Есть в наличии"
    MeshInventoryPresenceStatus.ABSENT -> "Нет в наличии"
}

fun MeshInventoryAcceptanceStatus.asUiText(): String = when (this) {
    MeshInventoryAcceptanceStatus.UNCHECKED -> "Не отмечено"
    MeshInventoryAcceptanceStatus.ACCEPTED -> "Принят"
    MeshInventoryAcceptanceStatus.NOT_ACCEPTED -> "Не принят"
}

fun MeshInventoryConfirmationStatus.asUiText(): String = when (this) {
    MeshInventoryConfirmationStatus.UNCHECKED -> "Не подтверждён"
    MeshInventoryConfirmationStatus.CONFIRMED -> "Подтверждён"
    MeshInventoryConfirmationStatus.NOT_CONFIRMED -> "Не подтверждён"
}

fun MeshInventoryScanResultStatus.asUiText(): String = when (this) {
    MeshInventoryScanResultStatus.RESOLVED -> "Объект найден"
    MeshInventoryScanResultStatus.INACTIVE -> "Код неактивен"
    MeshInventoryScanResultStatus.NOT_FOUND -> "Не найдено"
    MeshInventoryScanResultStatus.INVALID -> "Неверный код"
    MeshInventoryScanResultStatus.ERROR -> "Ошибка"
}

fun MeshInventoryCondition.asUiText(): String = when (this) {
    MeshInventoryCondition.NEW -> "Новое"
    MeshInventoryCondition.GOOD -> "Хорошее"
    MeshInventoryCondition.FAIR -> "Удовлетворительное"
    MeshInventoryCondition.NEEDS_REPAIR -> "Требует ремонта"
    MeshInventoryCondition.OUT_OF_SERVICE -> "Не используется"
    MeshInventoryCondition.UNKNOWN -> "Неизвестно"
}

fun MeshInventoryExportStatus.asUiText(): String = when (this) {
    MeshInventoryExportStatus.REQUESTED -> "Запрошен"
    MeshInventoryExportStatus.IN_PROGRESS -> "Готовится"
    MeshInventoryExportStatus.COMPLETED -> "Готов"
    MeshInventoryExportStatus.FAILED -> "Ошибка"
    MeshInventoryExportStatus.CANCELLED -> "Отменён"
}

fun MeshInventoryPrintStatus.asUiText(): String = when (this) {
    MeshInventoryPrintStatus.REQUESTED -> "Запрошено"
    MeshInventoryPrintStatus.GENERATED -> "PDF готов"
    MeshInventoryPrintStatus.FAILED -> "Ошибка"
    MeshInventoryPrintStatus.PRINTED -> "Напечатано"
}

fun MeshInventoryPrintStatus.asTone(): ChipTone = when (this) {
    MeshInventoryPrintStatus.REQUESTED -> ChipTone.INFO
    MeshInventoryPrintStatus.GENERATED -> ChipTone.SUCCESS
    MeshInventoryPrintStatus.PRINTED -> ChipTone.SUCCESS
    MeshInventoryPrintStatus.FAILED -> ChipTone.ERROR
}

fun MeshMessageDeliveryStatus.asTone(): ChipTone = when (this) {
    MeshMessageDeliveryStatus.DELIVERED -> ChipTone.SUCCESS
    MeshMessageDeliveryStatus.FAILED -> ChipTone.ERROR
    MeshMessageDeliveryStatus.ACK_PENDING,
    MeshMessageDeliveryStatus.QUEUED,
    MeshMessageDeliveryStatus.SENT,
    MeshMessageDeliveryStatus.NEW,
    -> ChipTone.INFO
}

fun MeshFileTransferStatus.asTone(): ChipTone = when (this) {
    MeshFileTransferStatus.COMPLETED -> ChipTone.SUCCESS
    MeshFileTransferStatus.FAILED,
    MeshFileTransferStatus.CANCELLED,
    -> ChipTone.ERROR
    MeshFileTransferStatus.PAUSED -> ChipTone.WARNING
    MeshFileTransferStatus.OFFERED,
    MeshFileTransferStatus.ACCEPTED,
    MeshFileTransferStatus.IN_PROGRESS,
    -> ChipTone.INFO
}

fun MeshCallStatus.asTone(): ChipTone = when (this) {
    MeshCallStatus.ACTIVE,
    MeshCallStatus.CONNECTED,
    MeshCallStatus.ACCEPTED,
    MeshCallStatus.ENDED,
    -> ChipTone.SUCCESS
    MeshCallStatus.REJECTED,
    MeshCallStatus.MISSED,
    MeshCallStatus.FAILED,
    -> ChipTone.ERROR
    MeshCallStatus.RECONNECTING -> ChipTone.WARNING
    MeshCallStatus.INVITED,
    MeshCallStatus.OUTGOING,
    MeshCallStatus.INCOMING,
    MeshCallStatus.RINGING,
    MeshCallStatus.CONNECTING,
    MeshCallStatus.LEFT,
    MeshCallStatus.NEW,
    -> ChipTone.INFO
}

fun MeshRouteMode.asTone(): ChipTone = when (this) {
    MeshRouteMode.LOCAL_DIRECT -> ChipTone.SUCCESS
    MeshRouteMode.RELAY_FLOOD,
    MeshRouteMode.RENDEZVOUS_RELAY,
    -> ChipTone.WARNING
    MeshRouteMode.OVERLAY_DIRECT,
    MeshRouteMode.RENDEZVOUS_DIRECT_CANDIDATE,
    -> ChipTone.INFO
}

/** Прогресс передачи от 0 до 1. */
fun MeshFileTransferSession.progress(): Float {
    val total = totalChunks.coerceAtLeast(1)
    val completed = when {
        acknowledgedChunks.isNotEmpty() -> acknowledgedChunks.size
        receivedChunks.isNotEmpty() -> receivedChunks.size
        else -> 0
    }
    return (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

enum class ChipTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

@Composable
fun BadgeChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
) {
    val colors = when (tone) {
        ChipTone.INFO -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipTone.SUCCESS -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFDDF5E5),
            labelColor = Color(0xFF1F6B36),
        )
        ChipTone.WARNING -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFFFF0C2),
            labelColor = Color(0xFF8A5A00),
        )
        ChipTone.ERROR -> AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFFFE2DE),
            labelColor = Color(0xFFB42318),
        )
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = text) },
        colors = colors,
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 420.dp
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(18.dp),
        ) {
            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBanner(text: String, tone: ChipTone) {
    val containerColor = when (tone) {
        ChipTone.INFO -> MaterialTheme.colorScheme.surfaceVariant
        ChipTone.SUCCESS -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        ChipTone.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        ChipTone.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    }
    val textColor = when (tone) {
        ChipTone.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        ChipTone.SUCCESS -> MaterialTheme.colorScheme.secondary
        ChipTone.WARNING -> MaterialTheme.colorScheme.tertiary
        ChipTone.ERROR -> MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
fun EmptyState(title: String, text: String) {
    SectionCard(title = title) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LoadingState(title: String, text: String) {
    SectionCard(title = title) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(text)
        }
    }
}

@Composable
fun ResponsiveColumns(
    modifier: Modifier = Modifier,
    breakpoint: Dp = 920.dp,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= breakpoint) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = first)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = second)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                first()
                second()
            }
        }
    }
}

@Composable
fun MonospaceValue(text: String) {
    SelectionContainer {
        Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun QrCodeCard(
    matrix: QrCodeMatrix,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    qrSize: Dp = 220.dp,
) {
    SectionCard(title = title, subtitle = subtitle, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(qrSize)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            val cellSize = minOf(size.width, size.height) / matrix.size.toFloat()
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix.isDark(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize, cellSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeCard(
    matrix: BarcodeMatrix,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    barcodeHeight: Dp = 140.dp,
) {
    SectionCard(title = title, subtitle = subtitle, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .size(width = 320.dp, height = barcodeHeight)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            val cellWidth = size.width / matrix.width.toFloat()
            val cellHeight = size.height / matrix.height.toFloat()
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix.isDark(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellWidth, y * cellHeight),
                            size = Size(cellWidth, cellHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PollingEffect(
    key: Any?,
    enabled: Boolean = true,
    intervalMillis: Long = 2_500,
    action: suspend () -> Unit,
) {
    LaunchedEffect(key, enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            action()
            delay(intervalMillis)
        }
    }
}
