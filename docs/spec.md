# Specification

## Реализованные функции messaging

### Личные чаты
- создание/открытие direct-чата;
- отправка и приём зашифрованных сообщений;
- ACK и обновление delivery status.

### Групповые чаты
- создание группы (`createGroupChat`);
- переименование (`renameChat`);
- добавление/удаление участников (`addParticipants`, `removeParticipant`);
- чтение списка групп (`groupChats`);
- чтение сообщений группы (`groupMessages`);
- чтение системных событий группы (`groupEvents`).

### Треды
- создание треда по root-сообщению (`createThread`);
- отправка reply в тред (`sendThreadReply`, `sendThreadMessage`);
- чтение метаданных треда (`thread`, `threadSummary`);
- чтение обновлений тредов чата (`threadUpdates`);
- чтение истории треда (`threadMessagesDetailed`, `threadMessages`).
- `threadUpdates` работает и на принимающей стороне за счёт агрегации reply из message storage, даже если тред не создавался локально.

### Сводки
- `chatSummaries` возвращает краткие карточки для списка чатов.

## Domain модели

### Базовые
- `Conversation`
- `ChatMessage`
- `MessageReceipt`

### Группы
- `GroupChat`
- `GroupChatEvent`
- `GroupEventType`
- `ChatSummary`

### Треды
- `ChatThread`
- `ThreadMessage`
- `ThreadSummary`

## Хранение

### Persistent storage ports
- `ConversationRepositoryPort`
- `MessageRepositoryPort`
- `GroupChatRepositoryPort`
- `ChatMemberRepositoryPort`
- `ThreadRepositoryPort`
- `ThreadMessageRepositoryPort`
- `GroupEventRepositoryPort`

### In-memory implementations
- `InMemoryConversationRepositoryAdapter`
- `InMemoryMessageRepositoryAdapter`
- `InMemoryGroupChatRepositoryAdapter`
- `InMemoryChatMemberRepositoryAdapter`
- `InMemoryThreadRepositoryAdapter`
- `InMemoryThreadMessageRepositoryAdapter`
- `InMemoryGroupEventRepositoryAdapter`

## Application сервисы
- `ChatMessagingService` — транспорт сообщений, ACK, thread-aware отправка и перенос метаданных группового чата в payload.
- `GroupChatService` — lifecycle группы и события.
- `ThreadService` — lifecycle тредов и история reply.

## Contract API (добавлено)

### Модели
- `MeshGroupChat`
- `MeshGroupEvent`
- `MeshThread`
- `MeshThreadMessage`
- `MeshChatSummary`

### Методы `MeshNode`
- `groupChats`
- `groupEvents`
- `groupMessages`
- `chatSummaries`
- `createThread`
- `thread`
- `threadUpdates`
- `threadMessagesDetailed`
- `sendThreadReply`

## Интеграция

### Simulator
Сценарий `GroupThreadDemoScenario` демонстрирует:
1. создание группы;
2. добавление участника;
3. отправку сообщения в группу;
4. создание треда;
5. отправку reply;
6. чтение истории и событий.

### composeApp/commonMain
Добавлены:
- группы и треды в presentation stores;
- маршруты `Group` и `Thread`;
- экраны группы и треда;
- thread navigation из экрана чата.

## Call subsystem v2

### Типы звонков
- `startAudioCall` — 1:1 аудио;
- `startVideoCall` — 1:1 видео;
- `startGroupAudioCall` — групповой аудио;
- `startGroupVideoCall` — групповой видео.

### Lifecycle операции
- `acceptCall`
- `rejectCall`
- `joinCall`
- `leaveCall`
- `endCall`

### Наблюдение за состоянием
- `callSessions`
- `observeActiveCall`
- `observeIncomingCalls`
- `observeCallParticipants`
- `observeCallEvents`
- `observeMediaState`
- `observeMediaStats`

### Управление media
- `toggleMicrophone`
- `toggleCamera`
- `switchCamera`

### Состояния звонка
`CallState`:
- `NEW`
- `INVITED`
- `OUTGOING`
- `INCOMING`
- `RINGING`
- `ACCEPTED`
- `CONNECTING`
- `ACTIVE`
- `CONNECTED`
- `RECONNECTING`
- `ENDED`
- `REJECTED`
- `FAILED`
- `MISSED`
- `LEFT`

### Сигналы
`CallSignalType`:
- `INVITE`
- `ACCEPT`
- `REJECT`
- `JOIN`
- `LEAVE`
- `HANGUP`
- `SDP_OFFER`
- `SDP_ANSWER`
- `ICE_CANDIDATE`
- `RINGING`
- `ACCEPTED`
- `REJECTED`
- `QUALITY`
- `MUTE_CHANGED`
- `VIDEO_CHANGED`
- `PARTICIPANT_STATE`
- `RECONNECTING`
- `RECONNECTED`

### Domain модели
- `CallSession`
- `CallRoom`
- `CallParticipant`
- `CallInvitation`
- `CallEvent`
- `CallMediaState`
- `CallMediaStats`
- `SessionDescription`
- `IceCandidate`

### Storage ports
- `CallSessionRepositoryPort`
- `CallRoomRepositoryPort`
- `CallParticipantRepositoryPort`
- `CallEventRepositoryPort`

### In-memory реализации
- `InMemoryCallSessionRepositoryAdapter`
- `InMemoryCallRoomRepositoryAdapter`
- `InMemoryCallParticipantRepositoryAdapter`
- `InMemoryCallEventRepositoryAdapter`

### Совместимость API
Legacy-методы `startCall`, `sendCallSignal`, `hangupCall` сохранены как совместимый слой поверх call v2.

## WebRTC media integration

### Domain media ports
- `MediaEnginePort`
- `WebRtcSessionPort`
- `AudioCapturePort`
- `VideoCapturePort`
- `MediaRendererPort`

### Contract media API
- `MeshMediaEngine`
- `MeshWebRtcSession`
- модели: `MeshCallMediaState`, `MeshMediaStats`, `MeshSessionDescription`, `MeshIceCandidate`, `MeshWebRtcSignalEvent`

### Runtime поведение
1. `CallMediaService` связывает signaling (`CallSignalingService`) и media backend.
2. В исходящем звонке backend создаёт invite и затем отправляет `SDP_OFFER`.
3. Входящие `SDP_ANSWER` и `ICE_CANDIDATE` применяются к `WebRtcSessionPort`.
4. `toggleMicrophone`/`toggleCamera` меняют local media state и рассылают `MUTE_CHANGED`/`VIDEO_CHANGED`.

### Platform поддержка
- Android: `AndroidWebRtcMediaEngineAdapter` реализует реальный WebRTC (`PeerConnection`, SDP/ICE, аудио/видео tracks, stats).
- Desktop: `DesktopWebRtcMediaEngineAdapter` оставлен boundary-адаптером (`isSupported=false`).

## Topology и host/failover

### Topology модели
- `NetworkTopologyState`
- `NetworkRoleState`
- `HostRole`
- `HostCandidate`
- `RouteHealth`
- `RouteHealthState`
- `ConnectivityMode`
- `ConnectivityStrategy`
- `RelayMode`
- `TopologyEvent`

### Contract модели
- `MeshTopologyState`
- `MeshNetworkRoleState`
- `MeshHostRole`
- `MeshHostCandidate`
- `MeshRouteHealth`
- `MeshRouteHealthState`
- `MeshConnectivityStrategy`
- `MeshConnectivityMode`
- `MeshRelayMode`
- `MeshTopologyEvent`

### Contract API
`MeshNode`:
- `observeTopologyState`
- `observeHostRole`
- `observeConnectivityStrategy(peerId)`
- `inspectRouteHealth`
- `relayModeState`
- `forceTopologyRefresh`

### Поведение failover
1. Потеря хоста (`onPeerLost`) помечает host candidate как unreachable.
2. Маршрут к потерянному узлу инвалидируется.
3. `TopologyStateService` перевыбирает хост и выставляет `failoverInProgress`.
4. После стабилизации маршрутов состояние переключается на новый `currentHostPeerId`.
5. При восстановлении continuity формируется `CONTINUITY_RECOVERED`.

### Поведение relay/proxy fallback
1. `ConnectivityStrategyService` выбирает стратегию доставки.
2. При наличии rendezvous+relay выбирается `RENDEZVOUS_RELAY`.
3. Успешная relay-доставка переводит `RelayMode` в `ACTIVE_FALLBACK`.
4. Snapshot topology отражает активную relay-стратегию и route health.

### Continuity tracking
`TopologyStateService` учитывает:
- pending ACK;
- outgoing queue;
- активные file transfer;
- активные call sessions;
- failed routes;
- host failover.

Если есть деградация, `continuityDegraded=true` и создаётся событие `CONTINUITY_DEGRADED`.
