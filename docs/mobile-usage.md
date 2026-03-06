# Mobile Usage

`simulator` теперь показывает backend ровно как библиотеку для мобильного приложения.
Во всех примерах используются только:
- `org.expert.link.mesh.backend.MeshBackend`
- `org.expert.link.mesh.contract.api.MeshNode`
- модели из `org.expert.link.mesh.contract.*`

Внутренние пакеты `backend:data`, `backend:engine`, `backend:infra`, `backend:runtime` мобильное приложение не использует.

## Импорты
```kotlin
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.api.MeshFileTransferCommand
import org.expert.link.mesh.contract.api.MeshHangupCallCommand
import org.expert.link.mesh.contract.api.MeshNode
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshRelayConfig
```

## Запуск узла
Запрос:
- `MeshNodeConfig`

Вызов:
```kotlin
val node: MeshNode = MeshBackend.launch(config)
```

Ответ:
- `MeshNode`
- `node.profile`
- `node.endpoint`

## Pairing
Вызовы:
```kotlin
val invite: String = node.createPairingInvite()
val session = node.pairWithInvite(invite)
val peers = node.peers()
```

Модели:
- `String` invite
- `MeshPairingSession`
- `MeshPairedPeer`

## Discovery и routing
Вызовы:
```kotlin
node.announcePresence()
node.discoverPeer(targetPeerId)
val nearby = node.nearbyPeers()
val routes = node.routes()
val plan = node.routingPlan(targetPeerId)
val relay = node.relayStatus()
```

Модели:
- `MeshNearbyPeer`
- `MeshRouteInfo`
- `MeshRoutingPlan`
- `MeshRelayStatus`

## Диалог и сообщения
Вызовы:
```kotlin
val conversation = node.openConversation(targetPeerId)
val outbound = node.sendChat(
    MeshChatCommand(
        targetPeerId = targetPeerId,
        body = "hello",
        conversationId = conversation.conversationId,
    ),
)
val messages = node.messages(conversation.conversationId)
val receipts = node.messageReceipts()
```

Модели:
- `MeshConversation`
- `MeshChatCommand`
- `MeshChatMessage`
- `MeshMessageReceipt`

## Передача файлов
Вызовы:
```kotlin
val transfer = node.sendFile(
    MeshFileTransferCommand(
        targetPeerId = targetPeerId,
        path = filePath,
        conversationId = conversationId,
    ),
)
val transfers = node.fileTransfers()
val resumed = node.resumeFileTransfer(transfer.transferId)
val cancelled = node.cancelFileTransfer(transfer.transferId)
```

Модели:
- `MeshFileTransferCommand`
- `MeshFileTransferSession`

## Signaling звонка
Вызовы:
```kotlin
val call = node.startCall(
    MeshStartCallCommand(
        targetPeerId = targetPeerId,
        offer = sdpOffer,
        conversationId = conversationId,
    ),
)

val signal = node.sendCallSignal(
    MeshCallSignalCommand(
        callId = call.callId,
        recipientPeerId = targetPeerId,
        signalType = MeshCallSignalType.ACCEPTED,
        payload = "ok",
    ),
)

val activeCalls = node.callSessions()
val hangup = node.hangupCall(
    MeshHangupCallCommand(
        callId = call.callId,
        recipientPeerId = targetPeerId,
        reason = "user-ended",
    ),
)
```

Модели:
- `MeshStartCallCommand`
- `MeshCallSignalCommand`
- `MeshHangupCallCommand`
- `MeshCallSession`
- `MeshCallSignal`

## Диагностика
Вызовы:
```kotlin
val events = node.recentEvents(limit = 50)
val metrics = node.metrics()
```

Модели:
- `MeshEventLogEntry`
- `MeshMetricSnapshot`

## Сценарии simulator
Смотрите готовые mobile-style примеры:
- `LifecycleDemoScenario`
- `PairingDemoScenario`
- `DiscoveryDemoScenario`
- `MessagingDemoScenario`
- `RoutingDemoScenario`
- `FileTransferDemoScenario`
- `CallDemoScenario`
- `DiagnosticsDemoScenario`

Запуск:
```bash
./gradlew :simulator:run
```
