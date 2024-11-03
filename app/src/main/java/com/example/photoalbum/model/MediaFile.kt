package com.example.photoalbum.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_file_table")
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "media_file_id")
    var mediaFileId: Long = 0L,

    @ColumnInfo(name = "date_taken")
    var dateTaken: Long, //时间戳

    @ColumnInfo(name = "bucket_id")
    var bucketId: Long, //分组id

    @ColumnInfo(name = "generation_added")
    var generationAdded: Int, //添加顺序id，单调递增

    @ColumnInfo(name = "bucket_display_name")
    var bucketDisplayName: String, //分组名

    @ColumnInfo(name = "display_name")
    var displayName: String,

    @ColumnInfo()
    var data: String,

    @ColumnInfo(name = "relativePath")
    var relativePath: String,

    @ColumnInfo(name = "owner_package_name")
    var ownerPackageName: String,

    @ColumnInfo(name = "volume_name")
    var volumeName: String,

    @ColumnInfo(name = "is_download")
    var isDownload: String,

    @ColumnInfo(name = "mime_type")
    var mimeType: String,
)
