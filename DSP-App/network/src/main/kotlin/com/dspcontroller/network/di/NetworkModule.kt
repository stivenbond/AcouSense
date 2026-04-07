package com.dspcontroller.network.di

import android.content.Context
import com.dspcontroller.network.NsdDiscoveryManager
import com.dspcontroller.network.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt dependency injection module for the network layer.
 *
 * Provides singleton instances of [OkHttpClient], [WebSocketManager],
 * and [NsdDiscoveryManager] to the entire application.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a configured [OkHttpClient] shared across WebSocket and REST connections.
     *
     * Configuration:
     * - 10-second connect/read/write timeouts (appropriate for LAN devices).
     * - HTTP logging at BODY level in debug builds.
     * - Ping interval of 10 seconds for WebSocket keep-alive.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Provides the singleton [WebSocketManager].
     */
    @Provides
    @Singleton
    fun provideWebSocketManager(okHttpClient: OkHttpClient): WebSocketManager {
        return WebSocketManager(okHttpClient)
    }

    /**
     * Provides the singleton [NsdDiscoveryManager].
     */
    @Provides
    @Singleton
    fun provideNsdDiscoveryManager(
        @ApplicationContext context: Context
    ): NsdDiscoveryManager {
        return NsdDiscoveryManager(context)
    }
}
