package com.example.photoalbum.enum

enum class AlbumType{
    MAIN_ALBUM, //显示在主页的相册
    MIXED_ALBUM, //即有相册又有照片
    ALBUM, //只有照片
}

enum class StorageType{
    LOCAL,
    CLOUD
}

enum class NavType{
    MEDIA_LIST,
    FAVORITE,
    SETTINGS
}

enum class UserState{
    FIRST_RUN_APP
}

enum class SettingsDialog{
    GET_PERMISSION,
    SCAN_LOCAL_STORAGE,
    GO_TO_APP_SETTINGS,
    SCAN_SUCCESS,
    SCAN_FAILED,
    SCANNING,
    NONE
}

enum class ScanResult{
    SUCCESS,
    FAILED,
    SCANNING,
    NONE
}