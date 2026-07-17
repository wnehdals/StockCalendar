package com.jdm.stockcalendar.data.remote

import okhttp3.Interceptor
import okhttp3.Response

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return chain.proceed(request)
    }
}
