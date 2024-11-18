package com.example.photoalbum.ui.screen

import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class FavoriteScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    /*    override fun injectUserAction(userAction: MutableStateFlow<UserAction>) {
            super.userAction = userAction
        }*/

}