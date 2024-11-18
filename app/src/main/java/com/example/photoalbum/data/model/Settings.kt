package com.example.photoalbum.data.model

import androidx.compose.runtime.mutableIntStateOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "settings_table")
data class Settings(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "open_local_net_storage_thumbnail")
    var openLocalNetStorageThumbnail: Boolean = true,

    @ColumnInfo(name = "grid_column_num")
    var gridColumnNum: Int = 3
) {
    @Ignore
    var gridColumnNumState = mutableIntStateOf(gridColumnNum)
}
