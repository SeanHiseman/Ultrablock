package com.ultrablock.di

import android.content.Context
import androidx.room.Room
import com.ultrablock.data.local.AppDatabase
import com.ultrablock.data.local.dao.AccountabilityPartnerDao
import com.ultrablock.data.local.dao.AppUsageDao
import com.ultrablock.data.local.dao.BlockedAppDao
import com.ultrablock.data.local.dao.ScheduleDao
import com.ultrablock.data.local.dao.UnblockHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ultrablock_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideBlockedAppDao(database: AppDatabase): BlockedAppDao = database.blockedAppDao()

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    fun provideUnblockHistoryDao(database: AppDatabase): UnblockHistoryDao = database.unblockHistoryDao()

    @Provides
    fun provideAppUsageDao(database: AppDatabase): AppUsageDao = database.appUsageDao()

    @Provides
    fun provideAccountabilityPartnerDao(database: AppDatabase): AccountabilityPartnerDao = database.accountabilityPartnerDao()
}
