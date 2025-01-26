package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.photoalbum.data.model.MediaFile

@Dao
interface MediaFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: List<MediaFile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaFile: MediaFile): Long

    @Query(value = "UPDATE media_file_table SET thumbnail_path = :thumbnail WHERE media_file_id = :id")
    suspend fun updateThumbnail(id: Long, thumbnail: String)

    @Query(value = "SELECT * FROM media_file_table")
    suspend fun query(): List<MediaFile>?

    @Query(value = "SELECT * FROM media_file_table WHERE media_file_id = :id")
    suspend fun queryById(id: Long): MediaFile?

    @Query(value = "DELETE FROM media_file_table")
    suspend fun clearTable()

    @Query(value = "SELECT MAX(generation_added) FROM media_file_table")
    suspend fun getMaxGenerationAdded(): Int?

    @Query(value = "DELETE FROM media_file_table WHERE media_file_id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int?

}