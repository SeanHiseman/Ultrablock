package com.ultrablock.domain

import com.ultrablock.domain.model.FrictionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class FrictionLevelTest {

    @Test
    fun `has exactly four levels`() {
        assertEquals(4, FrictionLevel.values().size)
    }

    @Test
    fun `each level has a non-empty display name`() {
        FrictionLevel.values().forEach { level ->
            assertFalse("${level.name} display name is empty", level.displayName.isEmpty())
        }
    }

    @Test
    fun `each level has a non-empty description`() {
        FrictionLevel.values().forEach { level ->
            assertFalse("${level.name} description is empty", level.description.isEmpty())
        }
    }

    @Test
    fun `gentle display name is Gentle`() {
        assertEquals("Gentle", FrictionLevel.GENTLE.displayName)
    }

    @Test
    fun `moderate display name is Moderate`() {
        assertEquals("Moderate", FrictionLevel.MODERATE.displayName)
    }

    @Test
    fun `strict display name is Strict`() {
        assertEquals("Strict", FrictionLevel.STRICT.displayName)
    }

    @Test
    fun `extreme display name is Extreme`() {
        assertEquals("Extreme", FrictionLevel.EXTREME.displayName)
    }

    @Test
    fun `valueOf returns correct level for each name`() {
        FrictionLevel.values().forEach { level ->
            assertEquals(level, FrictionLevel.valueOf(level.name))
        }
    }

    @Test
    fun `gentle description mentions free or pause`() {
        val desc = FrictionLevel.GENTLE.description.lowercase()
        assert(desc.contains("pause") || desc.contains("free") || desc.contains("5")) {
            "GENTLE description should mention pause/free/5: ${FrictionLevel.GENTLE.description}"
        }
    }

    @Test
    fun `extreme description mentions locked or no`() {
        val desc = FrictionLevel.EXTREME.description.lowercase()
        assert(desc.contains("lock") || desc.contains("no")) {
            "EXTREME description should mention locked/no: ${FrictionLevel.EXTREME.description}"
        }
    }

    @Test
    fun `strict description mentions payment`() {
        val desc = FrictionLevel.STRICT.description.lowercase()
        assert(desc.contains("payment") || desc.contains("pay") || desc.contains("cost")) {
            "STRICT description should mention payment: ${FrictionLevel.STRICT.description}"
        }
    }
}
