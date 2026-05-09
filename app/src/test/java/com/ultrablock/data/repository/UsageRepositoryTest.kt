package com.ultrablock.data.repository

import app.cash.turbine.test
import com.ultrablock.data.local.dao.AppUsageDao
import com.ultrablock.data.local.entity.AppUsageSession
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class UsageRepositoryTest {

    private val dao = mockk<AppUsageDao>()
    private lateinit var repository: UsageRepository

    @Before
    fun setup() {
        repository = UsageRepository(dao)
    }

    // ── recordBlockAttempt ─────────────────────────────────────────────────

    @Test
    fun `recordBlockAttempt inserts session and returns generated id`() = runTest {
        coEvery { dao.insert(any()) } returns 42L

        val id = repository.recordBlockAttempt("com.example.app", "Example App", "STRICT")

        assertEquals(42L, id)
    }

    @Test
    fun `recordBlockAttempt inserts session with correct package name and friction level`() = runTest {
        val slot = slot<AppUsageSession>()
        coEvery { dao.insert(capture(slot)) } returns 1L

        repository.recordBlockAttempt("com.social.app", "Social App", "GENTLE")

        assertEquals("com.social.app", slot.captured.packageName)
        assertEquals("Social App", slot.captured.appName)
        assertEquals("GENTLE", slot.captured.frictionLevel)
        assertFalse(slot.captured.wasUnblocked)
        assertEquals(0, slot.captured.unblockDurationMinutes)
    }

    @Test
    fun `recordBlockAttempt sets blockedAtTimestamp close to current time`() = runTest {
        val slot = slot<AppUsageSession>()
        val before = System.currentTimeMillis()
        coEvery { dao.insert(capture(slot)) } returns 1L

        repository.recordBlockAttempt("com.test", "Test", "MODERATE")

        val after = System.currentTimeMillis()
        assertTrue(slot.captured.blockedAtTimestamp in before..after)
    }

    // ── recordUnblock ──────────────────────────────────────────────────────

    @Test
    fun `recordUnblock calls dao markUnblocked with correct arguments`() = runTest {
        coJustRun { dao.markUnblocked(any(), any()) }

        repository.recordUnblock(sessionId = 7L, durationMinutes = 30)

        coVerify(exactly = 1) { dao.markUnblocked(7L, 30) }
    }

    @Test
    fun `recordUnblock with zero minutes is valid (free bypass)`() = runTest {
        coJustRun { dao.markUnblocked(any(), any()) }

        repository.recordUnblock(sessionId = 1L, durationMinutes = 0)

        coVerify { dao.markUnblocked(1L, 0) }
    }

    // ── query flows ────────────────────────────────────────────────────────

    @Test
    fun `getTodayBlockAttempts returns flow from dao`() = runTest {
        every { dao.getBlockAttemptsSince(any()) } returns flowOf(5)

        repository.getTodayBlockAttempts().test {
            assertEquals(5, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getTodaySuccessfulBlocks returns flow from dao`() = runTest {
        every { dao.getSuccessfulBlocksSince(any()) } returns flowOf(3)

        repository.getTodaySuccessfulBlocks().test {
            assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getWeekBlockAttempts queries with 7-day window`() = runTest {
        val capturedSince = slot<Long>()
        every { dao.getBlockAttemptsSince(capture(capturedSince)) } returns flowOf(20)

        repository.getWeekBlockAttempts().test {
            awaitItem()
            awaitComplete()
        }

        // The since timestamp should be ~6 days ago (within 1 minute tolerance for test speed)
        val expectedMin = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        assertTrue(capturedSince.captured >= expectedMin - 60_000)
    }

    @Test
    fun `getTodayUnblocks delegates to dao`() = runTest {
        every { dao.getUnblocksSince(any()) } returns flowOf(2)

        repository.getTodayUnblocks().test {
            assertEquals(2, awaitItem())
            awaitComplete()
        }
    }
}
