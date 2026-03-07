# expert-link

`expert-link` — Kotlin Multiplatform проект, состоящий из backend-ядра mesh/p2p узла, публичного контракта и клиентских host-модулей.

## Модули
- `contract` — публичный API и сериализуемые модели.
- `backend` — публичный facade поверх runtime узла.
- `backend:data` — domain-модели, entity и port-интерфейсы.
- `backend:application` — application factory, mapper и service.
- `backend:infra` — adapter, client и repository-реализации портов.
- `backend:runtime` — composition root, controller и lifecycle узла.
- `bootstrap` — CLI host для JVM.
- `simulator` — сценарии использования публичного backend API.
- `app-shared` — shared presentation и Compose UI.
- `app-desktop` — Desktop host.
- `app-android` — Android host, если доступен SDK.

## Backend-слои
- `controller` — принимает транспортный запрос и передаёт его в lifecycle узла.
- `application` — оркестрация use case, фабрики пакетов, мапперы entity.
- `domain` — модели, entity и порты.
- `infrastructure` — реализации портов, транспортные и discovery-адаптеры, in-memory repository.

Пакеты backend:
- `org.expert.link.mesh.controller`
- `org.expert.link.mesh.application.factory`
- `org.expert.link.mesh.application.mapper`
- `org.expert.link.mesh.application.service`
- `org.expert.link.mesh.domain.entity`
- `org.expert.link.mesh.domain.model`
- `org.expert.link.mesh.domain.port`
- `org.expert.link.mesh.infrastructure.adapter`
- `org.expert.link.mesh.infrastructure.client`
- `org.expert.link.mesh.infrastructure.repository`

## Зависимости модулей
- `backend:data` не зависит от `application`, `infra` и `runtime`.
- `backend:application` зависит от `backend:data`.
- `backend:infra` зависит от `backend:data` и `backend:application`.
- `backend:runtime` зависит от `backend:data`, `backend:application` и `backend:infra`.
- `backend` зависит от `contract`, `backend:data`, `backend:application` и `backend:runtime`.
- `simulator`, `app-shared`, `app-desktop` и `app-android` используют backend только через `backend` и `contract`.

## Точки входа
- публичный backend facade: `org.expert.link.mesh.backend.MeshBackend`
- публичный контракт узла: `org.expert.link.mesh.contract.api.MeshNode`
- runtime bootstrap: `org.expert.link.mesh.bootstrap.MeshNodeBootstrap`
- CLI host: `org.expert.link.mesh.bootstrap.MeshNodeCliKt`
- simulator: `org.expert.link.mesh.simulator.SimulatorMainKt`

## Основные сценарии backend
1. `MeshBackend.launch(config)` создаёт runtime через `MeshNodeBootstrap`.
2. `MeshNode` вызывает `NodeLifecycleService`.
3. `NodeLifecycleService` делегирует сценарий в application service.
4. Application service использует domain-port интерфейсы.
5. Infrastructure-реализации портов выполняют storage, discovery, transport, crypto и relay-операции.
6. Входящий HTTP пакет проходит через `PacketRouteController` -> `PacketController` -> `NodeLifecycleService`.

## Storage
Постоянное хранение:
- локальный профиль
- доверенные узлы
- pairing-сессии
- conversations
- messages
- file transfer history
- call sessions
- event log
- block list

Runtime cache:
- endpoint cache
- route repository
- reverse path repository
- dedup cache
- outgoing queue
- pending ACK repository

## Сборка и запуск
Сборка:
```bash
./gradlew compileKotlin
```

Тесты:
```bash
./gradlew test
```

Simulator:
```bash
./gradlew :simulator:run
```

Desktop host:
```bash
export JAVA_HOME=/home/itech/.jdks/corretto-21.0.9
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app-desktop:run --args="--memory --name=desktop-demo"
```

Два desktop-инстанса для сетевой проверки:
```bash
./gradlew :app-desktop:run --args="--name=Алиса --http=18100 --discovery=19100" --no-daemon
./gradlew :app-desktop:run --args="--name=Боб --http=18101 --discovery=19100" --no-daemon
```

## Документы
- `docs/architecture.md` — структура модулей и слоёв.
- `docs/spec.md` — фактические backend-сценарии и модели.
- `docs/kmp-readiness.md` — текущее распределение common/jvm частей.
- `docs/mobile-usage.md` — использование backend facade из клиентских host-модулей.
