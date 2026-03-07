package org.expert.link.mesh.contract.api

/**
 * Платформенная поддержка multicast-discovery.
 *
 * Используется backend-ядром как boundary для platform-specific подготовки
 * (например, Android MulticastLock) без прямой зависимости backend-кода от Android API.
 */
interface MeshMulticastSupport {
    /** Возвращает `true`, если multicast доступен на текущей платформе и в текущей сети. */
    suspend fun isMulticastSupported(): Boolean

    /** Выполняет подготовку перед multicast discovery. */
    suspend fun prepareForMulticast()

    /** Освобождает ресурсы после остановки discovery. */
    suspend fun releaseMulticast() {}
}
