package com.example.photoalbum.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_net_storage_directory_table")
data class LocalNetStorageDirectory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "connect_id")
    val connectId: Int,
    val path: String
)