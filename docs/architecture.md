# Architecture

## Модули
- `contract` — публичный API (`MeshNode`), команды и DTO.
- `backend` — публичный facade, адаптирует contract-вызовы к runtime/application.
- `backend:data` — domain model, entity, port.
- `backend:application` — use-case сервисы, factory и mapper.
- `backend:infra` — реализации port (adapter/client/repository).
- `backend:runtime` — bootstrap и controller.
- `simulator` — сценарии использования только через `MeshNode`.
- `composeApp` — единый KMP client module:
  - `commonMain` — Compose UI, navigation, presentation;
  - `jvmMain` — Desktop entry point и desktop actual-сервисы;
  - `androidMain` — Android entry point и android actual-сервисы.

## ComposeApp структура
- `composeApp/commonMain`
  - `org.expert.link.app.shared.ui.*` — экранный UI и навигация;
  - `org.expert.link.app.shared.presentation.*` — stores и состояние экранов;
  - `org.expert.link.app.shared.platform.AppPlatformServices` — `expect`-контракт платформенных сервисов.
- `composeApp/jvmMain`
  - `org.expert.link.app.desktop.DesktopMainKt` — Desktop entry point;
  - `org.expert.link.app.shared.platform.AppPlatformServices.jvm.kt` — `actual` desktop-реализация.
- `composeApp/androidMain`
  - `org.expert.link.app.android.MainActivity` — Android entry point;
  - `org.expert.link.app.shared.platform.AppPlatformServices.android.kt` — `actual` Android-реализация.

## Слои backend
- `controller` (`backend/runtime: org.expert.link.mesh.controller`) принимает пакет и передаёт в lifecycle.
- `application` (`backend/application`) оркестрирует use-case.
- `domain` (`backend/data`) содержит модели и порты.
- `infrastructure` (`backend/infra`) реализует порты.

Поток вызова: `controller -> application -> domain <-> infrastructure`.

## Messaging подсистема

### Domain модели
- Базовые: `Conversation`, `ChatMessage`, `MessageReceipt`.
- Группы: `GroupChat`, `GroupChatEvent`, `GroupEventType`, `ChatSummary`.
- Треды: `ChatThread`, `ThreadMessage`, `ThreadSummary`.

### Repository ports
- Базовые: `ConversationRepositoryPort`, `MessageRepositoryPort`.
- Группы/треды:
  - `GroupChatRepositoryPort`
  - `ChatMemberRepositoryPort`
  - `GroupEventRepositoryPort`
  - `ThreadRepositoryPort`
  - `ThreadMessageRepositoryPort`

### Application сервисы
- `ChatMessagingService` — отправка/приём сообщений, ACK, thread-aware payload.
- `GroupChatService` — lifecycle группы (create/rename/add/remove), события группы, chat summaries.
- `ThreadService` — lifecycle треда (create/reply/history), синхронизация reply count.

### Синхронизация групп и тредов между узлами
- `ChatMessagePayload` передаёт `chatType`, `chatTitle`, `chatDescription`, `participantPeerIds`.
- При получении `ChatMessagingService` создаёт/обновляет `Conversation` как `GROUP`, если payload указывает групповой чат.
- `ThreadService.threadSummaries` агрегирует данные из `ThreadRepositoryPort` и `MessageRepositoryPort`, поэтому треды видны даже на узлах, где метаданные треда не создавались локально.

### Infrastructure реализации
- `InMemoryConversationRepositoryAdapter`
- `InMemoryMessageRepositoryAdapter`
- `InMemoryGroupChatRepositoryAdapter`
- `InMemoryChatMemberRepositoryAdapter`
- `InMemoryGroupEventRepositoryAdapter`
- `InMemoryThreadRepositoryAdapter`
- `InMemoryThreadMessageRepositoryAdapter`

## Runtime wiring

`MeshNodeBootstrap` создаёт и связывает:
1. in-memory repository adapters;
2. application services (`ChatMessagingService`, `GroupChatService`, `ThreadService`);
3. transport/discovery/crypto adapters;
4. `NodeLifecycleService` и packet controllers.

`MeshNodeComponents` хранит ссылки на сервисы и репозитории, используемые facade `backend:MeshBackend`.

## Публичный contract для групп/тредов

`MeshNode` дополнен методами:
- группы: `groupChats`, `groupEvents`, `groupMessages`, `chatSummaries`;
- треды: `createThread`, `thread`, `threadUpdates`, `threadMessagesDetailed`, `sendThreadReply`;
- совместимость: существующие `conversations`, `messages`, `sendThreadMessage`, `threadMessages`.

## App/Simulator интеграция
- `simulator` использует только `MeshBackend.launch` и `MeshNode`.
- `composeApp/commonMain` использует только `MeshNode` и contract-модели.
- Внутренние `backend:data/application/infra/runtime` классы в UI/Simulator напрямую не используются.

## Topology и Connectivity подсистема

### Domain модели
- `NetworkTopologyState` — полный runtime-снимок топологии.
- `NetworkRoleState` — роль узла и текущий выбранный хост.
- `HostRole`, `HostCandidate` — модель хоста и кандидатов.
- `RouteHealth`, `RouteHealthState` — состояние маршрутов.
- `ConnectivityMode`, `ConnectivityStrategy` — стратегия связности.
- `RelayMode` — состояние relay/proxy fallback.
- `TopologyEvent` / `TopologyEventType` — события перестройки сети.

### Application сервисы
- `TopologyStateService`:
  - агрегирует topology snapshot;
  - отслеживает host election/failover;
  - фиксирует route health и continuity degradation/recovery;
  - возвращает стратегию связности и relay mode.
- `RoutingService`:
  - обучает маршруты и передаёт изменения в `TopologyStateService`;
  - инвалидирует маршруты при деградации доставки.
- `DiscoveryOrchestrationService`:
  - передаёт события обнаружения/потери узлов в `TopologyStateService`.
- `RelayService` и `DeliveryTrackingService`:
  - передают результат доставки для обновления route health и relay state.
- `ConnectivityStrategyService`:
  - выбирает стратегию (`LOCAL_DIRECT` / `RELAY_FLOOD` / `RENDEZVOUS_*`);
  - хранит snapshots активных стратегий для topology.

### Runtime wiring
- `MeshNodeBootstrap` создаёт `TopologyStateService` и передаёт его в:
  - `RoutingService`
  - `DiscoveryOrchestrationService`
  - `RelayService`
  - `DeliveryTrackingService`
  - `NodeLifecycleService`
- `NodeLifecycleService.start()` инициализирует topology с локальным endpoint и relay mode из config.
- `rememberPeerEndpoint`/`forgetPeerEndpoint` обновляют topology state.

### Contract API
`MeshNode` предоставляет topology-интерфейс:
- `observeTopologyState`
- `observeHostRole`
- `observeConnectivityStrategy`
- `inspectRouteHealth`
- `relayModeState`
- `forceTopologyRefresh`

### Инфраструктура relay/proxy
- `RendezvousRelayClient` реализует:
  - регистрацию/heartbeat/lookup/unregister;
  - relay candidate selection;
  - stale cleanup для registration записей;
  - relay-aware lookup (`relayOnly`, `directEndpoints`, `relayCandidates`).

## Call подсистема (v2)

### Domain модели
- `CallSession`, `CallRoom`, `CallParticipant`, `CallInvitation`.
- `CallState`, `CallType`, `CallScope`, `CallParticipantState`.
- `CallSignal`, `CallEvent`.

### Repository ports
- `CallSessionRepositoryPort`
- `CallRoomRepositoryPort`
- `CallParticipantRepositoryPort`
- `CallEventRepositoryPort`

### Application сервис
- `CallSignalingService`:
  - запускает direct/group звонки (`audio`/`video`);
  - обрабатывает `invite`, `accept/reject`, `join/leave`, `hangup`;
  - ведёт state machine звонка;
  - сохраняет участников и события.
- `CallMediaService`:
  - управляет WebRTC media lifecycle;
  - создаёт/закрывает media-сессии по `callId`;
  - применяет входящие `SDP/ICE` к media session;
  - отправляет исходящие `SDP/ICE` через `CallSignalingService`;
  - синхронизирует `mute/camera` состояния.

### Signaling поток
1. `MeshNode` вызывает call-метод контракта.
2. `backend:MeshBackend` делегирует в `NodeLifecycleService`.
3. `NodeLifecycleService` делегирует в `CallSignalingService`.
4. `CallSignalingService` формирует `PacketEnvelope` c payload `CALL_INVITE`/`CALL_SIGNAL`/`CALL_HANGUP`.
5. На принимающей стороне `PacketController -> NodeLifecycleService` вызывает `handleInvite`/`handleSignal`/`handleHangup`.
6. Состояния, участники и события сохраняются через call repository ports.

### Media поток (WebRTC)
1. `MeshNode.start*Call` вызывает `NodeLifecycleService.start*Call`.
2. `NodeLifecycleService` делегирует в `CallMediaService`.
3. `CallMediaService` запускает signaling invite через `CallSignalingService`.
4. При наличии media backend (`MediaEnginePort.isSupported=true`) открывается `WebRtcSessionPort`.
5. `SDP_OFFER/SDP_ANSWER/ICE_CANDIDATE` конвертируются в `CALL_SIGNAL` пакеты.
6. На принимающей стороне `CallMediaService.handleSignal` применяет SDP/ICE к локальной media session.
7. `toggleMicrophone`/`toggleCamera`/`switchCamera` обновляют media state и синхронизируются signaling-сигналами.

### Infrastructure реализации
- `InMemoryCallSessionRepositoryAdapter`
- `InMemoryCallRoomRepositoryAdapter`
- `InMemoryCallParticipantRepositoryAdapter`
- `InMemoryCallEventRepositoryAdapter`

### Contract API
`MeshNode` поддерживает:
- запуск: `startAudioCall`, `startVideoCall`, `startGroupAudioCall`, `startGroupVideoCall`;
- действия: `acceptCall`, `rejectCall`, `joinCall`, `leaveCall`, `endCall`;
- чтение состояний: `observeActiveCall`, `observeIncomingCalls`, `observeCallParticipants`, `observeCallEvents`.
- media control: `toggleMicrophone`, `toggleCamera`, `switchCamera`.
- media snapshots: `observeMediaState`, `observeMediaStats`.

Legacy методы `startCall`, `sendCallSignal`, `hangupCall` оставлены для совместимости.

### Platform media adapters
- Android host: `AndroidWebRtcMediaEngineAdapter` (реальный WebRTC backend через `org.webrtc:google-webrtc`).
- Desktop host: `DesktopWebRtcMediaEngineAdapter` (`isSupported=false`, signaling работает, media backend не подключён).
