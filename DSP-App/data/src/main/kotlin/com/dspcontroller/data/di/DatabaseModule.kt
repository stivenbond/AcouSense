package com.dspcontroller.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dspcontroller.data.db.DspDatabase
import com.dspcontroller.data.db.dao.AlertDao
import com.dspcontroller.data.db.dao.DeviceDao
import com.dspcontroller.data.db.dao.DspParamDao
import com.dspcontroller.data.db.dao.PresetDao
import com.dspcontroller.data.db.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAO instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton [DspDatabase] with WAL journal mode enabled.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DspDatabase {
        return Room.databaseBuilder(
            context,
            DspDatabase::class.java,
            DspDatabase.DATABASE_NAME
        )
            // WHY: WAL mode provides better concurrent read/write performance,
            // critical for real-time telemetry inserts alongside UI queries.
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Placeholder for future seed data or migration initialization.
                }
            })
            .build()
    }

    @Provides
    fun provideDeviceDao(db: DspDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun providePresetDao(db: DspDatabase): PresetDao = db.presetDao()

    @Provides
    fun provideDspParamDao(db: DspDatabase): DspParamDao = db.dspParamDao()

    @Provides
    fun provideTelemetryDao(db: DspDatabase): TelemetryDao = db.telemetryDao()

    @Provides
    fun provideAlertDao(db: DspDatabase): AlertDao = db.alertDao()
}
