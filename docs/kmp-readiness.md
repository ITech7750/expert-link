# KMP Readiness

Проект приведён к состоянию, в котором общие модели, контракты и storage-границы готовы к следующему шагу в сторону KMP/KMM.

## Что уже готово
- `contract` переведён на Kotlin Multiplatform plugin и собирается через `commonMain`
- `backend:data` переведён на Kotlin Multiplatform plugin и собирается через `commonMain`
- публичные DTO, domain models и repository/storage ports очищены от `java.util.UUID`, `java.util.Base64`, `java.net.URI`
- время и даты в общих слоях идут через `kotlinx.datetime`
- file transfer API трактует `path` как platform-neutral file handle
- in-memory storage adapters переведены на coroutine-friendly state и Kotlin collections, без зависимости на JVM concurrent API в общих контрактах

## Что осталось JVM-only
- `backend:infra`
  - crypto через JCA/JCE
  - UDP discovery
  - Ktor JVM transport
  - filesystem adapter для чанков
  - stub rendezvous client
- `backend:runtime`
  - composition root
  - embedded Ktor server wiring
- `bootstrap`
  - CLI host

Это нормальная граница. Именно здесь позже должны появиться `jvmMain`, `androidMain`, `iosMain` реализации.

## Главные замены
- `java.util.UUID` -> KMP-friendly random hex id generator
- `java.util.Base64` в общих слоях -> `kotlin.io.encoding.Base64`
- `java.net.URI` в common-friendly mapper logic -> локальный parser без JVM URI API
- `java.io.File` в `engine` -> `FileChunkStoragePort` + platform-neutral file handle
- `MessageDigest` в `engine` -> hashing вынесен в filesystem storage adapter
- `ConcurrentHashMap` и `AtomicLong` в `engine` -> `Mutex` + Kotlin collections
- `java.util.ArrayDeque` в common-friendly частях -> Kotlin `ArrayDeque`

## Storage архитектура

### Persistent storage contracts
- `LocalProfileRepositoryPort`
- `PeerRepositoryPort`
- `PairingSessionRepositoryPort`
- `BlockListRepositoryPort`
- `ConversationRepositoryPort`
- `MessageRepositoryPort`
- `FileTransferRepositoryPort`
- `CallSessionRepositoryPort`
- `EventLogRepositoryPort`

Эти порты уже пригодны для будущего SQLDelight/SQLite слоя.

### Runtime caches
- `EndpointCachePort`
- `RouteRepositoryPort`
- `ReversePathRepositoryPort`
- `DedupCachePort`
- `OutgoingQueuePort`
- `PendingAckRepositoryPort`

Это краткоживущий runtime state. Его не нужно тащить в постоянную БД без отдельного решения.

### File storage boundary
`FileChunkStoragePort` теперь отвечает за platform-specific file операции:
- описание локального файла
- чтение чанков
- вычисление SHA-256 файла
- staging входящих чанков
- сборку файла
- построение target path/handle

Это место для будущих Android/iOS adapters.

## Куда позже подключать SQLDelight / SQLite
- новый persistent adapter слой под `backend:data` ports
- реализация persistent repositories в `androidMain` / `iosMain` / `jvmMain`
- SQLDelight schema для:
  - локального профиля
  - trusted peers
  - pairing sessions
  - conversations
  - messages
  - file transfer history
  - block list
  - event log

Runtime caches при этом можно оставить in-memory.

## Что ещё можно сделать на следующем этапе
- перевести часть `backend:engine` в Multiplatform модуль после выделения logging facade
- заменить JVM filesystem adapter на expect/actual или platform adapters через KMP source sets
- подготовить Android и iOS реализации `FileChunkStoragePort`
- ввести SQLDelight adapters для persistent repository ports
