package com.ultrablock.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.ultrablock.data.local.AppDatabase
import com.ultrablock.data.local.entity.AccountabilityPartner
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
class AccountabilityPartnerDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AccountabilityPartnerDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.accountabilityPartnerDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── insert ─────────────────────────────────────────────────────────────

    @Test
    fun insertedPartnerAppearsInGetAll() = runTest {
        dao.insert(partner("UB-A1B2-C3D4", "Alice"))

        dao.getAllPartners().test {
            val partners = awaitItem()
            assertEquals(1, partners.size)
            assertEquals("UB-A1B2-C3D4", partners[0].partnerCode)
            assertEquals("Alice", partners[0].displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertReplacesDuplicateCode() = runTest {
        dao.insert(partner("UB-A1B2-C3D4", "Alice"))
        dao.insert(partner("UB-A1B2-C3D4", "Alice Updated"))

        dao.getAllPartners().test {
            val partners = awaitItem()
            assertEquals(1, partners.size)
            assertEquals("Alice Updated", partners[0].displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getByCode ──────────────────────────────────────────────────────────

    @Test
    fun getByCodeReturnsPartnerWhenExists() = runTest {
        dao.insert(partner("UB-A1B2-C3D4", "Alice"))

        val result = dao.getByCode("UB-A1B2-C3D4")

        assertNotNull(result)
        assertEquals("Alice", result!!.displayName)
    }

    @Test
    fun getByCodeReturnsNullWhenNotFound() = runTest {
        val result = dao.getByCode("UB-XXXX-XXXX")

        assertNull(result)
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    fun deletedPartnerDisappearsFromGetAll() = runTest {
        val p = partner("UB-A1B2-C3D4", "Alice")
        dao.insert(p)
        dao.delete(p)

        dao.getAllPartners().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletingOnePartnerLeavesOthersIntact() = runTest {
        val alice = partner("UB-A1B2-C3D4", "Alice")
        val bob = partner("UB-E5F6-G7H8", "Bob")
        dao.insert(alice)
        dao.insert(bob)

        dao.delete(alice)

        dao.getAllPartners().test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals("Bob", remaining[0].displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getAllPartners ordering ────────────────────────────────────────────

    @Test
    fun getAllPartnersReturnsInDescendingAddedAtOrder() = runTest {
        val earlier = partner("UB-A1B2-C3D4", "Alice", addedAt = 1000L)
        val later = partner("UB-E5F6-G7H8", "Bob", addedAt = 2000L)
        dao.insert(earlier)
        dao.insert(later)

        dao.getAllPartners().test {
            val partners = awaitItem()
            assertEquals("Bob", partners[0].displayName)   // most recent first
            assertEquals("Alice", partners[1].displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── updatePartnerStats ─────────────────────────────────────────────────

    @Test
    fun updatePartnerStatsChangesStoredValues() = runTest {
        dao.insert(partner("UB-A1B2-C3D4", "Alice"))

        dao.updatePartnerStats(
            code = "UB-A1B2-C3D4",
            blockCount = 42,
            successRate = 0.85f,
            syncAt = 9999L
        )

        val updated = dao.getByCode("UB-A1B2-C3D4")
        assertNotNull(updated)
        assertEquals(42, updated!!.lastKnownBlockCount)
        assertEquals(0.85f, updated.lastKnownSuccessRate, 0.001f)
        assertEquals(9999L, updated.lastSyncAt)
    }

    // ── empty state ────────────────────────────────────────────────────────

    @Test
    fun getAllPartnersEmitsEmptyListWhenNoPartnersExist() = runTest {
        dao.getAllPartners().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun partner(code: String, name: String, addedAt: Long = System.currentTimeMillis()) =
        AccountabilityPartner(partnerCode = code, displayName = name, addedAt = addedAt)
}
