package de.mobilej.ktest

import android.app.Application
import android.arch.persistence.room.Room
import android.os.StrictMode
import com.facebook.stetho.Stetho
import de.mobilej.ktest.model.AppDatabase
import de.mobilej.whitemagic.advancedAsyncTaskDebugLogging
import de.mobilej.whitemagic.advancedAsyncTaskRuntimeChecksEnabled

class App : Application() {

    companion object {
        private lateinit var app: App

        fun component(): App {
            return app
        }
    }

    init {
        App.app = this
    }

    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectActivityLeaks().build())
        Stetho.initializeWithDefaults(this)

        database = Room.databaseBuilder(this, AppDatabase::class.java, "test-db").build()

        advancedAsyncTaskDebugLogging = true
        advancedAsyncTaskRuntimeChecksEnabled = true

        // no need to initialize - it's all done in InitProvider of the library
    }

    fun getDatabase() = database

}