package com.example.photoalbum.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_net_storage_info_table")
data class LocalNetStorageInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "display_name")
    val displayName: String = "",
    val ip: String = "",
    val user: String = "",
    val password: String = "",
    val shared: String = "",
    val usable: Boolean = false
)