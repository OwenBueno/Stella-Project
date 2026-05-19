package com.stella.core.network

import com.stella.core.data.SettingsRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Rewrites request host/scheme/port from saved API URL so Settings changes apply without app restart.
 */
class DynamicBaseUrlInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured = settingsRepository.getApiBaseUrl().trimEnd('/').toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val path = request.url.encodedPath
        val query = request.url.encodedQuery

        val newUrl = configured.newBuilder()
            .encodedPath(path)
            .apply {
                if (query != null) encodedQuery(query)
            }
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
