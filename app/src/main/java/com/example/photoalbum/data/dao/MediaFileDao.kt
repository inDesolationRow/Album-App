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

    @Transaction
    @Query(
        value = """ 
        SELECT m.*
        FROM media_file_table AS m
        INNER JOIN directory_media_file_cross_ref  AS r
        ON m.media_file_id = r.media_file_id
        WHERE r.directory_id = :directoryId
        ORDER BY m.date_taken DESC"""
    )
    suspend fun querySortedMediaFilesByDirectoryId(directoryId: Long): List<MediaFile>?

    @Query(value = """
        SELECT m.*
        FROM media_file_table AS m
        INNER JOIN album_media_file_cross_ref AS r 
        ON m.media_file_id = r.media_file_id
        WHERE r.id = :albumId AND r.type IN (2, 3)""")
    suspend fun queryByAlbumId(albumId: Long):List<MediaFile>?

    @Query(value = "DELETE FROM media_file_table")
    suspend fun clearTable()

    @Query(value = "SELECT MAX(generation_added) FROM media_file_table")
    suspend fun getMaxGenerationAdded(): Int?

    @Query(value = "DELETE FROM media_file_table WHERE media_file_id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int?

}