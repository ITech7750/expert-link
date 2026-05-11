# Expert-Link Hybrid Technical Report

## 1. Итог интеграции
`expert-link` доработан как hybrid edge-node:
- offline-first/mesh-first поведение сохранено;
- central backend подключён как canonical source of truth;
- локальная работа (inventory + messenger + file transfer + calls + diagnostics) продолжает работать без интернета;
- при доступности сети выполняются push/pull, reconciliation и conflict-aware синхронизация.

## 2. Как выполнена интеграция с central backend
- Добавлен central client layer в `backend:infra` (`CentralApiClients.kt`) с typed Ktor-клиентами:
  - auth
  - organization-access
  - inventory
  - sync
  - attachment
  - export
- Runtime wiring (`MeshNodeBootstrap`) создаёт central clients через factory и подключает их в application services.
- Публичный `MeshNode` контракт расширен central/hybrid операциями (auth, org context, sync status, pending/conflicts, sync now, conflict resolve, attachments/exports).

## 3. Как сохранено offline-first и mesh-first
- Local operations сохраняются в локальные repositories и inventory event journal.
- Mesh функции не изменены как обязательная зависимость от central; discovery/pairing/chats/groups/threads/files/calls/routing/relay/diagnostics работают автономно.
- При отсутствии central backend приложение переходит в `MESH_ONLY`/`OFFLINE` и не блокирует пользовательские сценарии.

## 4. Как реализована синхронизация
- `SyncQueueService` формирует pending changes из `InventoryEvent` с учётом `lastUploadedSequence`.
- `CentralSyncOrchestrationService` выполняет:
  - upload pending changes;
  - pull canonical changes по cursor;
  - apply incoming events через `InventoryEventApplier`;
  - обновление `CentralSyncState` (cursor/seq/pending/conflicts/lastError).
- Поддержаны retry/resume через повторный запуск `syncNow` на актуальном состоянии sync-state.

## 5. Как обеспечено сосуществование mesh и central truth
- Mesh propagation и canonical sync логически разделены:
  - mesh: packet/event delivery между edge-узлами;
  - central: upload/pull change-batches и canonical reconciliation.
- Local-first принцип сохранён: изменения доступны локально/по mesh сразу, затем догоняются в central.
- Серверные конфликты не теряются: сохраняются локально и отражаются в режиме `CONFLICT_REVIEW_REQUIRED`.

## 6. Как клиент работает с `organization-access-service`
- Используется только новый сервис `organization-access-service`.
- `CentralOrganizationAccessService` кэширует:
  - organizations
  - memberships
  - roles/permissions
  - active organization
- Last-known org context доступен офлайн.
- Membership привязывается к локальному `peerId` для корректного local RBAC в offline/mesh режиме.

## 7. Прозрачность storage strategy (demo/local FS vs normal/S3)
- Клиент работает только через unified central attachment/export API.
- `artifactRef` рассматривается как opaque reference; схема/тип backend storage не парсятся и не ветвят клиентскую логику.
- Поэтому одинаково поддерживаются:
  - `demo` профиль (local filesystem storage на central backend);
  - normal profiles (`dev/stage/prod`) с S3-compatible storage.

## 8. Изменения в UI (ComposeApp)
- Добавлены hybrid state stores и экраны состояния в `MainScreen`/`ProfileScreen`:
  - online/offline + central availability;
  - auth status;
  - active organization;
  - pending changes;
  - conflict indicators;
  - ручной sync.
- Existing messenger и inventory flows сохранены.

## 9. Simulator и тесты
- Добавлен `HybridCentralDemoScenario` для end-to-end гибридных сценариев.
- Добавлены/обновлены тесты:
  - central auth/session persistence;
  - organization-access + RBAC binding to local peer;
  - sync queue + cursor behavior;
  - conflict transition;
  - offline mesh fallback;
  - storage-agnostic handling для attachment/export artifact refs.

## 10. Что добавлено в текущем проходе
- Central organization workspace snapshot:
  - клиент читает и кэширует `locations`, `departments`, `cost-centers`, `bank-accounts`, `organization-parties`, `permissions`, `organization dashboard`, `user profile`, `relay nodes`;
  - snapshot сохраняется локально (`central_organization_workspace`) и доступен офлайн.
- `organization-access-service` и `relay-service` покрыты typed central client-операциями без обходных REST-вызовов.
- `Profile` UI получил отображение central workspace summary и статуса справочников/relay.
- Верхняя навигация расширена разделом `Отчёты` с прямым входом в inventory reports flow.
- Android scan flow переведён на реальное чтение QR + линейных кодов (в т.ч. Code128).
- Desktop print flow дополнен системной печатью PDF (`printFile`) из экранов маркировки.
