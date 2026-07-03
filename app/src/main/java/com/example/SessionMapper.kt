package com.example

import java.time.Instant

object SessionMapper {
    fun toEntity(session: Session): SessionEntity {
        return SessionEntity(
            id = session.id,
            packageName = session.packageName,
            appName = session.appName,
            startTimeUtc = session.startTimeUtc.toEpochMilli(),
            endTimeUtc = session.endTimeUtc.toEpochMilli(),
            durationSeconds = session.durationSeconds,
            syncStatus = session.syncStatus.name,
            createdAtUtc = session.createdAtUtc.toEpochMilli(),
            errorMessage = null
        )
    }

    fun toDomain(entity: SessionEntity): Session {
        return Session(
            id = entity.id,
            packageName = entity.packageName,
            appName = entity.appName,
            startTimeUtc = Instant.ofEpochMilli(entity.startTimeUtc),
            endTimeUtc = Instant.ofEpochMilli(entity.endTimeUtc),
            durationSeconds = entity.durationSeconds,
            syncStatus = SyncStatus.valueOf(entity.syncStatus),
            createdAtUtc = Instant.ofEpochMilli(entity.createdAtUtc)
        )
    }
}
