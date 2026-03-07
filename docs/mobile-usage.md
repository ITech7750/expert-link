# Mobile Usage

`simulator` теперь показывает backend ровно как библиотеку для мобильного приложения.
Во всех примерах используются только:
- `org.expert.link.mesh.backend.MeshBackend`
- `org.expert.link.mesh.contract.api.MeshNode`
- модели из `org.expert.link.mesh.contract.*`

Внутренние пакеты `backend:data`, `backend:application`, `backend:infra`, `backend:runtime` мобильное приложение не использует.

## KMP app-модули
- `app-shared` — общий Compose UI, navigation, state holders и integration layer
- `app-desktop` — Desktop host
- `app-android` — Android host

`app-shared` использует backend так же, как это будет делать мобильное приложение:
- создаёт `MeshNodeConfig`
- запускает `MeshBackend.launch(config)`
- хранит `MeshNode` как SDK facade
- вызывает только публичные операции `MeshNode`
- отображает данные из `contract` моделей

## Что уже улучшено в UX
Интерфейс рассчитан на обычного пользователя и не показывает лишнюю технику в базовых сценариях.

Основные принципы:
- короткие русские названия
- быстрые действия на главных экранах
- отдельная диагностика для сетевых деталей
- явные действия `Создать приглашение`, `Копировать`, `Поделиться`, `Отправить`, `Повторить`
- пустые, загрузочные и ошибочные состояния без перегрузки

Pairing и invite:
- на `Сопряжение` и `Профиль` можно создать приглашение
- при поддержке платформы показывается QR
- есть `Копировать`
- есть `Поделиться`, если платформа умеет
- всегда остаётся ручной ввод invite

Desktop:
- широкий layout
- удобное копирование invite и peerId
- системный выбор файла
- длинные значения показаны в удобных моноширинных блоках

Mobile foundation:
- нижняя навигация для основных разделов
- touch-friendly кнопки
- компактные карточки и секции
- QR как основной способ показа invite

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

В Compose-клиенте этот вызов инкапсулирован в `NodeSessionController` из `app-shared`, чтобы UI работал через `StateFlow`.

## Основные экраны
- `Главная` — состояние узла, быстрые действия, краткий статус сети
- `Узлы рядом` — поиск соседей, nearby peers и route summary
- `Сопряжение` — создание invite, QR, копирование, ручной импорт
- `Контакты` — доверенные узлы и быстрые действия: чат, файл, звонок, блокировка
- `Чаты` — список диалогов и быстрый переход в разговор
- `Диалог` — сообщения, доставка, файл, звонок, маршрут
- `Передачи` — прогресс, resume, cancel, история
- `Звонок` — signaling состояния и действия
- `Диагностика` — события, метрики, сеть, relay
- `Профиль` — локальный профиль, peerId, invite
- `Настройки` — имя узла, порты и demo-флаги

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

Как это используется в UI:
- пользователь нажимает `Создать приглашение`
- приложение показывает invite string и QR
- по кнопке `Копировать` вызывается platform service, затем показывается короткое подтверждение
- по кнопке `Поделиться` используется системный share sheet, если он доступен
- второй пользователь вставляет invite вручную и запускает pairing

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

Как это используется в UI:
- экран `Узлы рядом` запускает поиск вручную и показывает только нужные детали
- расширенные route и relay данные вынесены в отдельные секции
- перед отправкой сообщения можно показать пользователю прямой, multihop или relay маршрут

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

Как это используется в UI:
- `Чаты` показывает список диалогов как обычный мессенджер
- `Диалог` показывает сообщения, кнопку отправки, доставку и повторную отправку
- ACK не показывается как низкоуровневая сущность, а маппится в понятный статус доставки

## Передача файлов
`MeshFileTransferCommand.path` считается платформенным file handle.
На JVM это обычный путь, на мобильной платформе позже это может быть URI, sandbox path или иной локальный идентификатор файла.

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

Как это используется в UI:
- на desktop файл выбирается через системный диалог
- на mobile позже сюда подключается платформенный picker
- экран `Передачи` показывает понятный прогресс и кнопки `Продолжить` / `Отменить`

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

Как это используется в UI:
- пользователь видит простой экран звонка без лишней техники
- состояние читается через `callSessions()`
- действия `Принять`, `Отклонить`, `Завершить` маппятся на публичные backend-команды

## Диагностика
Вызовы:
```kotlin
val events = node.recentEvents(limit = 50)
val metrics = node.metrics()
```

Модели:
- `MeshEventLogEntry`
- `MeshMetricSnapshot`

Как это используется в UI:
- `Диагностика` отделена от основных пользовательских сценариев
- внутри есть простое разделение на события, метрики и сеть
- повседневный пользователь туда не попадает случайно, но для demo и отладки всё доступно

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

## Desktop клиент
Запуск Desktop UI:

```bash
export JAVA_HOME=/home/itech/.jdks/corretto-21.0.9
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app-desktop:run --args="--memory --name=desktop-demo"
```

Для локального demo безопаснее использовать `--memory`, чтобы UI работал без реального UDP/HTTP транспорта.

Два desktop-инстанса для проверки:
```bash
./gradlew :app-desktop:run --args="--name=Алиса --http=18100 --discovery=19100" --no-daemon
./gradlew :app-desktop:run --args="--name=Боб --http=18101 --discovery=19100" --no-daemon
```

Для двух отдельных процессов не используйте `--memory`, иначе они не увидят друг друга.

## Android клиент
`app-android` использует тот же `app-shared` слой и тот же `MeshBackend` facade.
Локально в этом окружении Android сборка не проверялась, потому что отсутствует Android SDK.

Ограничения текущего этапа:
- QR-сканирование пока не реализовано, доступен ручной импорт
- системный share flow реализован только там, где платформа это поддерживает
- desktop и Android используют один и тот же presentation слой, но platform services различаются
