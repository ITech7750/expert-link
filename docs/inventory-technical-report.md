# Inventory Marking Technical Report

## 1. Что реализовано
В inventory platform добавлен production-like контур маркировки техники:
- доменные модели кодов, этикеток, шаблонов, задач печати, событий сканирования;
- backend use-cases для генерации/перевыпуска/деактивации кода;
- backend use-cases для preview/PDF/печати (single + batch);
- backend resolve-flow сканирования с регистрацией scan event;
- UI на mobile/desktop в карточке объекта, списке объектов, сессии и экране сканирования;
- тесты для code/label/scan и UI-мэппингов;
- обновлённая документация.

## 2. Выбранная стратегия кодирования

### 2.1 Barcode (Code128)
Для 1D barcode используется короткий стабильный `assetCode`:
- формат: `EL-XXXXXXXX` (8 символов из безопасного алфавита);
- генерация выполняется backend-сервисом `InventoryCodeService`;
- уникальность проверяется через `InventoryCodeRepositoryPort.findByRawValue`.

### 2.2 QR
QR содержит структурированный payload:
- формат: `inventory:v1:{organizationId}:{inventoryItemId}:{assetCode}`;
- поддержан fallback-парсинг legacy payload (`inventory:{organizationId}:{inventoryItemId}`);
- QR используется для быстрого открытия карточки и расширенного resolve-flow.

## 3. Backend: модели и хранение

### 3.1 Доменные модели
Добавлены/расширены модели:
- `InventoryCode`, `InventoryBarcodeFormat`, `InventoryLabel`, `InventoryLabelTemplate`, `InventoryPrintTask`;
- `InventoryScanEvent`, `InventoryScanResultStatus` (включая `INACTIVE`);
- `InventoryLabelFieldKey`, `InventoryLabelTemplateType`.

### 3.2 Entities/records
Реализованы сущности хранения маркировки:
- `InventoryCodeEntity`, `InventoryLabelTemplateEntity`, `InventoryPrintTaskEntity`, `InventoryScanEventEntity` (domain/entity);
- Room records: `InventoryCodeRecord`, `InventoryLabelTemplateRecord`, `InventoryLabelRecord`, `InventoryPrintTaskRecord`, `InventoryScanEventRecord`.

### 3.3 Repository ports/adapters
Поддержаны порты и адаптеры:
- `InventoryCodeRepositoryPort`;
- `InventoryLabelTemplateRepositoryPort`;
- `InventoryLabelRepositoryPort`;
- `InventoryPrintTaskRepositoryPort`;
- `InventoryScanEventRepositoryPort`.

## 4. Backend: use-cases и API

### 4.1 Коды
`InventoryCodeService` реализует:
- генерацию кода;
- получение активного кода;
- перевыпуск (`CODE_REGENERATED`);
- деактивацию;
- поиск объекта по коду (`resolveScannedCode`).

Публичные contract-операции:
- `generateInventoryCode`, `getInventoryCode`, `regenerateInventoryCode`, `deactivateInventoryCode`, `findInventoryItemByCode`.

### 4.2 Этикетки и печать
`InventoryLabelService` реализует:
- `generateLabelPreview`;
- `generateLabelPdf`;
- `printLabel`;
- `printLabelsBatch`;
- управление шаблонами (`list/create/update/selectDefault`).

Поддержаны шаблоны:
- `SHORT` (короткий);
- `STANDARD` (стандартный, default);
- `FULL` (расширенный).

Рендер PDF выполняется через `InventoryLabelRendererPort` (`PdfBoxInventoryLabelRendererAdapter`) с отрисовкой Barcode/QR.

Публичные contract-операции:
- `generateInventoryLabelPreview`, `generateInventoryLabelPdf`;
- `printInventoryLabel`, `printInventoryLabelsBatch`;
- `getInventoryLabelTemplates`, `createInventoryLabelTemplate`, `updateInventoryLabelTemplate`, `selectDefaultLabelTemplate`.

### 4.3 Сканирование
Resolve-flow в backend:
1. разбор QR payload;
2. поиск по `InventoryCodeRepositoryPort`;
3. fallback по `item.qrCode`, `item.barcode`, `inventoryNumber`;
4. возврат статуса (`RESOLVED` / `INACTIVE` / `NOT_FOUND` / `INVALID` / `ERROR`);
5. регистрация `InventoryScanEvent`.

Публичные contract-операции:
- `resolveScannedCode`;
- `registerScanEvent`;
- legacy-compatible `recordInventoryScan`.

## 5. Аудит, история, экспорт
События маркировки пишутся в общий `InventoryEvent` журнал и синхронизируются mesh-событиями:
- генерация/перевыпуск/деактивация кода;
- создание/печать этикетки;
- создание print task;
- scan event.

В UI доступны:
- история кодов (включая неактивные);
- история печати;
- история сканирования.

Данные маркировки доступны для экспортных пайплайнов через репозитории и event stream inventory-домена.

## 6. Интеграция с workflow
Маркировка встроена в inventory workflow:
- после создания объекта доступна генерация кода;
- после подтверждения объекта доступны финальные этикетки;
- после сканирования доступны действия осмотра (отметка проверки, комментарий, фото, переход в обсуждение);
- scan event фиксируется в истории объекта.

## 7. UI-реализация

### 7.1 Карточка объекта
Раздел **«Маркировка»**:
- Barcode + QR preview;
- генерация/перевыпуск/деактивация;
- шаблон и поля этикетки;
- preview/PDF/печать;
- скачивание/шаринг;
- история кодов, печати, сканирования.

### 7.2 Печать
Поток печати реализован в двух местах:
- из списка объектов (`InventoryScreen`);
- из инвентаризационной сессии (`InventorySessionScreen`).

Оба сценария поддерживают batch print, шаблоны, выбор полей и export PDF.

### 7.3 Сканирование
Экран **«Сканировать»**:
- камера + manual fallback;
- детализированный результат resolve;
- быстрые действия после успешного скана.

## 8. Тесты
Добавлены тесты:
- `InventoryCodeServiceTest`:
  - генерация кодов;
  - уникальность barcode;
  - перевыпуск/деактивация;
  - resolve inactive;
  - регистрация scan event.
- `InventoryLabelServiceTest`:
  - шаблоны этикеток;
  - preview;
  - PDF;
  - batch print;
  - интеграционная цепочка `item ↔ code ↔ label ↔ scan`.
- `UiSupportTest`:
  - UI-мэппинги статусов scan/print для экранов печати и сканирования.

## 9. Сохранение messenger stack
Подсистемы mesh/p2p, discovery, pairing, personal/group chats, threads, file transfer, calls, routing, multihop, relay/proxy readiness и diagnostics сохранены без удаления/упрощения и продолжают работать как базовый транспорт и collaboration слой для inventory.
