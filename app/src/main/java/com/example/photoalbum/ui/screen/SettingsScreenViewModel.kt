package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.ScanResult
import com.example.photoalbum.model.SettingsDialogEntity
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings,
) : BaseViewModel(application, userAction, settings) {

    var showDialog by mutableStateOf(SettingsDialogEntity())

    var scanResult by mutableStateOf(ScanResult.NONE)

    init {
        viewModelScope.launch {
            userAction.collect {
                if (it is UserAction.ScanAction) {
                    println("扫描状态 ${it.scanState}")
                    scanResult = it.scanState
                }
            }
        }
    }

}