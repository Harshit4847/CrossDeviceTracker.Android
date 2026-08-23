package com.example

object SessionUploadMapper {
    fun toDto(session: Session): SessionUploadDto {
        return SessionUploadDto(
            id = session.id,
            packageName = session.packageName,
            appName = session.appName,
            startTimeUtc = session.startTimeUtc.toString(),
            endTimeUtc = session.endTimeUtc.toString(),
            durationSeconds = session.durationSeconds,
            createdAtUtc = session.createdAtUtc.toString()
        )
    }
}
