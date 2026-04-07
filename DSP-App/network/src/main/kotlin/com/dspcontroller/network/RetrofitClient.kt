package com.dspcontroller.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Factory for creating Retrofit [RestApiService] instances.
 *
 * Because the ESP32 device IP is dynamic (discovered at runtime via mDNS or
 * manual entry), the base URL must be set on each new connection rather than
 * at app startup.
 */
object RetrofitClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Create a [RestApiService] bound to the given device IP address.
     *
     * @param okHttpClient The shared OkHttp client instance.
     * @param deviceIp The IP address of the ESP32 device on the local network.
     * @return A Retrofit-backed [RestApiService] ready for API calls.
     */
    fun create(okHttpClient: OkHttpClient, deviceIp: String): RestApiService {
        // WHY: ESP32 runs plain HTTP on the local network — no TLS.
        // If port 443 is detected in the future, switch to HTTPS.
        val baseUrl = "http://$deviceIp/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(RestApiService::class.java)
    }
}
