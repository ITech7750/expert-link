# Приложения, схемы и диаграммы

## Перечень рекомендуемых приложений

### Приложение 1. Компонентная архитектура
Использовать в технической презентации и пояснительной записке.

```mermaid
flowchart LR
    subgraph Devices["Клиентские устройства"]
        M["Мобильный клиент"]
        D["Desktop-клиент"]
    end

    subgraph Edge["Локальный контур узла"]
        UI["UI и workflow"]
        INV["Инвентаризация и маркировка"]
        COMM["Чаты, файлы, звонки"]
        DB["Локальное хранилище"]
        Q["Очередь синхронизации"]
        MESH["Mesh-модуль"]
    end

    subgraph Central["Центральный контур"]
        AUTH["Доступ и организации"]
        CINV["Центральный inventory-контур"]
        SYNC["Сервис синхронизации"]
        ATT["Вложения и экспорт"]
    end

    M --> UI
    D --> UI
    UI --> INV
    UI --> COMM
    INV --> DB
    COMM --> DB
    DB --> Q
    MESH --> COMM
    MESH --> INV
    Q --> SYNC
    SYNC --> CINV
    UI --> AUTH
    UI --> ATT
```

### Приложение 2. Режимы работы online/offline
Использовать в бизнесовой и технической презентации.

```mermaid
flowchart TB
    A["Пользователь на площадке"] --> B{"Есть связь с центральным контуром?"}
    B -- "Да" --> C["Локальная работа + central sync"]
    B -- "Нет" --> D["Локальная автономная работа"]
    C --> E["Карточки, сессии, чаты, файлы, звонки"]
    D --> E
    D --> F["Изменения копятся локально и передаются между соседними узлами"]
    F --> G["После восстановления связи запускается синхронизация"]
    C --> G
```

### Приложение 3. Сценарий синхронизации
Использовать в пояснительной записке и технической презентации.

```mermaid
sequenceDiagram
    participant User as Пользователь
    participant Edge as Локальный узел
    participant Queue as Очередь изменений
    participant Central as Центральный контур

    User->>Edge: Создание/изменение объекта, комментария, результата проверки
    Edge->>Queue: Фиксация локального события
    alt Центральный контур доступен
        Queue->>Central: Upload pending changes
        Central-->>Queue: Подтверждение / конфликты / новые изменения
        Queue-->>Edge: Обновление локального состояния
    else Центральный контур недоступен
        Queue-->>Edge: Работа продолжается локально
    end
```

### Приложение 4. Сценарий работы комиссии
Использовать в бизнесовой презентации.

```mermaid
flowchart LR
    A["Подготовка сессии"] --> B["Выезд на объект"]
    B --> C["Сканирование и открытие карточки"]
    C --> D["Проверка фактического состояния"]
    D --> E["Комментарий / фото / инцидент"]
    E --> F["Обсуждение спорного объекта в связанном чате"]
    F --> G["Фиксация результата проверки"]
    G --> H["Локальное сохранение и последующая синхронизация"]
```

### Приложение 5. Верхнеуровневая ER-модель предметного контура
Использовать в технической презентации как укрупнение существующей ER-диаграммы.

```mermaid
erDiagram
    ORGANIZATION ||--o{ INVENTORY_ITEM : contains
    ORGANIZATION ||--o{ INVENTORY_SESSION : runs
    INVENTORY_ITEM ||--o{ INVENTORY_ATTACHMENT : has
    INVENTORY_ITEM ||--o{ INVENTORY_INCIDENT : may_have
    INVENTORY_ITEM ||--o{ INVENTORY_CODE : marked_by
    INVENTORY_SESSION ||--o{ INVENTORY_REVIEW : includes
    INVENTORY_SESSION ||--o{ INVENTORY_SESSION_MEMBER : has

    ORGANIZATION {
        string organizationId
        string name
    }
    INVENTORY_ITEM {
        string itemId
        string name
        string inventoryNumber
        string serialNumber
        string status
        string locationId
    }
    INVENTORY_SESSION {
        string sessionId
        string status
        datetime startedAt
    }
    INVENTORY_REVIEW {
        string reviewId
        string itemId
        string sessionId
        string reviewStatus
    }
    INVENTORY_INCIDENT {
        string incidentId
        string itemId
        string severity
        string status
    }
    INVENTORY_ATTACHMENT {
        string attachmentId
        string itemId
        string type
    }
    INVENTORY_CODE {
        string codeId
        string itemId
        string codeType
        string rawValue
    }
```

### Приложение 6. Верхнеуровневая ER-модель коммуникаций и синхронизации
Использовать в технической презентации как укрупнение существующей ER-диаграммы.

```mermaid
erDiagram
    CONVERSATION ||--o{ MESSAGE : contains
    MESSAGE ||--o{ THREAD_MESSAGE : spawns
    CONVERSATION ||--o{ FILE_TRANSFER : includes
    CONVERSATION ||--o{ CALL_SESSION : linked_call
    PEER_DEVICE ||--o{ SYNC_STATE : has
    SYNC_STATE ||--o{ SYNC_CHANGE : accumulates

    CONVERSATION {
        string conversationId
        string type
        string title
    }
    MESSAGE {
        string messageId
        string senderId
        string deliveryStatus
    }
    THREAD_MESSAGE {
        string threadMessageId
        string parentMessageId
    }
    FILE_TRANSFER {
        string transferId
        string status
    }
    CALL_SESSION {
        string callId
        string state
    }
    PEER_DEVICE {
        string peerId
        string trustState
    }
    SYNC_STATE {
        string peerDeviceId
        string status
        int revisionFrom
        int revisionTo
    }
    SYNC_CHANGE {
        string changeId
        string aggregateType
        string operation
    }
```

### Приложение 7. Схема передачи файлов и коммуникаций
Использовать в технической презентации.

```mermaid
sequenceDiagram
    participant A as Узел A
    participant B as Узел B

    A->>B: FILE_OFFER
    B->>A: FILE_ACCEPT
    loop Передача частей
        A->>B: FILE_CHUNK
        B->>A: FILE_ACK
    end
    B->>A: FILE_COMPLETE
    Note over A,B: При необходимости возможен FILE_RESUME_REQUEST
```

## Какие реальные иллюстрации уже есть

### Из бизнесовой презентации
- Титульный слайд и блок общей информации.
- Сравнение с аналогами.
- Скриншоты desktop-прототипа.

### Из технической презентации
- Компонентная архитектура.
- ER-диаграмма inventory.
- ER-диаграмма мессенджера, файлов, звонков и синхронизации.
- Последовательности по пакетам, звонкам и файлам.
- Набор экранов desktop inventory-клиента.

### Из репозитория
- [docs/11.jpg](/home/itech/IdeaProjects/expert-link/docs/11.jpg) - desktop экран профиля узла
- [docs/img.png](/home/itech/IdeaProjects/expert-link/docs/img.png) - mobile главный экран
- [docs/img_3.png](/home/itech/IdeaProjects/expert-link/docs/img_3.png) - mobile экран передач
- [docs/img_4.png](/home/itech/IdeaProjects/expert-link/docs/img_4.png) - mobile экран чатов
- [docs/10.jpg](/home/itech/IdeaProjects/expert-link/docs/10.jpg) - mobile сканирование приглашения
- [docs/9.jpg](/home/itech/IdeaProjects/expert-link/docs/9.jpg) - mobile экран звонка

## Рекомендуемый порядок приложений в финальном PDF
- Приложение А. Компонентная архитектура
- Приложение Б. Режимы работы online/offline
- Приложение В. Сценарий синхронизации
- Приложение Г. Сценарий работы комиссии
- Приложение Д. Верхнеуровневая ER-модель предметного контура
- Приложение Е. Верхнеуровневая ER-модель коммуникаций и синхронизации
- Приложение Ж. Реальные экраны прототипа
