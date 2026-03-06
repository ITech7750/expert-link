# expert-link

`expert-link` — embedded backend-ядро mesh/p2p мессенджера для мобильного приложения.

Мобильное приложение должно использовать только:
- модуль `backend`
- модуль `contract`

Точка входа:
- `org.expert.link.mesh.backend.MeshBackend`

Публичный контракт:
- `org.expert.link.mesh.contract.api.MeshNode`

## Модули
- `contract` — стабильные публичные модели и API для мобильной интеграции
- `backend` — библиотечный facade поверх внутреннего runtime
- `backend:data` — внутренние доменные модели и порты
- `backend:engine` — внутренняя application-логика
- `backend:infra` — внутренние адаптеры и репозитории
- `backend:runtime` — внутренний composition root узла
- `bootstrap` — CLI host
- `simulator` — mobile-style demo сценарии только через публичный API

## Быстрый старт
Сборка:

```bash
./gradlew compileKotlin
```

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
- `simulator/src/main/kotlin/org/expert/link/mesh/simulator/scenario`
