package com.example.photoalbum.enums

enum class AlbumType {
    MAIN_ALBUM, //显示在主页的相册
    MIXED_ALBUM, //即有相册又有照片
    ALBUM, //只有照片
}

enum class StorageType {
    LOCAL,
    CLOUD
}

enum class NavType {
    MEDIA_LIST,
    FAVORITE,
    SETTINGS
}

enum class UserState {
    FIRST_RUN_APP
}

enum class MediaListDialog {
    LOCAL_NET_IP_ERROR,
    LOCAL_NET_USER_ERROR,
    LOCAL_NET_SHARED_ERROR,
    LOCAL_NET_ADD_SUCCESS,
    LOCAL_NET_CONNECTING,
    NONE
}

enum class SettingsDialog {
    SCAN_LOCAL_STORAGE,
    GO_TO_APP_SETTINGS,
    SCAN_SUCCESS,
    SCAN_FAILED,
    SCANNING,
    NONE
}

enum class ScanResult {
    SUCCESS,
    FAILED,
    SCANNING,
    NONE
}

enum class ItemType {
    DIRECTORY,
    IMAGE,
    VIDEO,
    ERROR
}

enum class ThumbnailsPath(val path: String){
    LOCAL_STORAGE("/Thumbnail/LocalStorage"),
    LOCAL_NET_STORAGE("/Thumbnail/LocalNetStorage")
}

enum class ImageSize(val size: Int){
    FOUR_K(5242880),
    TWO_K(2621440),
    ONE_K(1310720)
}