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

### app-shared
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
