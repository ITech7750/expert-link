# Mobile Usage

## Используемые модули
Клиентский код использует только:
- `backend`
- `contract`

Внутренние backend-модули `backend:data`, `backend:application`, `backend:infra` и `backend:runtime` напрямую не используются.

## Точка входа
```kotlin
val node = MeshBackend.launch(config)
```

Типы:
- вход: `MeshNodeConfig`
- результат: `MeshNode`

## Основные вызовы
### Профиль
```kotlin
val profile = node.profile
val endpoint = node.endpoint
```

### Pairing
```kotlin
val invite = node.createPairingInvite()
val session = node.pairWithInvite(invite)
val peers = node.peers()
```

### Discovery
```kotlin
node.announcePresence()
node.discoverPeer(peerId)
val nearby = node.nearbyPeers()
val routes = node.routes()
val plan = node.routingPlan(peerId)
```

### Messaging
```kotlin
val conversation = node.openConversation(peerId)
val message = node.sendChat(command)
val messages = node.messages(conversation.conversationId)
val receipts = node.messageReceipts()
```

### Group chat
```kotlin
val created = node.createGroupChat(command)
val groups = node.groupChats()
val events = node.groupEvents(created.conversationId)
val history = node.groupMessages(created.conversationId)
```

### Threads
```kotlin
val thread = node.createThread(command)
val updates = node.threadUpdates(thread.chatId)
val summary = node.threadSummary(thread.chatId, thread.rootMessageId)
val replies = node.threadMessagesDetailed(thread.chatId, thread.rootMessageId)
val sent = node.sendThreadReply(replyCommand)
```

### File transfer
```kotlin
val transfer = node.sendFile(command)
val transfers = node.fileTransfers()
val resumed = node.resumeFileTransfer(transfer.transferId)
val cancelled = node.cancelFileTransfer(transfer.transferId)
```

### Call signaling
```kotlin
val call = node.startCall(command)
val signal = node.sendCallSignal(command)
val sessions = node.callSessions()
val result = node.hangupCall(command)
```

### Diagnostics
```kotlin
val events = node.recentEvents(50)
val metrics = node.metrics()
val relay = node.relayStatus()
```

## Host-модули
- `simulator` показывает сценарии через тот же `MeshNode`.
- `app-shared` использует тот же facade в presentation-слое.
- `app-desktop` и `app-android` добавляют platform services для shared UI.
