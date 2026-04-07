package com.dspcontroller.data.di

import com.dspcontroller.data.repository.AlertRepositoryImpl
import com.dspcontroller.data.repository.ConnectionRepositoryImpl
import com.dspcontroller.data.repository.DeviceRepositoryImpl
import com.dspcontroller.data.repository.PresetRepositoryImpl
import com.dspcontroller.data.repository.TelemetryRepositoryImpl
import com.dspcontroller.domain.repository.AlertRepository
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.domain.repository.DeviceRepository
import com.dspcontroller.domain.repository.PresetRepository
import com.dspcontroller.domain.repository.TelemetryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository implementations to their domain interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository

    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(impl: TelemetryRepositoryImpl): TelemetryRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository
}
