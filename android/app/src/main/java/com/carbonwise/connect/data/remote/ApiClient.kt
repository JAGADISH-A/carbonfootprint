package com.carbonwise.connect.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val authInterceptor: com.carbonwise.connect.data.network.AuthInterceptor
) {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    @Inject
    @Named("base_url")
    lateinit var baseUrl: String

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(if (::baseUrl.isInitialized && baseUrl.isNotEmpty()) baseUrl else "https://api.carbonwise.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val mobileApiService: com.carbonwise.connect.data.network.MobileApiService by lazy {
        Retrofit.Builder()
            .baseUrl(if (::baseUrl.isInitialized && baseUrl.isNotEmpty()) baseUrl else "https://api.carbonwise.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.carbonwise.connect.data.network.MobileApiService::class.java)
    }
}
