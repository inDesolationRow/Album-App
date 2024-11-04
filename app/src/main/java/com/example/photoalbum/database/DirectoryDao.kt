package com.example.photoalbum.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.photoalbum.database.model.Directory
import com.example.photoalbum.database.model.DirectoryWithMediaFile

@Dao
interface DirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(directory: Directory): Long

    @Query(value = "SELECT * FROM directory_table WHERE display_name = :displayName")
    suspend fun queryByDisplayName(displayName: String): Directory?

    @Transaction
    @Query(value = "SELECT * FROM directory_table WHERE display_name = :displayName")
    suspend fun queryDirectoryWithMediaFileByDisplayName(displayName: String): List<DirectoryWithMediaFile>?

    @Transaction
    @Query(value = "SELECT * FROM directory_table")
    suspend fun queryDirectoryWithMediaFile(): List<DirectoryWithMediaFile>?

    @Query(value = "DELETE FROM directory_table")
    suspend fun clearTable()
}