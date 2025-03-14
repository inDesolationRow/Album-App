package com.example.photoalbum.data.model

import android.util.Size
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
    var gridColumnNum: Int = 3,

    @ColumnInfo(name = "low_memory_mode")
    var lowMemoryMode: Boolean = false,

    @ColumnInfo(name = "high_pixel_thumbnail")
    var highPixelThumbnail: Boolean = false,

    @ColumnInfo(name = "smb_mode")
    var smbMode: Boolean = false,
) {
    @Ignore
    var phoneSize: Size? = null

    @Ignore
    var expandDuration: Int = 400

    @Ignore
    var gridColumnNumState = mutableIntStateOf(gridColumnNum)

    @Ignore
    var maxSizeLarge = 90

    @Ignore
    var initialLoadSizeLarge = 30

    @Ignore
    var pageSizeLarge = 30

    @Ignore
    var prefetchDistanceLarge = 20

    @Ignore
    var maxSizeMedium = 60

    @Ignore
    var initialLoadSizeMedium = 20

    @Ignore
    var pageSizeMedium = 20

    @Ignore
    var prefetchDistanceMedium = 10

}
