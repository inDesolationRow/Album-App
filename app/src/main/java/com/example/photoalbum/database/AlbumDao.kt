package com.example.photoalbum.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.photoalbum.enums.AlbumType
import com.example.photoalbum.database.model.Album

@Dao
interface AlbumDao {

    @Insert()
    suspend fun insert(album: Album)

    @Query("DELETE FROM album_table WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM album_table WHERE id = :id AND del_flag = :delFlag")
    suspend fun queryById(id: Long, delFlag: Boolean): Album?

    @Query("SELECT * FROM album_table WHERE type = :type AND del_flag = :delFlag")
    suspend fun queryMainAlbum(type: String = AlbumType.MAIN_ALBUM.name,delFlag: Boolean = false): List<Album>?

    @Query("SELECT * FROM album_table WHERE del_flag = :delFlag")
    suspend fun queryByDelFlag(delFlag: Boolean): List<Album>?

    @Query("SELECT * FROM album_table WHERE parent_id = :parentId AND del_flag = :delFlag")
    suspend fun queryByParentId(parentId: Long, delFlag: Boolean = false): List<Album>?

    @Update
    suspend fun update(album: Album)
}