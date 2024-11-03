package com.example.photoalbum.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.photoalbum.enum.AlbumType

@Entity(tableName = "album_table")
data class Album(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "parent_id")
    var parentId: Long = -1L,

    @ColumnInfo(name = "type")
    var type: String = AlbumType.MAIN_ALBUM.name,

    /*只有Type为MAIN_COLLECTION 显示在主页的相册才能自定义排序
    * 默认排序为加入主页的时间顺序*/
    @ColumnInfo(name = "order")
    var order: Int = -1,

    @ColumnInfo(name = "create_date")
    var createDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name= "photos_number")
    var photosNumber: Int = -1,

    @ColumnInfo(name = "del_flag")
    var del: Boolean = false,

    @ColumnInfo(name = "tag")
    var tag: String = ""
)
