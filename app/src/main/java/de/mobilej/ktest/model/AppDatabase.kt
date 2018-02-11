package de.mobilej.ktest.model

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(WebSite::class), version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun webSiteDao(): WebSiteDao
}