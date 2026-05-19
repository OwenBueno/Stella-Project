package com.stella.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StellaApi {
    @POST("sync/push")
    suspend fun syncPush(@Body body: SyncPushRequest): SyncPushResponse

    @GET("sync/pull")
    suspend fun syncPull(@Query("since") since: String? = null): SyncPullResponse
}
