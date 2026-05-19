package com.stella.core.network

import com.stella.core.data.SettingsRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = settingsRepository.getApiKey()
        val request = if (apiKey.isNotBlank()) {
            chain.request().newBuilder()
                .addHeader("X-Api-Key", apiKey)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
