package com.example

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SessionApi {
    @POST("api/timelogs/batch")
    suspend fun uploadSessions(
        @Header("Authorization") authToken: String,
        @Body sessions: List<SessionUploadDto>
    ): Response<Unit>
}
