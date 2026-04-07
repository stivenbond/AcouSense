package com.dspcontroller.data.mapper

import com.dspcontroller.data.db.entity.AlertEntity
import com.dspcontroller.domain.model.Alert

/**
 * Extension functions for mapping between [AlertEntity] and domain [Alert].
 */

/** Convert an [AlertEntity] to a domain [Alert]. */
fun AlertEntity.toDomain(): Alert = Alert(
    id = id,
    deviceMac = deviceMac,
    metric = metric,
    condition = condition,
    threshold = threshold,
    notify = notify == 1,
    triggered = triggered == 1
)

/** Convert a domain [Alert] to an [AlertEntity]. */
fun Alert.toEntity(): AlertEntity = AlertEntity(
    id = id,
    deviceMac = deviceMac,
    metric = metric,
    condition = condition,
    threshold = threshold,
    notify = if (notify) 1 else 0,
    triggered = if (triggered) 1 else 0
)
