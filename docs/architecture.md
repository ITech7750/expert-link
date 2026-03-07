# Architecture

## Модули
- `contract` — публичный API (`MeshNode`), команды и DTO.
- `backend` — публичный facade, адаптирует contract-вызовы к runtime/application.
- `backend:data` — domain model, entity, port.
- `backend:application` — use-case сервисы, factory и mapper.
- `backend:infra` — реализации port (adapter/client/repository).
- `backend:runtime` — bootstrap и controller.
- `simulator` — сценарии использования только через `MeshNode`.
- `app-shared` — presentation и Compose UI, работает только с `contract`.

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
- `app-shared` использует только `MeshNode` и contract-модели.
- Внутренние `backend:data/application/infra/runtime` классы в UI/Simulator напрямую не используются.

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

### Signaling поток
1. `MeshNode` вызывает call-метод контракта.
2. `backend:MeshBackend` делегирует в `NodeLifecycleService`.
3. `NodeLifecycleService` делегирует в `CallSignalingService`.
4. `CallSignalingService` формирует `PacketEnvelope` c payload `CALL_INVITE`/`CALL_SIGNAL`/`CALL_HANGUP`.
5. На принимающей стороне `PacketController -> NodeLifecycleService` вызывает `handleInvite`/`handleSignal`/`handleHangup`.
6. Состояния, участники и события сохраняются через call repository ports.

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

Legacy методы `startCall`, `sendCallSignal`, `hangupCall` оставлены для совместимости.
