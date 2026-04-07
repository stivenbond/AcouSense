package com.dspcontroller.di

import com.dspcontroller.domain.repository.AlertRepository
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.domain.repository.DeviceRepository
import com.dspcontroller.domain.repository.PresetRepository
import com.dspcontroller.domain.repository.TelemetryRepository
import com.dspcontroller.domain.usecase.ConnectToDeviceUseCase
import com.dspcontroller.domain.usecase.EvaluateAlertsUseCase
import com.dspcontroller.domain.usecase.GetAlertsUseCase
import com.dspcontroller.domain.usecase.LoadPresetUseCase
import com.dspcontroller.domain.usecase.ObserveTelemetryUseCase
import com.dspcontroller.domain.usecase.SavePresetUseCase
import com.dspcontroller.domain.usecase.SendParamUseCase
import com.dspcontroller.domain.usecase.SyncPresetsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing all domain use case instances.
 *
 * Use cases are not singletons — they are lightweight and stateless,
 * so a new instance per injection site is fine.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideConnectToDeviceUseCase(
        connectionRepository: ConnectionRepository,
        deviceRepository: DeviceRepository
    ): ConnectToDeviceUseCase = ConnectToDeviceUseCase(connectionRepository, deviceRepository)

    @Provides
    fun provideSendParamUseCase(
        connectionRepository: ConnectionRepository
    ): SendParamUseCase = SendParamUseCase(connectionRepository)

    @Provides
    fun provideLoadPresetUseCase(
        connectionRepository: ConnectionRepository
    ): LoadPresetUseCase = LoadPresetUseCase(connectionRepository)

    @Provides
    fun provideSavePresetUseCase(
        presetRepository: PresetRepository
    ): SavePresetUseCase = SavePresetUseCase(presetRepository)

    @Provides
    fun provideSyncPresetsUseCase(
        presetRepository: PresetRepository
    ): SyncPresetsUseCase = SyncPresetsUseCase(presetRepository)

    @Provides
    fun provideObserveTelemetryUseCase(
        telemetryRepository: TelemetryRepository
    ): ObserveTelemetryUseCase = ObserveTelemetryUseCase(telemetryRepository)

    @Provides
    fun provideGetAlertsUseCase(
        alertRepository: AlertRepository
    ): GetAlertsUseCase = GetAlertsUseCase(alertRepository)

    @Provides
    fun provideEvaluateAlertsUseCase(
        alertRepository: AlertRepository
    ): EvaluateAlertsUseCase = EvaluateAlertsUseCase(alertRepository)
}
