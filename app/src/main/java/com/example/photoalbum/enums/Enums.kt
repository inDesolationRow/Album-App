package com.example.photoalbum.enums

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
    LOCAL_NET_OFFLINE,
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

enum class ItemType(val value: Int) {
    GROUPING(0),
    DIRECTORY(1),
    IMAGE(2),
    VIDEO(3),
    ERROR(4)
}

enum class ThumbnailsPath(val path: String) {
    LOCAL_STORAGE("/Thumbnail/LocalStorage"),
    LOCAL_NET_STORAGE("/Thumbnail/LocalNetStorage")
}

enum class ImageSize(val size: Int) {
    M_30(31457280),
    M_10(10485760),
    M_5(5242880),
    M_2(2621440),
    M_1(1310720)
}

enum class SystemFolder(val displayName: String) {
    DCIM("dcim"),
    CAMERA("camera"),
    PICTURES("picture"),
    SCREENSHOTS("screenshot"),
    DOCUMENT("document"),
    DOWNLOAD("download"),
}

enum class Direction(val value: Int) {
    LEFT(-1),
    RIGHT(1),
    NULL(-10)
}

enum class WorkTag(val value: String) {
    SYNC_DATABASE("sync_database"),
    CREATE_THUMBNAILS("create_thumbnails")
}