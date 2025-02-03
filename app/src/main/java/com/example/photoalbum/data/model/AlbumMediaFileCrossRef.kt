package com.example.photoalbum.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "album_media_file_cross_ref",
    primaryKeys = ["id", "media_file_id"],
    indices = [
        Index(value = ["id"]),
        Index(value = ["media_file_id"]),
        Index(value = ["type"])
    ]
)
data class AlbumMediaFileCrossRef(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "media_file_id")
    val mediaFileId: Long,
    @ColumnInfo(name = "type")
    val type: Int,
)