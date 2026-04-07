package com.dspcontroller.data.repository

import com.dspcontroller.data.db.dao.DspParamDao
import com.dspcontroller.data.db.dao.PresetDao
import com.dspcontroller.data.db.entity.PresetEntity
import com.dspcontroller.domain.model.DspParam
import com.dspcontroller.domain.model.Preset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PresetRepositoryImpl].
 *
 * Uses MockK for DAO mocking and verifies:
 * - Preset saving with checksum computation
 * - Preset retrieval with parameters
 * - Preset deletion
 * - Mark synced
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresetRepositoryImplTest {

    private lateinit var presetDao: PresetDao
    private lateinit var dspParamDao: DspParamDao
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var repository: PresetRepositoryImpl

    @Before
    fun setup() {
        presetDao = mockk(relaxed = true)
        dspParamDao = mockk(relaxed = true)
        okHttpClient = OkHttpClient.Builder().build()
        repository = PresetRepositoryImpl(presetDao, dspParamDao, okHttpClient)
    }

    @Test
    fun `savePreset inserts preset and parameters`() = runTest {
        val preset = Preset(
            deviceMac = "AA:BB:CC:DD:EE:FF",
            name = "Test Preset",
            description = "Test Description",
            params = listOf(
                DspParam(key = "gain", value = 0.5f, minVal = 0f, maxVal = 1f),
                DspParam(key = "bass", value = 0.8f, minVal = 0f, maxVal = 1f)
            )
        )

        coEvery { presetDao.insert(any()) } returns 1L

        val result = repository.savePreset(preset)

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { presetDao.insert(any()) }
        coVerify { dspParamDao.deleteByPresetId(1L) }
        coVerify { dspParamDao.insertAll(any()) }
    }

    @Test
    fun `getPresetById returns preset with params`() = runTest {
        val entity = PresetEntity(
            id = 1L,
            deviceMac = "AA:BB:CC:DD:EE:FF",
            name = "Test",
            description = "",
            checksum = "abc123",
            synced = 1,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        coEvery { presetDao.getById(1L) } returns entity
        coEvery { dspParamDao.getByPresetId(1L) } returns emptyList()

        val result = repository.getPresetById(1L)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("Test", result.getOrNull()?.name)
        assertEquals(true, result.getOrNull()?.synced)
    }

    @Test
    fun `getPresetById returns null for non-existent ID`() = runTest {
        coEvery { presetDao.getById(999L) } returns null

        val result = repository.getPresetById(999L)

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun `deletePreset calls dao deleteById`() = runTest {
        coEvery { presetDao.deleteById(1L) } returns Unit

        val result = repository.deletePreset(1L)

        assertTrue(result.isSuccess)
        coVerify { presetDao.deleteById(1L) }
    }

    @Test
    fun `markSynced calls dao markSynced`() = runTest {
        coEvery { presetDao.markSynced(1L) } returns Unit

        val result = repository.markSynced(1L)

        assertTrue(result.isSuccess)
        coVerify { presetDao.markSynced(1L) }
    }

    @Test
    fun `observePresetsForDevice emits mapped presets`() = runTest {
        val entities = listOf(
            PresetEntity(
                id = 1L,
                deviceMac = "AA:BB:CC:DD:EE:FF",
                name = "Preset 1",
                description = "",
                checksum = "abc",
                synced = 0,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )

        every { presetDao.observeByDeviceMac("AA:BB:CC:DD:EE:FF") } returns flowOf(entities)
        coEvery { dspParamDao.getByPresetId(1L) } returns emptyList()

        val flow = repository.observePresetsForDevice("AA:BB:CC:DD:EE:FF")
        var emitted: List<Preset>? = null
        flow.collect { emitted = it; return@collect }

        assertNotNull(emitted)
        assertEquals(1, emitted?.size)
        assertEquals("Preset 1", emitted?.first()?.name)
    }
}
