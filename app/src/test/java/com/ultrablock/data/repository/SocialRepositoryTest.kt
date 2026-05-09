package com.ultrablock.data.repository

import app.cash.turbine.test
import com.ultrablock.data.local.dao.AccountabilityPartnerDao
import com.ultrablock.data.local.entity.AccountabilityPartner
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SocialRepositoryTest {

    private val dao = mockk<AccountabilityPartnerDao>()
    private lateinit var repository: SocialRepository

    @Before
    fun setup() {
        repository = SocialRepository(dao)
    }

    // ── addPartner ─────────────────────────────────────────────────────────

    @Test
    fun `addPartner succeeds with valid code and name`() = runTest {
        coEvery { dao.getByCode(any()) } returns null
        coJustRun { dao.insert(any()) }

        val result = repository.addPartner("UB-A1B2-C3D4", "Alice")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `addPartner inserts partner with uppercased code`() = runTest {
        val slot = slot<AccountabilityPartner>()
        coEvery { dao.getByCode(any()) } returns null
        coEvery { dao.insert(capture(slot)) } returns Unit

        repository.addPartner("ub-a1b2-c3d4", "Alice")

        assertEquals("UB-A1B2-C3D4", slot.captured.partnerCode)
    }

    @Test
    fun `addPartner trims whitespace from display name`() = runTest {
        val slot = slot<AccountabilityPartner>()
        coEvery { dao.getByCode(any()) } returns null
        coEvery { dao.insert(capture(slot)) } returns Unit

        repository.addPartner("UB-A1B2-C3D4", "  Alice  ")

        assertEquals("Alice", slot.captured.displayName)
    }

    @Test
    fun `addPartner fails with blank code`() = runTest {
        val result = repository.addPartner("", "Alice")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `addPartner fails with whitespace-only code`() = runTest {
        val result = repository.addPartner("   ", "Alice")

        assertTrue(result.isFailure)
    }

    @Test
    fun `addPartner fails with blank display name`() = runTest {
        val result = repository.addPartner("UB-A1B2-C3D4", "")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `addPartner fails when partner with same code already exists`() = runTest {
        coEvery { dao.getByCode("UB-A1B2-C3D4") } returns
                AccountabilityPartner("UB-A1B2-C3D4", "Existing")

        val result = repository.addPartner("UB-A1B2-C3D4", "Alice")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already") == true)
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `addPartner error message is user-friendly`() = runTest {
        val result = repository.addPartner("", "Alice")

        val message = result.exceptionOrNull()?.message
        assertTrue(!message.isNullOrBlank())
    }

    // ── removePartner ──────────────────────────────────────────────────────

    @Test
    fun `removePartner calls dao delete with the partner`() = runTest {
        val partner = AccountabilityPartner("UB-A1B2-C3D4", "Alice")
        coJustRun { dao.delete(any()) }

        repository.removePartner(partner)

        coVerify(exactly = 1) { dao.delete(partner) }
    }

    // ── getAllPartners ─────────────────────────────────────────────────────

    @Test
    fun `getAllPartners returns flow from dao`() = runTest {
        val partners = listOf(
            AccountabilityPartner("UB-A1B2-C3D4", "Alice"),
            AccountabilityPartner("UB-E5F6-G7H8", "Bob")
        )
        every { dao.getAllPartners() } returns flowOf(partners)

        repository.getAllPartners().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Alice", result[0].displayName)
            assertEquals("Bob", result[1].displayName)
            awaitComplete()
        }
    }

    @Test
    fun `getAllPartners returns empty list when no partners`() = runTest {
        every { dao.getAllPartners() } returns flowOf(emptyList())

        repository.getAllPartners().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }
}
