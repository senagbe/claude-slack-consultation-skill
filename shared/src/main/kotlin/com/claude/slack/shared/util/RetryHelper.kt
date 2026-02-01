package com.claude.slack.shared.util

import org.slf4j.LoggerFactory
import kotlin.math.min
import kotlin.math.pow

class RetryHelper(
    private val maxAttempts: Int = 5,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000,
    private val multiplier: Double = 2.0,
    private val onRetry: ((attempt: Int, delayMs: Long, exception: Exception?) -> Unit)? = null
) {
    private val logger = LoggerFactory.getLogger(RetryHelper::class.java)

    sealed class RetryResult<T> {
        data class Success<T>(val value: T) : RetryResult<T>()
        data class Failure<T>(val attempts: Int, val lastException: Exception?) : RetryResult<T>()
    }

    fun <T> execute(
        shouldRetry: (Exception) -> Boolean = { true },
        block: () -> T
    ): RetryResult<T> {
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                val result = block()
                logger.debug("Operation succeeded on attempt $attempt")
                return RetryResult.Success(result)
            } catch (e: Exception) {
                lastException = e
                logger.debug("Attempt $attempt failed: ${e.message}")

                if (attempt == maxAttempts || !shouldRetry(e)) {
                    logger.warn("All retry attempts exhausted or retry not applicable")
                    return RetryResult.Failure(attempt, lastException)
                }

                val delay = calculateDelay(attempt)
                onRetry?.invoke(attempt, delay, e)

                logger.debug("Retrying in ${delay}ms (attempt ${attempt + 1}/$maxAttempts)")
                Thread.sleep(delay)
            }
        }

        return RetryResult.Failure(maxAttempts, lastException)
    }

    fun <T> executeWithCondition(
        condition: () -> Boolean,
        block: () -> T
    ): RetryResult<T> {
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                if (condition()) {
                    val result = block()
                    logger.debug("Operation succeeded on attempt $attempt")
                    return RetryResult.Success(result)
                } else {
                    logger.debug("Condition not met on attempt $attempt")
                }
            } catch (e: Exception) {
                lastException = e
                logger.debug("Attempt $attempt failed: ${e.message}")
            }

            if (attempt < maxAttempts) {
                val delay = calculateDelay(attempt)
                onRetry?.invoke(attempt, delay, lastException)

                logger.debug("Retrying in ${delay}ms (attempt ${attempt + 1}/$maxAttempts)")
                Thread.sleep(delay)
            }
        }

        return RetryResult.Failure(maxAttempts, lastException)
    }

    private fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = (initialDelayMs * multiplier.pow(attempt - 1)).toLong()
        return min(exponentialDelay, maxDelayMs)
    }

    companion object {
        fun withDefaults(
            onRetry: ((attempt: Int, delayMs: Long, exception: Exception?) -> Unit)? = null
        ): RetryHelper {
            return RetryHelper(onRetry = onRetry)
        }

        fun forServerWait(
            maxAttempts: Int = 10,
            initialDelayMs: Long = 2000,
            onRetry: ((attempt: Int, delayMs: Long, exception: Exception?) -> Unit)? = null
        ): RetryHelper {
            return RetryHelper(
                maxAttempts = maxAttempts,
                initialDelayMs = initialDelayMs,
                maxDelayMs = 15000,
                multiplier = 1.5,
                onRetry = onRetry
            )
        }
    }
}
