package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.photoalbum.data.model.Directory
import com.example.photoalbum.data.model.DirectoryWithMediaFile
import com.example.photoalbum.data.model.MediaFile

@Dao
interface DirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(directory: Directory): Long

    @Query(value = "SELECT * FROM directory_table WHERE display_name = :displayName")
    suspend fun queryByDisplayName(displayName: String): Directory?

    @Query(value = "SELECT * FROM directory_table WHERE parent_id = :parentId")
    suspend fun queryDirectoryByParentId(parentId: Long): List<Directory>?

    @Transaction
    @Query(value = "SELECT * FROM directory_table WHERE directory_id = :directoryId")
    suspend fun queryDirectoryWithMediaFileById(directoryId: Long): DirectoryWithMediaFile?

    @Transaction
    @Query(value = """ 
           SELECT media_file_table.media_file_id, 
           media_file_table.date_taken, 
           media_file_table.bucket_id, 
           media_file_table.generation_added, 
           media_file_table.bucket_display_name, 
           media_file_table.display_name, 
           media_file_table.data, 
           media_file_table.relativePath, 
           media_file_table.owner_package_name, 
           media_file_table.volume_name, 
           media_file_table.is_download, 
           media_file_table.mime_type, 
           media_file_table.size, 
           media_file_table.thumbnail_path, 
           media_file_table.orientation
           FROM media_file_table 
           INNER JOIN directory_media_file_cross_ref 
           ON media_file_table.media_file_id = directory_media_file_cross_ref.media_file_id
           WHERE directory_media_file_cross_ref.directory_id = :directoryId
           ORDER BY media_file_table.date_taken DESC""")
    suspend fun querySortedMediaFilesByDirectoryId(directoryId: Long): List<MediaFile>?

    @Transaction
    @Query(value = "SELECT * FROM directory_table WHERE parent_id = :parentId")
    suspend fun queryDirectoryWithMediaFileByParentId(parentId: Long):  List<DirectoryWithMediaFile>?

    @Transaction
    @Query(value = "SELECT * FROM directory_table")
    suspend fun queryDirectoryWithMediaFile(): List<DirectoryWithMediaFile>?

    @Query(value = "DELETE FROM directory_table")
    suspend fun clearTable()
}