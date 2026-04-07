package com.dspcontroller.data.mapper

import com.dspcontroller.data.db.entity.DeviceEntity
import com.dspcontroller.domain.model.Device

/**
 * Extension functions for mapping between [DeviceEntity] and domain [Device].
 */

/** Convert a [DeviceEntity] to a domain [Device]. */
fun DeviceEntity.toDomain(): Device = Device(
    id = id,
    mac = mac,
    label = label,
    ipLast = ipLast,
    firmware = firmware,
    addedAt = addedAt,
    lastSeen = lastSeen
)

/** Convert a domain [Device] to a [DeviceEntity]. */
fun Device.toEntity(): DeviceEntity = DeviceEntity(
    id = id,
    mac = mac,
    label = label,
    ipLast = ipLast,
    firmware = firmware,
    addedAt = addedAt,
    lastSeen = lastSeen
)
