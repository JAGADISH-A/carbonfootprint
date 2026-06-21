package com.carbonwise.connect

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CarbonWiseApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            android.util.Log.d("SYNC_DEBUG", "CarbonWiseApp: workManagerConfiguration getter called!")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("SYNC_DEBUG", "CarbonWiseApp: onCreate() called!")
    }
}
