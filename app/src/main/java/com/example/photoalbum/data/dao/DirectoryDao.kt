package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.photoalbum.data.model.Directory
import com.example.photoalbum.data.model.DirectoryWithMediaFile

@Dao
interface DirectoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(directory: Directory): Long

    @Query(value = "SELECT * FROM directory_table WHERE path = :path")
    suspend fun queryByPath(path: String): Directory?

    @Transaction
    @Query(value = "SELECT * FROM directory_table WHERE directory_id = :directoryId")
    suspend fun queryDirectoryWithMediaFileById(directoryId: Long): DirectoryWithMediaFile?

    @Query(value = "SELECT path FROM directory_table WHERE directory_id = :directoryId")
    suspend fun getPathById(directoryId: Long): String?

    @Query(value = "SELECT * FROM directory_table WHERE parent_id = :parentId ORDER BY path ASC")
    suspend fun querySortedByNameByParentId(parentId: Long): List<Directory>?

    @Query(
        value = """
        SELECT d.*
        FROM directory_table AS d
        INNER JOIN album_media_file_cross_ref AS r 
        ON d.directory_id = r.media_file_id
        WHERE r.id = :albumId AND r.type = 1"""
    )
    suspend fun queryDirectoryByAlbumId(albumId: Long): List<Directory>?

    @Query(value = "DELETE FROM directory_table")
    suspend fun clearTable()
}