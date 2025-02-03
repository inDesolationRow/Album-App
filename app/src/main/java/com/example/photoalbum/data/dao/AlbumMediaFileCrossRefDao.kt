package com.example.photoalbum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photoalbum.data.model.AlbumMediaFileCrossRef

@Dao
interface AlbumMediaFileCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(albumMediaFileCrossRef: AlbumMediaFileCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: List<AlbumMediaFileCrossRef>)

    @Query(value = "DELETE FROM album_media_file_cross_ref")
    suspend fun clearTable()

}