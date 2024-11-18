package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photoalbum.data.model.Settings

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings_table WHERE id = 1 LIMIT 1")
    suspend fun getUserSettings(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: Settings)

}
