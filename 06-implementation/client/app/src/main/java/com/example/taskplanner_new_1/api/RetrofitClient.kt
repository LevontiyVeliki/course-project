package com.example.taskplanner_new_1.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Для эмулятора Android используй 10.0.2.2 вместо localhost
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Токен хранится здесь после логина
    var token: String? = null

    private val authInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        val t = token
        val request = if (!t.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $t")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
