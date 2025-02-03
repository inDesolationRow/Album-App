package com.example.photoalbum.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_table")
data class Album(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "parent_id")
    var parentId: Long = -1L,

    @ColumnInfo(name = "order")
    var order: Int = -1,

    @ColumnInfo(name = "create_date")
    var createDate: Long = -1,

    @ColumnInfo(name= "photos_number")
    var photosNumber: Int = -1,

    @ColumnInfo(name = "del_flag")
    var del: Boolean = false,

    @ColumnInfo(name = "tag")
    var tag: String = ""
)
