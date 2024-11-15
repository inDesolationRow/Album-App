package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.photoalbum.data.model.LocalNetStorageInfo

@Dao
interface LocalNetStorageInfoDao {

    @Insert
    suspend fun insert(localNetStorageInfo: LocalNetStorageInfo): Long

    @Query(value = "SELECT * FROM local_net_storage_info_table ORDER BY id ASC")
    suspend fun getList(): List<LocalNetStorageInfo>?

    @Query(value = "SELECT * FROM local_net_storage_info_table WHERE id = :id")
    suspend fun getById(id: Int): LocalNetStorageInfo?

    @Query(value = "DELETE FROM local_net_storage_info_table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Update
    suspend fun update(localNetStorageInfo: LocalNetStorageInfo)
}