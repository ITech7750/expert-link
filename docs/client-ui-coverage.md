# Expert-Link Client UI Coverage (по `expert-link-core`)

## 1. Источник правды
Карта составлена по фактическим API модулей `expert-link-core`:
- `organization-access-service`
- `inventory-service`
- `sync-service`
- `relay-service`
- `attachment-service`
- `export-service`

## 2. Покрытие organization/access
- Organizations: список/выбор активной организации в `Profile` + `Inventory`.
- Memberships/Roles/Permissions: загрузка через central integration и применение в UI/действиях.
- Locations/Departments/Cost centers: загрузка в central workspace snapshot и запись в локальные inventory-справочники.
- Bank accounts/Organization parties: загрузка и отображение summary в `Profile`.
- User profile settings (central): отображение профиля в `Profile`.
- Organization dashboard: summary отображается в `Profile` (central workspace).

## 3. Покрытие inventory
- Items: список, карточка, создание/редактирование, история, обсуждение, вложения.
- Sessions: список/карточка, состав, прогресс, обсуждение, export, batch print.
- Incidents: список и управление статусами.
- Categories/Subcategories/Tags/Templates/Attributes: справочники и формы в `Inventory`.
- Search/Dashboard: фильтрация и dashboard секции в inventory-экранах.
- Labels/Barcode/QR: preview, PDF, печать, история сканирований/печати, управление кодами.

## 4. Покрытие sync/relay/attachments/exports
- Sync batches/pull/conflicts: `HybridStore` + экраны `Главная`/`Профиль`.
- Relay nodes: получение через `relay-service`, вывод в central workspace summary.
- Attachments: локальные вложения + central upload/sync при доступной сети.
- Export tasks: запрос и просмотр export задач в inventory/session flow.

## 5. Коммуникации и mesh runtime
- Discovery/pairing/nearby peers/topology/diagnostics: сохранены и доступны в `Main`/`Profile`.
- Personal/group chats/threads: сохранены в `Chats`.
- File transfer: раздел `Передачи`.
- Calls (audio/video/group): сохранены в call/chat flows.

## 6. Mobile/Desktop UX изменения в этом цикле
- Mobile:
  - scan flow поддерживает QR + линейные штрихкоды;
  - fallback ручного ввода остаётся в scanner screen.
- Desktop:
  - добавлена системная печать PDF-этикеток;
  - сохранены `Скачать PDF` и `Поделиться`, где доступно.
- Adaptive navigation:
  - добавлен top-level раздел `Отчёты` в основной навигации.
