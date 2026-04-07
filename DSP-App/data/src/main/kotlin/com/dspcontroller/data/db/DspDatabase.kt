package com.dspcontroller.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dspcontroller.data.db.dao.AlertDao
import com.dspcontroller.data.db.dao.DeviceDao
import com.dspcontroller.data.db.dao.DspParamDao
import com.dspcontroller.data.db.dao.PresetDao
import com.dspcontroller.data.db.dao.TelemetryDao
import com.dspcontroller.data.db.entity.AlertEntity
import com.dspcontroller.data.db.entity.DeviceEntity
import com.dspcontroller.data.db.entity.DspParamEntity
import com.dspcontroller.data.db.entity.PresetEntity
import com.dspcontroller.data.db.entity.TelemetryEntity

/**
 * Main Room database for the DSP Controller application.
 *
 * Contains 5 entities: devices, presets, dsp_params, telemetry, alerts.
 * WAL journal mode is enabled in [com.dspcontroller.data.di.DatabaseModule].
 */
@Database(
    entities = [
        DeviceEntity::class,
        PresetEntity::class,
        DspParamEntity::class,
        TelemetryEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DspDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun presetDao(): PresetDao
    abstract fun dspParamDao(): DspParamDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val DATABASE_NAME = "dsp_controller.db"
    }
}
