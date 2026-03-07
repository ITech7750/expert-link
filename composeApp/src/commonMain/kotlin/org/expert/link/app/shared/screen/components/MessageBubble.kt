package org.expert.link.app.shared.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshMessageType

@Composable
fun MessageBubble(
    message: MeshChatMessage,
    isFromCurrentUser: Boolean,
    senderName: String,
    modifier: Modifier = Modifier,
) {
    val bubbleShape = if (isFromCurrentUser) {
        RoundedCornerShape(24.dp, 8.dp, 24.dp, 24.dp)
    } else {
        RoundedCornerShape(8.dp, 24.dp, 24.dp, 24.dp)
    }
    val bubbleColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isFromCurrentUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(senderName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(42.dp))
        }

        Column(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!isFromCurrentUser) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = when (message.messageType) {
                    MeshMessageType.FILE -> "Файл: ${message.body}"
                    MeshMessageType.CALL_EVENT -> "Событие звонка: ${message.body}"
                    else -> message.body
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            Text(
                text = formatMessageTime(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End),
            )
        }

        if (isFromCurrentUser) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(senderName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(42.dp))
        }
    }
}
