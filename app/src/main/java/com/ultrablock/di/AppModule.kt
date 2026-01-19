package com.ultrablock.di

import android.content.Context
import androidx.room.Room
import com.ultrablock.data.local.AppDatabase
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
        ).build()
    }

    @Provides
    fun provideBlockedAppDao(database: AppDatabase): BlockedAppDao {
        return database.blockedAppDao()
    }

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    fun provideUnblockHistoryDao(database: AppDatabase): UnblockHistoryDao {
        return database.unblockHistoryDao()
    }
}
