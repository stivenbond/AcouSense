package com.dspcontroller.network

import com.dspcontroller.network.dto.DeviceInfoDto
import com.dspcontroller.network.dto.PresetDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface defining the ESP32 REST API endpoints.
 *
 * Base URL is dynamically set via [RetrofitClient] based on the connected device IP.
 */
interface RestApiService {

    /**
     * List all presets stored on the device.
     */
    @GET("api/presets")
    suspend fun getPresets(): List<PresetDto>

    /**
     * Get a single preset by ID, including its full parameters array.
     */
    @GET("api/presets/{id}")
    suspend fun getPresetById(@Path("id") id: Int): PresetDto

    /**
     * Save a new preset to the device.
     *
     * @param preset The preset data to save.
     * @return The saved preset with its assigned device-side ID.
     */
    @POST("api/presets")
    suspend fun savePreset(@Body preset: PresetDto): PresetDto

    /**
     * Delete a preset from the device by ID.
     */
    @DELETE("api/presets/{id}")
    suspend fun deletePreset(@Path("id") id: Int)

    /**
     * Get device information (MAC, firmware version, uptime).
     */
    @GET("api/info")
    suspend fun getDeviceInfo(): DeviceInfoDto
}
