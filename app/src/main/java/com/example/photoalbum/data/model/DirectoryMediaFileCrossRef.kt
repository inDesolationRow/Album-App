package com.example.photoalbum.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "directory_media_file_cross_ref",
    primaryKeys = ["directory_id","media_file_id"],
    indices = [
        Index(value = ["media_file_id"]),
        Index(value = ["directory_id"])
    ])
data class DirectoryMediaFileCrossRef (
    @ColumnInfo(name = "directory_id")
    val directoryId: Long,
    @ColumnInfo(name = "media_file_id")
    val mediaFileId: Long
)