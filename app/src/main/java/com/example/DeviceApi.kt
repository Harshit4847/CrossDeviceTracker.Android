package com.example

import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApi {
    @POST("api/devices")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): DeviceRegistrationResponse
}

data class DeviceRegistrationRequest(
    val deviceName: String,
    val platform: String,
    val installationId: String
)

data class DeviceRegistrationResponse(
    val deviceId: String,
    val deviceJwt: String
)
