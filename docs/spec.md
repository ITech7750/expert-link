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
