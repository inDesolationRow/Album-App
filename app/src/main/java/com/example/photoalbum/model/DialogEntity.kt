package com.example.photoalbum.model

import com.example.photoalbum.enums.MediaListDialog
import com.example.photoalbum.enums.SettingsDialog

data class SettingsDialogEntity(val settingsDialog: SettingsDialog = SettingsDialog.NONE, val isShow: Boolean = false)

data class MediaListDialogEntity(val settingsDialog: MediaListDialog = MediaListDialog.NONE, val isShow: Boolean = false)