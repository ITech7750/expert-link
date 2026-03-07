# expert-link

`expert-link` — KMP-ready backend-ядро и клиентское Compose Multiplatform приложение для mesh/p2p мессенджера.

Клиентское приложение и мобильная интеграция должны использовать только:
- модуль `backend`
- модуль `contract`

Точка входа:
- `org.expert.link.mesh.backend.MeshBackend`

Публичный контракт:
- `org.expert.link.mesh.contract.api.MeshNode`

## Модули
- `contract` — KMP-ready публичные модели и API для мобильной интеграции
- `backend` — библиотечный facade поверх внутреннего runtime
- `backend:data` — KMP-ready доменные модели, storage contracts и порты
- `backend:engine` — внутренняя application-логика
- `backend:infra` — внутренние адаптеры и репозитории
- `backend:runtime` — внутренний composition root узла
- `app-shared` — общий presentation/UI/state слой Compose Multiplatform
- `app-desktop` — Desktop клиент поверх `app-shared + backend`
- `app-android` — Android entry point поверх `app-shared + backend`
- `bootstrap` — CLI host
- `simulator` — mobile-style demo сценарии только через публичный API

`contract` и `backend:data` уже собираются как Kotlin Multiplatform модули с `commonMain`.
JVM-only код изолирован в `backend:infra`, `backend:runtime` и `bootstrap`.
`app-shared` тоже построен как KMP-модуль и не использует внутренние backend-пакеты напрямую.

## Быстрый старт
Сборка:

```bash
./gradlew compileKotlin
```

Запуск Desktop-клиента:

```bash
export JAVA_HOME=/home/itech/.jdks/corretto-21.0.9
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app-desktop:run --args="--memory --name=desktop-demo"
```

Аргументы Desktop-клиента:
- `--name=<имя>` — отображаемое имя узла
- `--http=<порт>` — HTTP transport port
- `--discovery=<порт>` — discovery port
- `--memory` — безопасный demo-режим на in-memory transport/discovery

## Что умеет клиент
Пользовательский интерфейс построен вокруг обычных сценариев, а не вокруг внутренних backend-терминов.

Основные возможности:
- запуск и остановка узла
- просмотр локального профиля
- создание приглашения для сопряжения
- копирование и показ invite в виде строки и QR
- ручной импорт invite
- поиск узлов рядом
- просмотр доверенных контактов
- чаты и статусы доставки
- передача файлов с прогрессом, продолжением и отменой
- signaling звонков
- отдельный экран диагностики, логов и сетевых состояний

## Пользовательский путь
Обычный сценарий в приложении выглядит так:
1. Открыть `Главная` и дождаться запуска узла.
2. Перейти в `Сопряжение` и нажать `Создать приглашение`.
3. Передать приглашение второму устройству через `Копировать`, `Поделиться` или QR.
4. На втором устройстве вставить invite вручную и выполнить сопряжение.
5. Перейти в `Контакты` или `Чаты`, открыть диалог и отправить сообщение.
6. При необходимости открыть `Передачи`, `Звонок` или `Диагностика`.

## Проверка двумя Desktop-инстансами
Для проверки реальной связи между двумя desktop-процессами не используйте `--memory`.

Терминал 1:

```bash
export JAVA_HOME=/home/itech/.jdks/corretto-21.0.9
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app-desktop:run --args="--name=Алиса --http=18100 --discovery=19100" --no-daemon
```

Терминал 2:

```bash
export JAVA_HOME=/home/itech/.jdks/corretto-21.0.9
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app-desktop:run --args="--name=Боб --http=18101 --discovery=19100" --no-daemon
```

Что проверить:
1. На первом окне открыть `Сопряжение` и создать приглашение.
2. Нажать `Копировать` и вставить invite во втором окне.
3. После сопряжения открыть `Чаты` и отправить сообщение.
4. В `Диагностика` проверить события, ACK и маршруты.

Для multihop-проверки нужен третий инстанс.

Запуск simulator:

```bash
./gradlew :simulator:run
```

Запуск тестов:

```bash
./gradlew test
```

## Как интегрировать в мобильное приложение
Минимальный пример:

```kotlin
import org.expert.link.mesh.backend.MeshBackend
import org.expert.link.mesh.contract.api.MeshChatCommand
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshNodeConfig

val node = MeshBackend.launch(
    MeshNodeConfig(
        displayName = "mobile-node",
        bindHost = "127.0.0.1",
        httpPort = 18081,
        discoveryPort = 19081,
        multicastGroup = "239.10.10.10",
        featureFlags = MeshFeatureFlags(
            discoveryEnabled = true,
            relayEnabled = false,
            inMemoryTransport = true,
            inMemoryDiscovery = true,
        ),
    ),
)

val invite = node.createPairingInvite()
val peers = node.peers()
val nearby = node.nearbyPeers()
val conversation = node.openConversation("target-peer-id")
val message = node.sendChat(
    MeshChatCommand(
        targetPeerId = "target-peer-id",
        body = "hello",
        conversationId = conversation.conversationId,
    ),
)
val metrics = node.metrics()
node.stop()
```

Подробные примеры:
- `docs/mobile-usage.md`
- `docs/kmp-readiness.md`
- `simulator/src/main/kotlin/org/expert/link/mesh/simulator/scenario`

## Клиентское приложение
Основные экраны Compose клиента:
- `Главная`
- `Узлы рядом`
- `Сопряжение`
- `Контакты`
- `Чаты`
- `Диалог`
- `Передачи`
- `Звонок`
- `Диагностика`
- `Профиль`
- `Настройки`

Особенности UX:
- короткие русские названия действий
- быстрые кнопки на главной и в карточках контактов
- пустые состояния без технического шума
- QR и invite string на экранах `Сопряжение` и `Профиль`
- `Копировать` и `Поделиться` там, где это уместно
- desktop-режим с более широким layout и выбором файла через системный диалог
- отдельная `Диагностика`, чтобы технические детали не мешали обычному использованию

Клиент использует backend только через:
- `MeshBackend.launch(config)`
- `MeshNode`
- модели из `contract`

`simulator` и `app-shared` демонстрируют один и тот же способ интеграции: UI не обращается к `backend:data`, `backend:engine`, `backend:infra` и `backend:runtime`.

## Статус платформ
- `Desktop` — готово и собирается локально
- `Android` — код и entry point готовы; локальная проверка не выполнена, потому что в текущем окружении нет Android SDK
- `iOS` — подготовлена shared foundation через `contract`, `backend:data` и `app-shared`, отдельный target пока не добавлен
- `Web` — подготовлена shared foundation, отдельный target пока не добавлен

Ограничения текущего UX:
- QR-сканирование пока не реализовано; остаётся ручной ввод invite
- на desktop доступно `Копировать`, а `Поделиться` не поддерживается платформенно
- звонки пока ограничены signaling-частью без медиадвижка
