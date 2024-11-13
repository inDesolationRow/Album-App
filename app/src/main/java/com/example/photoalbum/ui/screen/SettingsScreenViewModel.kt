package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.model.SettingsDialogEntity
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsScreenViewModel(
    val application: MediaApplication,
    userAction: MutableStateFlow<UserAction>
) : BaseViewModel(application, userAction) {

    var showDialog by mutableStateOf(SettingsDialogEntity())

}