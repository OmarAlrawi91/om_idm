package com.example.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * High-performance Coroutine-friendly Token Bucket rate limiter.
 * Regulates download throughput without dropping socket connections or jitter.
 */
class TokenBucketLimiter(
    @Volatile private var maxBytesPerSecond: Long = 0L // 0 = unlimited
) {
    private val mutex = Mutex()
    private var availableTokens: Double = 0.0
    private var lastRefillTimestamp: Long = System.currentTimeMillis()

    fun setRateLimit(bytesPerSecond: Long) {
        maxBytesPerSecond = bytesPerSecond.coerceAtLeast(0L)
    }

    fun getRateLimit(): Long = maxBytesPerSecond

    /**
     * Acquires permission to read [requestedBytes].
     * Suspends if token bucket is exhausted until sufficient tokens are refilled.
     */
    suspend fun acquire(requestedBytes: Int) {
        val limit = maxBytesPerSecond
        if (limit <= 0L) return // No speed limit active

        val targetTokens = requestedBytes.toDouble()

        while (true) {
            val waitMillis = mutex.withLock {
                val now = System.currentTimeMillis()
                val elapsedSeconds = (now - lastRefillTimestamp).coerceAtLeast(0) / 1000.0
                lastRefillTimestamp = now

                // Max bucket capacity is 1 second worth of bandwidth or at least 128KB
                val capacity = limit.toDouble().coerceAtLeast(128.0 * 1024)
                availableTokens = (availableTokens + elapsedSeconds * limit).coerceAtMost(capacity)

                if (availableTokens >= targetTokens) {
                    availableTokens -= targetTokens
                    return // Acquired successfully
                }

                // Calculate required sleep duration to accumulate the missing tokens
                val neededTokens = targetTokens - availableTokens
                val waitSec = neededTokens / limit.toDouble()
                (waitSec * 1000.0).toLong().coerceIn(10L, 500L)
            }

            delay(waitMillis)
        }
    }
}
