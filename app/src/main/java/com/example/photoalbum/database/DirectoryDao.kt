package com.example.photoalbum.database

import androidx.compose.runtime.MutableState
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

    @Query(value = "SELECT * FROM directory_table WHERE parent_id = :parentId")
    suspend fun queryDirectoryByParentId(parentId: Long): List<Directory>?

    @Transaction
    @Query(value = "SELECT * FROM directory_table WHERE display_name = :displayName")
    suspend fun queryDirectoryWithMediaFileByDisplayName(displayName: String): List<DirectoryWithMediaFile>?

    @Transaction
    @Query(value = "SELECT * FROM directory_table WHERE parent_id = :parentId")
    suspend fun queryDirectoryWithMediaFileByParentId(parentId: Long):  List<DirectoryWithMediaFile>?

    @Transaction
    @Query(value = "SELECT m.data FROM directory_table CROSS JOIN media_file_table AS m WHERE parent_id = :id ORDER BY m.date_taken DESC Limit 1")
    suspend fun queryPreviewImageByParentId(id: Long): String

    @Transaction
    @Query(value = "SELECT * FROM directory_table")
    suspend fun queryDirectoryWithMediaFile(): List<DirectoryWithMediaFile>?

    @Query(value = "DELETE FROM directory_table")
    suspend fun clearTable()
}