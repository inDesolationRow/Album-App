package com.example.photoalbum.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "directory_table")
data class Directory (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "directory_id")
    var directoryId: Long = 0L,

    @ColumnInfo(name = "parent_id")
    var parentId: Long = -1,

    @ColumnInfo(name = "display_name")
    var displayName: String,

    @ColumnInfo(name = "size")
    var size: Int = 0,

    @ColumnInfo(name = "image_size")
    var imageSize: Int = 0,

    @ColumnInfo(name = "video_size")
    var videoSize: Int = 0,

    @ColumnInfo(name = "del_flag")
    var del: Boolean = false,

    @ColumnInfo(name = "tag")
    var tag: String = "",

    @ColumnInfo(name = "thumbnail_src")
    var thumbnail: String = "",
)