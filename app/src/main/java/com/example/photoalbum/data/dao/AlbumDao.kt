package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.photoalbum.data.model.Album

@Dao
interface AlbumDao {

    @Insert()
    suspend fun insert(album: Album):Long

    @Query("DELETE FROM album_table WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM album_table WHERE id = :id AND del_flag = :delFlag")
    suspend fun queryById(id: Long, delFlag: Boolean): Album?

    @Query("SELECT * FROM album_table WHERE del_flag = :delFlag")
    suspend fun queryByDelFlag(delFlag: Boolean): List<Album>?

    @Query("SELECT * FROM album_table WHERE parent_id = :parentId AND del_flag = :delFlag")
    suspend fun queryByParentId(parentId: Long, delFlag: Boolean = false): List<Album>?

    @Update
    suspend fun update(album: Album)

    @Query(value = "DELETE FROM album_table WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int?

}