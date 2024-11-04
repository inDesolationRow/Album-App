package com.example.photoalbum.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.photoalbum.enum.StorageType

@Entity(tableName = "photo_info_table")
data class PhotoInfo (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "album_id")
    var albumId: Long = -1L,

    @ColumnInfo(name = "photo_src")
    var src:  String = "",

    @ColumnInfo(name = "thumbnail_src")
    var thumbnail: String = "",

    @ColumnInfo(name = "storage_type")
    var type: String = StorageType.LOCAL.name,

    @ColumnInfo(name = "create_date")
    var createDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "del_flag")
    var del: Boolean = false,
)
