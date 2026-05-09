package com.ultrablock.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.ultrablock.data.local.AppDatabase
import com.ultrablock.data.local.entity.AppUsageSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AppUsageDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppUsageDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.appUsageDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── insert ─────────────────────────────────────────────────────────────

    @Test
    fun insertReturnsPositiveId() = runTest {
        val session = buildSession("com.test", "Test")

        val id = dao.insert(session)

        assertTrue(id > 0)
    }

    @Test
    fun insertedSessionAppearsInQueryResults() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(buildSession("com.example", "Example", timestamp = now))

        dao.getSessionsSince(now - 1000).test {
            val sessions = awaitItem()
            assertEquals(1, sessions.size)
            assertEquals("com.example", sessions[0].packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── markUnblocked ──────────────────────────────────────────────────────

    @Test
    fun markUnblockedUpdatesSessionCorrectly() = runTest {
        val now = System.currentTimeMillis()
        val id = dao.insert(buildSession("com.test", "Test", timestamp = now))

        dao.markUnblocked(id, 30)

        dao.getSessionsSince(now - 1000).test {
            val sessions = awaitItem()
            val session = sessions.first { it.id == id }
            assertTrue(session.wasUnblocked)
            assertEquals(30, session.unblockDurationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markUnblockedWithZeroMinutesIsValid() = runTest {
        val now = System.currentTimeMillis()
        val id = dao.insert(buildSession("com.test", "Test", timestamp = now))

        dao.markUnblocked(id, 0)

        dao.getSessionsSince(now - 1000).test {
            val sessions = awaitItem()
            val session = sessions.first { it.id == id }
            assertTrue(session.wasUnblocked)
            assertEquals(0, session.unblockDurationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getBlockAttemptsSince ──────────────────────────────────────────────

    @Test
    fun getBlockAttemptsSinceCountsAllSessionsAfterTimestamp() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(buildSession("com.a", "A", timestamp = now - 500))
        dao.insert(buildSession("com.b", "B", timestamp = now))
        dao.insert(buildSession("com.c", "C", timestamp = now - 2000)) // before window

        dao.getBlockAttemptsSince(now - 1000).test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getBlockAttemptsSinceReturnsZeroWithNoSessions() = runTest {
        dao.getBlockAttemptsSince(System.currentTimeMillis() - 1000).test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getSuccessfulBlocksSince ───────────────────────────────────────────

    @Test
    fun getSuccessfulBlocksSinceOnlyCountsNonUnblocked() = runTest {
        val now = System.currentTimeMillis()
        val id1 = dao.insert(buildSession("com.a", "A", timestamp = now))
        val id2 = dao.insert(buildSession("com.b", "B", timestamp = now))
        dao.insert(buildSession("com.c", "C", timestamp = now)) // stays blocked
        dao.markUnblocked(id1, 15) // bypassed
        dao.markUnblocked(id2, 0)  // bypassed (free)

        dao.getSuccessfulBlocksSince(now - 1000).test {
            assertEquals(1, awaitItem()) // only com.c was truly blocked
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getUnblocksSince ───────────────────────────────────────────────────

    @Test
    fun getUnblocksSinceCountsOnlyUnblockedSessions() = runTest {
        val now = System.currentTimeMillis()
        val id = dao.insert(buildSession("com.a", "A", timestamp = now))
        dao.insert(buildSession("com.b", "B", timestamp = now)) // stays blocked
        dao.markUnblocked(id, 15)

        dao.getUnblocksSince(now - 1000).test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── multiple apps ──────────────────────────────────────────────────────

    @Test
    fun multipleSessionsForSameAppAreCountedSeparately() = runTest {
        val now = System.currentTimeMillis()
        repeat(5) { dao.insert(buildSession("com.social", "Social", timestamp = now)) }

        dao.getBlockAttemptsSince(now - 1000).test {
            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun buildSession(
        packageName: String,
        appName: String,
        timestamp: Long = System.currentTimeMillis(),
        frictionLevel: String = "STRICT"
    ) = AppUsageSession(
        packageName = packageName,
        appName = appName,
        blockedAtTimestamp = timestamp,
        frictionLevel = frictionLevel
    )
}
