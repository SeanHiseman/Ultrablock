package com.ultrablock.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val testDbName = "migration_test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    // ── Migration 1 → 2 ────────────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migration1To2AddsNewTablesAndColumn() {
        // Create v1 schema
        helper.createDatabase(testDbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO blocked_apps (packageName, appName, isBlocked) VALUES " +
                        "('com.existing', 'Existing App', 1)"
            )
        }

        // Run migration and get the v2 database
        val db = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        // Verify existing data is preserved
        db.query("SELECT packageName, appName FROM blocked_apps").use { cursor ->
            assertTrue("Existing row should be preserved", cursor.moveToFirst())
            assertEquals("com.existing", cursor.getString(0))
            assertEquals("Existing App", cursor.getString(1))
        }

        // Verify frictionLevel column was added (should be NULL for pre-existing rows)
        db.query("SELECT frictionLevel FROM blocked_apps WHERE packageName = 'com.existing'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("frictionLevel should be null for migrated rows", cursor.isNull(0))
        }

        // Verify app_usage_sessions table was created
        db.query("SELECT COUNT(*) FROM app_usage_sessions").use { cursor ->
            assertTrue("app_usage_sessions table should exist", cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }

        // Verify accountability_partners table was created
        db.query("SELECT COUNT(*) FROM accountability_partners").use { cursor ->
            assertTrue("accountability_partners table should exist", cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration1To2AppUsageSessionsHasCorrectColumns() {
        helper.createDatabase(testDbName, 1).use { /* nothing to insert */ }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        // Insert a row into app_usage_sessions to verify schema
        db.execSQL(
            "INSERT INTO app_usage_sessions " +
                    "(packageName, appName, blockedAtTimestamp, wasUnblocked, unblockDurationMinutes, frictionLevel) " +
                    "VALUES ('com.test', 'Test', 1000, 0, 0, 'STRICT')"
        )

        db.query("SELECT * FROM app_usage_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val colNames = cursor.columnNames.toSet()
            assertTrue("id column missing", colNames.contains("id"))
            assertTrue("packageName column missing", colNames.contains("packageName"))
            assertTrue("wasUnblocked column missing", colNames.contains("wasUnblocked"))
            assertTrue("frictionLevel column missing", colNames.contains("frictionLevel"))
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration1To2AccountabilityPartnersHasCorrectColumns() {
        helper.createDatabase(testDbName, 1).use { /* nothing */ }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        db.execSQL(
            "INSERT INTO accountability_partners " +
                    "(partnerCode, displayName, addedAt, lastKnownBlockCount, lastKnownSuccessRate, lastSyncAt) " +
                    "VALUES ('UB-A1B2-C3D4', 'Alice', 1000, 0, 0.0, 0)"
        )

        db.query("SELECT * FROM accountability_partners").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val colNames = cursor.columnNames.toSet()
            assertTrue("partnerCode column missing", colNames.contains("partnerCode"))
            assertTrue("displayName column missing", colNames.contains("displayName"))
            assertTrue("lastKnownSuccessRate column missing", colNames.contains("lastKnownSuccessRate"))
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration1To2PreservesMultipleExistingRows() {
        helper.createDatabase(testDbName, 1).use { db ->
            db.execSQL("INSERT INTO blocked_apps (packageName, appName, isBlocked) VALUES ('com.a', 'A', 1)")
            db.execSQL("INSERT INTO blocked_apps (packageName, appName, isBlocked) VALUES ('com.b', 'B', 0)")
            db.execSQL("INSERT INTO blocked_apps (packageName, appName, isBlocked) VALUES ('com.c', 'C', 1)")
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        db.query("SELECT COUNT(*) FROM blocked_apps").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("All 3 rows should be preserved", 3, cursor.getInt(0))
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration1To2CanInsertNewRowsWithFrictionLevel() {
        helper.createDatabase(testDbName, 1).use { /* nothing */ }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        // Should be able to insert with frictionLevel
        db.execSQL(
            "INSERT INTO blocked_apps (packageName, appName, isBlocked, frictionLevel) " +
                    "VALUES ('com.new', 'New App', 1, 'GENTLE')"
        )

        db.query("SELECT frictionLevel FROM blocked_apps WHERE packageName = 'com.new'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("GENTLE", cursor.getString(0))
        }

        db.close()
    }
}
