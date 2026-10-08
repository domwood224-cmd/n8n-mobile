package com.napcity.n8nmobile.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object N8nClient {
    private var api: N8nApi? = null
    private var currentBaseUrl: String = ""
    private var currentApiKey: String = ""

    fun getApi(baseUrl: String, apiKey: String): N8nApi {
        val normalizedUrl = baseUrl.trim().trimEnd('/')
        if (api == null || normalizedUrl != currentBaseUrl || apiKey != currentApiKey) {
            currentBaseUrl = normalizedUrl
            currentApiKey = apiKey

            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-N8N-API-KEY", apiKey)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            api = Retrofit.Builder()
                .baseUrl("$normalizedUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(N8nApi::class.java)
        }
        return api!!
    }

    fun invalidate() {
        api = null
        currentBaseUrl = ""
        currentApiKey = ""
    }
}
