package com.example.photoalbum.model

import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.enums.ScanMode
import com.example.photoalbum.enums.SettingsDialog

data class SettingsDialogEntity(
    val settingsDialog: SettingsDialog = SettingsDialog.NONE,
    val isShow: Boolean = false,
    val scanMode: ScanMode = ScanMode.MODE_2
)

data class MediaListDialogEntity(
    val mediaListDialog: MediaListDialog = MediaListDialog.NONE,
    val isShow: Boolean = false,
    val onClick: () -> Unit = {}
)