package com.ultrablock.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.ultrablock.data.local.AppDatabase
import com.ultrablock.data.local.entity.BlockedApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BlockedAppDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BlockedAppDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.blockedAppDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── frictionLevel field ────────────────────────────────────────────────

    @Test
    fun insertedAppWithNullFrictionLevelPersistsAsNull() = runTest {
        dao.insert(BlockedApp("com.test", "Test", frictionLevel = null))

        val retrieved = dao.getByPackageName("com.test")
        assertNotNull(retrieved)
        assertNull(retrieved!!.frictionLevel)
    }

    @Test
    fun insertedAppWithExplicitFrictionLevelPersists() = runTest {
        dao.insert(BlockedApp("com.test", "Test", frictionLevel = "GENTLE"))

        val retrieved = dao.getByPackageName("com.test")
        assertEquals("GENTLE", retrieved!!.frictionLevel)
    }

    @Test
    fun updatingFrictionLevelPreservesOtherFields() = runTest {
        val original = BlockedApp("com.test", "Test", isBlocked = true, frictionLevel = null)
        dao.insert(original)

        dao.update(original.copy(frictionLevel = "EXTREME"))

        val updated = dao.getByPackageName("com.test")
        assertEquals("EXTREME", updated!!.frictionLevel)
        assertTrue(updated.isBlocked) // preserved
        assertEquals("Test", updated.appName) // preserved
    }

    // ── blocking state ─────────────────────────────────────────────────────

    @Test
    fun getBlockedAppCountReturnsOnlyBlockedApps() = runTest {
        dao.insert(BlockedApp("com.a", "A", isBlocked = true))
        dao.insert(BlockedApp("com.b", "B", isBlocked = false))
        dao.insert(BlockedApp("com.c", "C", isBlocked = true))

        dao.getBlockedAppCount().test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isAppCurrentlyBlockedReturnsTrueForBlockedApp() = runTest {
        dao.insert(BlockedApp("com.test", "Test", isBlocked = true))

        val result = dao.isAppCurrentlyBlocked("com.test", System.currentTimeMillis())

        assertNotNull(result)
    }

    @Test
    fun isAppCurrentlyBlockedReturnsFalseForTemporarilyUnblockedApp() = runTest {
        val future = System.currentTimeMillis() + 60_000L
        dao.insert(BlockedApp("com.test", "Test", isBlocked = true, temporarilyUnblockedUntil = future))

        val result = dao.isAppCurrentlyBlocked("com.test", System.currentTimeMillis())

        assertNull(result)
    }

    @Test
    fun isAppCurrentlyBlockedReturnsTrueAfterTemporaryUnblockExpires() = runTest {
        val past = System.currentTimeMillis() - 1000L
        dao.insert(BlockedApp("com.test", "Test", isBlocked = true, temporarilyUnblockedUntil = past))

        val result = dao.isAppCurrentlyBlocked("com.test", System.currentTimeMillis())

        assertNotNull(result)
    }

    // ── temporary unblock ──────────────────────────────────────────────────

    @Test
    fun setTemporaryUnblockStoresExpiryTimestamp() = runTest {
        dao.insert(BlockedApp("com.test", "Test", isBlocked = true))
        val until = System.currentTimeMillis() + 30_000L

        dao.setTemporaryUnblock("com.test", until)

        val app = dao.getByPackageName("com.test")
        assertEquals(until, app!!.temporarilyUnblockedUntil)
    }

    @Test
    fun clearExpiredUnblocksNullifiesExpiredTimestamps() = runTest {
        val past = System.currentTimeMillis() - 1000L
        dao.insert(BlockedApp("com.test", "Test", isBlocked = true, temporarilyUnblockedUntil = past))

        dao.clearExpiredUnblocks(System.currentTimeMillis())

        val app = dao.getByPackageName("com.test")
        assertNull(app!!.temporarilyUnblockedUntil)
    }

    @Test
    fun clearExpiredUnblockDoesNotAffectActiveFutureUnblock() = runTest {
        val future = System.currentTimeMillis() + 30_000L
        dao.insert(BlockedApp("com.test", "Test", isBlocked = true, temporarilyUnblockedUntil = future))

        dao.clearExpiredUnblocks(System.currentTimeMillis())

        val app = dao.getByPackageName("com.test")
        assertEquals(future, app!!.temporarilyUnblockedUntil) // untouched
    }

    // ── CRUD ───────────────────────────────────────────────────────────────

    @Test
    fun getByPackageNameReturnsNullForUnknownPackage() = runTest {
        val result = dao.getByPackageName("com.nonexistent")
        assertNull(result)
    }

    @Test
    fun deleteRemovesApp() = runTest {
        val app = BlockedApp("com.test", "Test")
        dao.insert(app)
        dao.delete(app)

        assertNull(dao.getByPackageName("com.test"))
    }
}
