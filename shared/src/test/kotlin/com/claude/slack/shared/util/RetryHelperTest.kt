package com.claude.slack.shared.util

import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetryHelperTest {

    @Test
    fun `should succeed on first attempt`() {
        val retryHelper = RetryHelper(maxAttempts = 3)
        val attemptCount = AtomicInteger(0)

        val result = retryHelper.execute {
            attemptCount.incrementAndGet()
            "success"
        }

        assertTrue(result is RetryHelper.RetryResult.Success)
        assertEquals("success", (result as RetryHelper.RetryResult.Success).value)
        assertEquals(1, attemptCount.get())
    }

    @Test
    fun `should retry and eventually succeed`() {
        val retryHelper = RetryHelper(
            maxAttempts = 3,
            initialDelayMs = 10, // Small delay for test speed
            multiplier = 1.0
        )
        val attemptCount = AtomicInteger(0)

        val result = retryHelper.execute {
            val count = attemptCount.incrementAndGet()
            if (count < 3) {
                throw RuntimeException("Temporary failure")
            }
            "success"
        }

        assertTrue(result is RetryHelper.RetryResult.Success)
        assertEquals("success", (result as RetryHelper.RetryResult.Success).value)
        assertEquals(3, attemptCount.get())
    }

    @Test
    fun `should fail after max attempts`() {
        val retryHelper = RetryHelper(
            maxAttempts = 3,
            initialDelayMs = 10,
            multiplier = 1.0
        )
        val attemptCount = AtomicInteger(0)

        val result = retryHelper.execute<String> {
            attemptCount.incrementAndGet()
            throw RuntimeException("Always fails")
        }

        assertTrue(result is RetryHelper.RetryResult.Failure)
        assertEquals(3, (result as RetryHelper.RetryResult.Failure).attempts)
        assertEquals(3, attemptCount.get())
    }

    @Test
    fun `should call onRetry callback`() {
        val retryNotifications = mutableListOf<Int>()
        val retryHelper = RetryHelper(
            maxAttempts = 3,
            initialDelayMs = 10,
            multiplier = 1.0,
            onRetry = { attempt, _, _ -> retryNotifications.add(attempt) }
        )
        val attemptCount = AtomicInteger(0)

        retryHelper.execute<String> {
            val count = attemptCount.incrementAndGet()
            if (count < 3) {
                throw RuntimeException("Temporary failure")
            }
            "success"
        }

        assertEquals(listOf(1, 2), retryNotifications)
    }

    @Test
    fun `should succeed with condition on first try`() {
        val retryHelper = RetryHelper(maxAttempts = 3, initialDelayMs = 10)
        val conditionChecks = AtomicInteger(0)

        val result = retryHelper.executeWithCondition(
            condition = {
                conditionChecks.incrementAndGet()
                true
            },
            block = { "success" }
        )

        assertTrue(result is RetryHelper.RetryResult.Success)
        assertEquals(1, conditionChecks.get())
    }

    @Test
    fun `should retry until condition met`() {
        val retryHelper = RetryHelper(
            maxAttempts = 5,
            initialDelayMs = 10,
            multiplier = 1.0
        )
        val conditionChecks = AtomicInteger(0)

        val result = retryHelper.executeWithCondition(
            condition = {
                conditionChecks.incrementAndGet() >= 3
            },
            block = { "success" }
        )

        assertTrue(result is RetryHelper.RetryResult.Success)
        assertEquals(3, conditionChecks.get())
    }

    @Test
    fun `should fail if condition never met`() {
        val retryHelper = RetryHelper(
            maxAttempts = 3,
            initialDelayMs = 10,
            multiplier = 1.0
        )

        val result = retryHelper.executeWithCondition(
            condition = { false },
            block = { "success" }
        )

        assertTrue(result is RetryHelper.RetryResult.Failure)
        assertEquals(3, (result as RetryHelper.RetryResult.Failure).attempts)
    }

    @Test
    fun `should respect shouldRetry predicate`() {
        val retryHelper = RetryHelper(
            maxAttempts = 5,
            initialDelayMs = 10,
            multiplier = 1.0
        )
        val attemptCount = AtomicInteger(0)

        val result = retryHelper.execute<String>(
            shouldRetry = { e -> e.message != "Fatal error" }
        ) {
            attemptCount.incrementAndGet()
            throw RuntimeException("Fatal error")
        }

        assertTrue(result is RetryHelper.RetryResult.Failure)
        assertEquals(1, attemptCount.get()) // Should not retry
    }

    @Test
    fun `factory method forServerWait should create helper with appropriate defaults`() {
        val retryNotifications = mutableListOf<Long>()
        val helper = RetryHelper.forServerWait(
            maxAttempts = 3,
            initialDelayMs = 100,
            onRetry = { _, delayMs, _ -> retryNotifications.add(delayMs) }
        )

        helper.execute<String> {
            throw RuntimeException("Always fails")
        }

        // Should have exponential-ish backoff
        assertTrue(retryNotifications.size == 2)
        assertTrue(retryNotifications[1] >= retryNotifications[0])
    }
}
