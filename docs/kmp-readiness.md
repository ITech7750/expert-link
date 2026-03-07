# KMP Readiness

## Common модули
- `contract`
- `backend:data`
- `composeApp/commonMain`

## JVM-only модули
- `backend:infra`
- `backend:runtime`
- `bootstrap`
- `composeApp/jvmMain`

## Границы platform-specific кода
### backend:infra
Содержит JVM-реализации:
- crypto adapter
- UDP discovery adapter
- HTTP transport adapter
- filesystem chunk storage adapter
- rendezvous relay client

### backend:runtime
Содержит JVM runtime:
- Ktor server wiring
- controller-слой
- lifecycle orchestration

### composeApp/androidMain и composeApp/jvmMain
Содержат `actual`-реализации `AppPlatformServices`:
- запуск backend-узла;
- clipboard/share/file picker;
- QR generation;
- platform media-engine wiring.

## Platform-neutral части storage
В `backend:data` расположены platform-neutral port-интерфейсы:
- persistent repository ports
- runtime cache ports
- file storage port

## Текущее правило интеграции
Клиентский код использует backend только через:
- `MeshBackend`
- `MeshNode`
- модели из `contract`
