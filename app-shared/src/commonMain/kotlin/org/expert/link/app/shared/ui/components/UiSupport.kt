package org.expert.link.app.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import org.expert.link.app.shared.platform.QrCodeMatrix
import org.expert.link.mesh.contract.model.MeshCallStatus
import org.expert.link.mesh.contract.model.MeshEndpointSource
import org.expert.link.mesh.contract.model.MeshEventCategory
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshFileTransferStatus
import org.expert.link.mesh.contract.model.MeshMessageDeliveryStatus
import org.expert.link.mesh.contract.model.MeshRouteMode
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
    MeshCallStatus.RINGING -> "Звонит"
    MeshCallStatus.ACTIVE -> "Разговор"
    MeshCallStatus.ENDED -> "Завершён"
    MeshCallStatus.REJECTED -> "Отклонён"
    MeshCallStatus.FAILED -> "Ошибка"
}

fun MeshRouteMode.asUiText(): String = when (this) {
    MeshRouteMode.LOCAL_DIRECT -> "Напрямую"
    MeshRouteMode.RELAY_FLOOD -> "Через узлы"
    MeshRouteMode.OVERLAY_DIRECT -> "Overlay"
    MeshRouteMode.RENDEZVOUS_DIRECT_CANDIDATE -> "Кандидат"
    MeshRouteMode.RENDEZVOUS_RELAY -> "Через relay"
}

fun MeshEventCategory.asUiText(): String = when (this) {
    MeshEventCategory.DISCOVERY -> "Поиск"
    MeshEventCategory.PAIRING -> "Сопряжение"
    MeshEventCategory.MESSAGING -> "Сообщения"
    MeshEventCategory.FILE_TRANSFER -> "Передачи"
    MeshEventCategory.CALL -> "Звонки"
    MeshEventCategory.SECURITY -> "Безопасность"
    MeshEventCategory.ROUTING -> "Маршруты"
    MeshEventCategory.SYSTEM -> "Система"
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
    MeshCallStatus.ENDED,
    -> ChipTone.SUCCESS
    MeshCallStatus.REJECTED,
    MeshCallStatus.FAILED,
    -> ChipTone.ERROR
    MeshCallStatus.INVITED,
    MeshCallStatus.RINGING,
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipTone.SUCCESS -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
            labelColor = MaterialTheme.colorScheme.secondary,
        )
        ChipTone.WARNING -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
            labelColor = MaterialTheme.colorScheme.tertiary,
        )
        ChipTone.ERROR -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            labelColor = MaterialTheme.colorScheme.error,
        )
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = text) },
        colors = colors,
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
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
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
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun EmptyState(title: String, text: String) {
    SectionCard(title = title) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
