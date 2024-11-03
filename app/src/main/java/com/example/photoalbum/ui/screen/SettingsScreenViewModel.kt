package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.model.DialogEntity
class SettingsScreenViewModel(val application: MediaApplication) : BaseViewModel(application) {

    var showDialog by mutableStateOf<DialogEntity>(DialogEntity())

}