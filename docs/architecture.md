# Architecture

## Модули
- `contract` — публичный API (`MeshNode`), команды и DTO.
- `backend` — публичный facade, адаптирует contract-вызовы к runtime/application.
- `backend:data` — domain model, entity, port.
- `backend:application` — use-case сервисы, factory и mapper.
- `backend:infra` — реализации port (adapter/client/repository).
- `backend:runtime` — bootstrap и controller.
- `simulator` — сценарии использования только через `MeshNode`.
- `composeApp` — единый KMP client module:
  - `commonMain` — Compose UI, navigation, presentation;
  - `jvmMain` — Desktop entry point и desktop actual-сервисы;
  - `androidMain` — Android entry point и android actual-сервисы.

## ComposeApp структура
- `composeApp/commonMain`
  - `org.expert.link.app.shared.ui.*` — экранный UI и навигация;
  - `org.expert.link.app.shared.presentation.*` — stores и состояние экранов;
  - `org.expert.link.app.shared.platform.AppPlatformServices` — `expect`-контракт платформенных сервисов.
- `composeApp/jvmMain`
  - `org.expert.link.app.desktop.DesktopMainKt` — Desktop entry point;
  - `org.expert.link.app.shared.platform.AppPlatformServices.jvm.kt` — `actual` desktop-реализация.
- `composeApp/androidMain`
  - `org.expert.link.app.android.MainActivity` — Android entry point;
  - `org.expert.link.app.shared.platform.AppPlatformServices.android.kt` — `actual` Android-реализация.

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
- `composeApp/commonMain` использует только `MeshNode` и contract-модели.
- Внутренние `backend:data/application/infra/runtime` классы в UI/Simulator напрямую не используются.

## Topology и Connectivity подсистема

### Domain модели
- `NetworkTopologyState` — полный runtime-снимок топологии.
- `NetworkRoleState` — роль узла и текущий выбранный хост.
- `HostRole`, `HostCandidate` — модель хоста и кандидатов.
- `RouteHealth`, `RouteHealthState` — состояние маршрутов.
- `ConnectivityMode`, `ConnectivityStrategy` — стратегия связности.
- `RelayMode` — состояние relay/proxy fallback.
- `TopologyEvent` / `TopologyEventType` — события перестройки сети.

### Application сервисы
- `TopologyStateService`:
  - агрегирует topology snapshot;
  - отслеживает host election/failover;
  - фиксирует route health и continuity degradation/recovery;
  - возвращает стратегию связности и relay mode.
- `RoutingService`:
  - обучает маршруты и передаёт изменения в `TopologyStateService`;
  - инвалидирует маршруты при деградации доставки.
- `DiscoveryOrchestrationService`:
  - передаёт события обнаружения/потери узлов в `TopologyStateService`.
- `RelayService` и `DeliveryTrackingService`:
  - передают результат доставки для обновления route health и relay state.
- `ConnectivityStrategyService`:
  - выбирает стратегию (`LOCAL_DIRECT` / `RELAY_FLOOD` / `RENDEZVOUS_*`);
  - хранит snapshots активных стратегий для topology.

### Runtime wiring
- `MeshNodeBootstrap` создаёт `TopologyStateService` и передаёт его в:
  - `RoutingService`
  - `DiscoveryOrchestrationService`
  - `RelayService`
  - `DeliveryTrackingService`
  - `NodeLifecycleService`
- `NodeLifecycleService.start()` инициализирует topology с локальным endpoint и relay mode из config.
- `rememberPeerEndpoint`/`forgetPeerEndpoint` обновляют topology state.

### Contract API
`MeshNode` предоставляет topology-интерфейс:
- `observeTopologyState`
- `observeHostRole`
- `observeConnectivityStrategy`
- `inspectRouteHealth`
- `relayModeState`
- `forceTopologyRefresh`

### Инфраструктура relay/proxy
- `RendezvousRelayClient` реализует:
  - регистрацию/heartbeat/lookup/unregister;
  - relay candidate selection;
  - stale cleanup для registration записей;
  - relay-aware lookup (`relayOnly`, `directEndpoints`, `relayCandidates`).

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
- `CallMediaService`:
  - управляет WebRTC media lifecycle;
  - создаёт/закрывает media-сессии по `callId`;
  - применяет входящие `SDP/ICE` к media session;
  - отправляет исходящие `SDP/ICE` через `CallSignalingService`;
  - синхронизирует `mute/camera` состояния.

### Signaling поток
1. `MeshNode` вызывает call-метод контракта.
2. `backend:MeshBackend` делегирует в `NodeLifecycleService`.
3. `NodeLifecycleService` делегирует в `CallSignalingService`.
4. `CallSignalingService` формирует `PacketEnvelope` c payload `CALL_INVITE`/`CALL_SIGNAL`/`CALL_HANGUP`.
5. На принимающей стороне `PacketController -> NodeLifecycleService` вызывает `handleInvite`/`handleSignal`/`handleHangup`.
6. Состояния, участники и события сохраняются через call repository ports.

### Media поток (WebRTC)
1. `MeshNode.start*Call` вызывает `NodeLifecycleService.start*Call`.
2. `NodeLifecycleService` делегирует в `CallMediaService`.
3. `CallMediaService` запускает signaling invite через `CallSignalingService`.
4. При наличии media backend (`MediaEnginePort.isSupported=true`) открывается `WebRtcSessionPort`.
5. `SDP_OFFER/SDP_ANSWER/ICE_CANDIDATE` конвертируются в `CALL_SIGNAL` пакеты.
6. На принимающей стороне `CallMediaService.handleSignal` применяет SDP/ICE к локальной media session.
7. `toggleMicrophone`/`toggleCamera`/`switchCamera` обновляют media state и синхронизируются signaling-сигналами.

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

## Inventory UI (ComposeApp)

### Экранный слой
- Добавлены экраны: `Инвентаризация` (секции Организации/Оборудование/Сессии/Проверка/Отчёты), карточка объекта, детали сессии, сканер QR.
- Существующие экраны мессенджера сохранены: чаты, диалоги, треды, файлы, звонки, диагностика.

### Presentation слой
- Новые stores: `InventoryStore`, `InventoryItemStore`, `InventorySessionStore`, `InventoryScannerStore`.
- Store работают только через `MeshNode` контракт, без прямого доступа к backend.

### Интеграция с мессенджером
- Для объекта/сессии создаётся чат комиссии, запись связывается через `MeshUpdateInventorySessionCommand`/`MeshLinkInventoryDiscussionCommand`.
- Переходы в чат/тред выполняются через существующую навигацию и `ChatScreen`.

### Платформенные сервисы
- `AppPlatformServices.describeFile` добавляет дескриптор файла для инвентарных вложений.
- QR-сканер использует `InlineQrScanner` (Android) или системный `scanQr()` при доступности.

## Inventory подсистема (дополнение поверх mesh)

### Domain модели
- Организации и RBAC: `Organization`, `OrganizationMember`, `Role`, `InventoryPermission`.
- Каталог: `InventoryCategory`, `InventorySubcategory`, `InventoryTag`, `InventoryAttributeDefinition`, `InventoryCategoryTemplate`, `InventoryFieldTemplate`, `InventoryRequiredFieldRule`.
- Владелец/структура: `InventoryOwner`, `InventoryDepartment`, `InventoryCostCenter`, `InventoryLegalHolder`, `InventorySupplier`, `InventoryFundingSource`.
- Инвентарь: `InventoryItem`, `InventoryStatus`, `InventoryCondition`, `InventoryItemType`, `InventoryLocation`.
- Сессии: `InventorySession`, `InventorySessionMember`, `InventorySessionStatus`, `InventorySessionReviewStatus`, `InventorySessionResult`.
- Workflow: `InventoryReview`, `InventoryReviewStatus`, `InventoryComment`, `InventoryAttachment`.
- Инциденты/алерты: `InventoryIncident`, `InventoryAlertEvent`, `InventoryReminder`, `InventoryRuleThreshold`, `InventoryDeadlineRule`.
- Аудит/история: `InventoryChangeLog`, `InventoryFieldChange`, `InventoryRevision`, `InventoryConflict`, `InventorySyncStatus`.
- Поиск/метрики: `InventoryDashboardSnapshot`, `InventorySearchQuery`, `InventorySearchResult`.
- QR/Barcode: `InventoryQrCode`, `InventoryCodeBinding`, `InventoryScanEvent`.
- События: `InventoryEvent`, `InventoryEventPayload`, `InventoryEventType`.

### Repository ports
- `OrganizationRepositoryPort`, `OrganizationMemberRepositoryPort`, `RoleRepositoryPort`.
- Каталог: `InventoryCategoryRepositoryPort`, `InventorySubcategoryRepositoryPort`, `InventoryTagRepositoryPort`,
  `InventoryAttributeDefinitionRepositoryPort`, `InventoryCategoryTemplateRepositoryPort`, `InventoryLocationRepositoryPort`.
- Владение: `InventoryOwnerRepositoryPort`, `InventoryDepartmentRepositoryPort`, `InventoryCostCenterRepositoryPort`,
  `InventoryLegalHolderRepositoryPort`, `InventorySupplierRepositoryPort`, `InventoryFundingSourceRepositoryPort`.
- Основные: `InventoryItemRepositoryPort`, `InventorySessionRepositoryPort`, `InventorySessionMemberRepositoryPort`,
  `InventoryReviewRepositoryPort`, `InventoryCommentRepositoryPort`, `InventoryAttachmentRepositoryPort`.
- Инциденты/алерты: `InventoryIncidentRepositoryPort`, `InventoryAlertRepositoryPort`, `InventoryReminderRepositoryPort`,
  `InventoryThresholdRuleRepositoryPort`, `InventoryDeadlineRuleRepositoryPort`.
- Аудит/метрики/поиск: `InventoryChangeLogRepositoryPort`, `InventoryDashboardRepositoryPort`,
  `InventoryRevisionRepositoryPort`, `InventoryConflictRepositoryPort`.
- QR/Barcode: `InventoryQrCodeRepositoryPort`, `InventoryCodeBindingRepositoryPort`, `InventoryScanEventRepositoryPort`.
- События/экспорт: `InventoryEventRepositoryPort`, `InventoryExportRepositoryPort`.

### Application сервисы
- `InventoryRbacService` — проверка permissions и базовые роли.
- `InventoryOrganizationService` — оргструктура и роли.
- `InventoryCatalogService` — каталог (категории/подкатегории/теги/шаблоны/локации).
- `InventoryOwnershipService` — владельцы, подразделения, контрагенты, источники финансирования.
- `InventoryItemService` — lifecycle объектов и workflow, комментарии/вложения.
- `InventorySessionService` — lifecycle сессий и состав комиссии.
- `InventoryReviewService` — подтверждение/отклонение.
- `InventoryIncidentService` — инциденты, алерты, правила, reminders.
- `InventoryExportService` — экспорт/отчёты.
- `InventoryChangeLogService` — аудит и история изменений.
- `InventoryDashboardService` — метрики и snapshot.
- `InventorySearchService` — фильтрация и поиск.
- `InventoryCodeService` — QR/Barcode binding и scan события.
- `InventoryQueryService` — QR/Barcode lookup.
- `InventoryDiscussionService` — интеграция с group chat/thread.
- `InventoryEventService` — аудит-события.
- `InventorySyncService` — доставка событий и синхронизация.

### Storage
- Room (`database`) дополнен таблицами `organization`, `organization_member`, `role`,
  `inventory_item`, `inventory_session`, `inventory_event`, `inventory_export`, `inventory_qrcode`.
- Добавлены таблицы каталога и владения: `inventory_subcategory`, `inventory_tag`, `inventory_attribute_definition`,
  `inventory_category_template`, `inventory_owner`, `inventory_department`, `inventory_cost_center`,
  `inventory_legal_holder`, `inventory_supplier`, `inventory_funding_source`, `inventory_location`.
- Добавлены таблицы инцидентов и аудита: `inventory_incident`, `inventory_alert`, `inventory_reminder`,
  `inventory_rule_threshold`, `inventory_deadline_rule`, `inventory_change_log`, `inventory_dashboard_snapshot`.
- Добавлены таблицы для QR/scan и sync: `inventory_code_binding`, `inventory_scan_event`,
  `inventory_revision`, `inventory_conflict`.
- Модели сохраняются полностью как `payloadJson` + индексы для выборок.
- События (`InventoryEvent`) хранятся как журнал с `sequence` для синхронизации.

### Mesh sync
- Добавлены `PacketType`: `INVENTORY_EVENT`, `INVENTORY_SYNC_REQUEST`, `INVENTORY_SYNC_RESPONSE`.
- `InventoryEventPacket` доставляет изменения объектов и workflow.
- `InventorySyncService.requestSync` инициирует выборку по `sequence`.
- Конфликты фиксируются `InventoryEventType.CONFLICT_DETECTED`, записью `InventoryConflict` и переводом объекта в `REQUIRES_UPDATE`.
- Ревизии сохраняются в `InventoryRevision` для conflict detection и merge readiness.

### Contract API
`MeshNode` дополнен операциями:
- организации/участники/роли;
- инвентарные объекты, сессии, review/approval;
- каталог (подкатегории, теги, атрибуты, шаблоны);
- владельцы/подразделения/контрагенты;
- инциденты, alerts, reminders, правила;
- search, dashboard snapshot, change log;
- QR/Barcode binding и scan events;
- экспорт;
- события и sync (`syncInventoryWithPeer`).

### Интеграция с messaging/files
- `InventoryItem.chatId` / `threadRootMessageId` связывают карточку с чатом/тредом.
- `InventoryAttachment.descriptor` хранит метаданные файла, передача выполняется через `MeshNode.sendFile`.

### Simulator
`InventoryDemoScenario` расширен: организация, каталог, владельцы, объект, сессия, review,
QR/scan, инцидент, правила, поиск, dashboard и sync между узлами.
- media control: `toggleMicrophone`, `toggleCamera`, `switchCamera`.
- media snapshots: `observeMediaState`, `observeMediaStats`.

Legacy методы `startCall`, `sendCallSignal`, `hangupCall` оставлены для совместимости.

### Platform media adapters
- Android host: `AndroidWebRtcMediaEngineAdapter` (реальный WebRTC backend через `org.webrtc:google-webrtc`).
- Desktop host: `DesktopWebRtcMediaEngineAdapter` (реальный WebRTC backend через `dev.onvoid.webrtc`, `isSupported=true`).

## Inventory Marking (Barcode/QR/Label)

### Domain and storage
- Marking models live in inventory domain: `InventoryCode`, `InventoryLabelTemplate`, `InventoryLabel`, `InventoryPrintTask`, `InventoryScanEvent`.
- Additional entity models are defined in `backend:data/domain/entity/InventoryEntities.kt`.
- Persistent tables: `inventory_code`, `inventory_label_template`, `inventory_label`, `inventory_print_task`, `inventory_scan_event`.

### Application services
- `InventoryCodeService`:
  - generates Code128 asset code (`EL-XXXXXXXX`),
  - builds QR payload (`inventory:v1:{organizationId}:{inventoryItemId}:{assetCode}`),
  - supports regenerate/deactivate,
  - resolves scanned values,
  - records scan events.
- `InventoryLabelService`:
  - manages templates (`SHORT`, `STANDARD`, `FULL`),
  - builds label preview from inventory item + org/location/department context,
  - generates PDF and print tasks,
  - supports single and batch printing.

### Rendering and artifacts
- PDF rendering uses `InventoryLabelRendererPort`.
- Infra adapter `PdfBoxInventoryLabelRendererAdapter` renders text fields + CODE_128 + QR and returns file artifact (`FileDescriptor`, local path).

### Contract/API
`MeshNode` exposes marking operations:
- code lifecycle: `generateInventoryCode`, `getInventoryCode`, `regenerateInventoryCode`, `deactivateInventoryCode`, `findInventoryItemByCode`;
- label lifecycle: `generateInventoryLabelPreview`, `generateInventoryLabelPdf`, `printInventoryLabel`, `printInventoryLabelsBatch`;
- templates: `inventoryLabelTemplates`, `createInventoryLabelTemplate`, `updateInventoryLabelTemplate`, `selectDefaultLabelTemplate`;
- scanning: `resolveScannedCode`, `registerScanEvent`.

### UI integration
- `InventoryItemScreen`: section `Маркировка` (код, preview, печать, история).
- `InventoryScreen` and `InventorySessionScreen`: batch print blocks with template/field selection.
- `InventoryScannerScreen`: scan + resolve + quick workflow actions.
- Android/JVM platform services now expose `buildBarcode(...)` to render Code128 preview in Compose.

### Mesh event propagation
- Marking actions are persisted via inventory repositories and also written to inventory event journal.
- Events are delivered via existing inventory sync over mesh transport (no separate transport subsystem introduced).

## Hybrid Edge Node + Central Backend

### Роль `expert-link`
- `expert-link` остаётся offline-first/mesh-first edge-узлом: локальные операции и mesh-коллаборация не зависят от интернета.
- Central backend рассматривается как canonical source of truth и подключается как дополнительный контур синхронизации.
- Messenger stack сохранён без удаления: discovery, pairing, personal chats, group chats, threads, file transfer, calls, routing, multihop, relay/proxy, diagnostics.

### Central client layer
В `backend:infra` добавлен typed central client layer (`CentralApiClients.kt`) через Ktor:
- `CentralAuthClient`
- `CentralOrganizationAccessClient`
- `CentralInventoryClient`
- `CentralSyncClient`
- `CentralAttachmentClient`
- `CentralExportClient`
- `CentralClientFactory` (создание bundle клиентов)

Именование синхронизировано с canonical backend:
- используется `organization-access-service`;
- старое `user-organization-service` не используется.

### Hybrid domain model и хранилище
Добавлены модели (`backend:data/domain/model/hybrid`):
- `CentralAuthSession`, `CentralAuthState`
- `CentralOrganizationAccess`, `CentralRoleDefinition`
- `CentralSyncState`, `CentralPendingChange`, `CentralConflict`
- `CentralHybridState`, `CentralConnectivityMode`, `CentralSyncRunState`
- `CentralAttachmentArtifact`, `CentralExportTask`

Локальное состояние разделено на:
- `local state` (рабочее состояние edge-узла);
- `canonical snapshot` (последний известный снимок из central);
- `pending local changes` (очередь на выгрузку);
- `synced` (`lastUploadedSequence`, `lastPulledCursor`);
- `conflicts` (локально сохранённые conflict записи).

Persistence (`database`) дополнен таблицами:
- `central_auth_session`
- `central_organization_access`
- `central_sync_state`
- `central_organization_workspace`

и миграциями `4 -> 5` и `5 -> 6`.

### Sync/reconciliation/conflict
Оркестрация реализована в `backend:application/CentralHybridServices.kt`:
- `SyncQueueService` собирает change-set из `InventoryEvent` журнала;
- `CentralSyncOrchestrationService` делает `push`/`pull`, ведёт cursor/ack и partial-failure fallback;
- `ConflictStateService` сохраняет server conflicts и отдаёт их в UI;
- `ConnectivityModeService` и `OnlineOfflineStateService` формируют режимы работы.

Поддержаны режимы:
- `OFFLINE`
- `MESH_ONLY`
- `CENTRAL_AVAILABLE`
- `CENTRAL_DEGRADED`
- `RECONNECTING`
- `CONFLICT_REVIEW_REQUIRED`

Политика конфликтов:
- серверные конфликты не перетираются молча;
- конфликт фиксируется в локальном хранилище и переводит режим в `CONFLICT_REVIEW_REQUIRED`;
- resolution выполняется отдельной операцией `resolveCentralConflict`.

### Auth и organization context
Добавлены central auth операции в `MeshNode`:
- login/refresh/logout/auth state;
- загрузка организаций/ролей/permissions;
- выбор active organization;
- offline fallback по last-known auth/org context.

`CentralOrganizationAccessService` использует `organization-access-service` как canonical источник:
- organizations/memberships/roles/permissions;
- кэширует last-known organization access локально;
- привязывает membership к локальному `peerId`, чтобы RBAC в edge-режиме продолжал работать без сети.

### Attachment/export и storage-agnostic поведение
- `CentralAttachmentSyncService` и `CentralExportIntegrationService` работают только через central API-контракты.
- Artifact references (`artifactRef`) трактуются как opaque значения: клиент не зависит от внутренней storage-реализации backend.
- Поддерживается прозрачная работа для обоих профилей central backend:
  - `demo` (server local filesystem storage);
  - normal profiles (`dev/stage/prod`) с S3-compatible storage.

### Mesh + central coexistence
- Mesh transport и central business sync разведены по сервисам и не смешиваются в одном packet-flow.
- Локальные изменения сначала сохраняются как inventory events и доступны mesh-пирам сразу.
- При появлении central connectivity эти же изменения догоняются в canonical backend через sync-service.
- Central не отключает mesh: оба контура работают параллельно, с приоритетом canonical truth на сервере.

### Contract и runtime wiring
Расширены `contract` и `backend`:
- новые DTO/команды в `MeshHybridModels` и `MeshNode` API;
- runtime config дополнен `central` секцией;
- `MeshNodeBootstrap` и `MeshNodeComponents` связывают central clients, hybrid repositories и orchestration services;
- `MeshBackend.DefaultMeshNode` реализует новые central/hybrid операции.

### ComposeApp hybrid UX
Добавлен `HybridStore` и UI-индикация:
- login + active organization;
- статус central connectivity и auth;
- pending changes/conflicts;
- ручной sync;
- offline/mesh/central режимы в `MainScreen` и `ProfileScreen`.

Все существующие messenger/inventory экраны сохранены.

### Simulator
`HybridCentralDemoScenario` демонстрирует:
- offline create/update;
- sync push/pull с central;
- conflict + resolution;
- attachment sync;
- export request;
- scan + resolve;
- mesh-only работу с последующим central catch-up.
