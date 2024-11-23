package com.example.photoalbum.ui.screen

import androidx.compose.ui.input.pointer.PointerId
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class ViewImageViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var local: Boolean = true

    fun initData(directory: Any, imageId: Long, local: Boolean) {
        this.local = local
        if (local){

        }else{

        }
    }

}