package de.mobilej.ktest.model

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE

@Entity(tableName = "website")
data class WebSite(@ColumnInfo(name = "url") var url: String) {
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Dao
interface WebSiteDao {

    @Query("select * from website")
    fun getAllLiveData(): LiveData<List<WebSite>>

    @Query("select * from website")
    fun getAll(): List<WebSite>

    @Query("select * from website where id = :id")
    fun findById(id: Long): WebSite

    @Insert(onConflict = REPLACE)
    fun insert(site: WebSite)

    @Update(onConflict = REPLACE)
    fun update(site: WebSite)

    @Delete
    fun delete(site: WebSite)
}