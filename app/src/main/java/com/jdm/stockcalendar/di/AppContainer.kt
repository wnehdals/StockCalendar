package com.jdm.stockcalendar.di

import android.content.Context
import androidx.room.Room
import com.jdm.stockcalendar.BuildConfig
import com.jdm.stockcalendar.data.DefaultEarningsRepository
import com.jdm.stockcalendar.data.EarningsRepository
import com.jdm.stockcalendar.data.local.AppDatabase
import com.jdm.stockcalendar.data.remote.NasdaqApiService
import com.jdm.stockcalendar.data.remote.UserAgentInterceptor
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val DATABASE_NAME = "stock_calendar.db"

class AppContainer(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor())
        .addNetworkInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(NasdaqApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()

    val nasdaqApiService: NasdaqApiService = retrofit.create(NasdaqApiService::class.java)

    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DATABASE_NAME,
    )
        // Pre-release app, no shipped installs to preserve across schema bumps yet.
        .fallbackToDestructiveMigration(true)
        .build()

    val earningsRepository: EarningsRepository = DefaultEarningsRepository(
        dao = database.earningsDao(),
        apiService = nasdaqApiService,
    )
}
