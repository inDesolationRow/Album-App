package com.example.photoalbum.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photoalbum.model.DirectoryMediaFileCrossRef

@Dao()
interface DirectoryMediaFileCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(directoryMediaFileCrossRef: DirectoryMediaFileCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: List<DirectoryMediaFileCrossRef>)

    @Query(value = "DELETE FROM directory_media_file_cross_ref")
    suspend fun clearTable()
}