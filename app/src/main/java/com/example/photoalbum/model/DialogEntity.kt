package com.example.photoalbum.model

import com.example.photoalbum.enums.SettingsDialog

data class DialogEntity(val settingsDialog: SettingsDialog = SettingsDialog.NONE, val isShow: Boolean = false)