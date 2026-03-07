package org.expert.link.mesh.application.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Фоновый планировщик повторов для пакетов без подтверждения. */
class RetrySchedulerService(
    private val deliveryTrackingService: DeliveryTrackingService,
    private val pollIntervalMillis: Long = 1_000,
) {
    private var job: Job? = null

    /**
     * Starts the retry scheduler inside the supplied scope.
     */
    fun start(scope: CoroutineScope) {
        if (job != null) {
            return
        }
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                deliveryTrackingService.retryExpired()
                delay(pollIntervalMillis)
            }
        }
    }

    /**
     * Stops the scheduler.
     */
    suspend fun stop() {
        job?.cancel()
        job = null
    }
}
