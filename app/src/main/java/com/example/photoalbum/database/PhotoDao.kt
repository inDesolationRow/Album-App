package com.example.photoalbum.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.photoalbum.model.Album
import com.example.photoalbum.model.PhotoInfo

@Dao
interface PhotoDao {

    @Insert
    suspend fun insert(photo: PhotoInfo)

    @Query("DELETE FROM photo_info_table WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM photo_info_table WHERE album_id = :albumId AND del_flag =:delFlag")
    suspend fun queryByAlbumId(albumId: Long, delFlag: Boolean = false): List<PhotoInfo>?

    @Query("SELECT * FROM photo_info_table WHERE del_flag = :delFlag")
    suspend fun queryByDelFlag(delFlag: Boolean): List<PhotoInfo>?

    @Update
    suspend fun update(photo: PhotoInfo)
}