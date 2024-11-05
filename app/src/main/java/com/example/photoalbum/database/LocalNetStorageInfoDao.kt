package com.example.photoalbum.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.photoalbum.database.model.LocalNetStorageInfo

@Dao
interface LocalNetStorageInfoDao {

    @Insert
    suspend fun insert(localNetStorageInfo: LocalNetStorageInfo): Long

    @Query(value = "SELECT * FROM local_net_storage_info_table ORDER BY id ASC")
    suspend fun getList(): List<LocalNetStorageInfo>?

    @Query(value = "DELETE FROM local_net_storage_info_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}