package com.example.photoalbum.ui.screen

import com.example.photoalbum.MediaApplication
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class FavoriteScreenViewModel(
    private val application: MediaApplication,
    userAction: MutableStateFlow<UserAction>
) : BaseViewModel(application, userAction) {

    /*    override fun injectUserAction(userAction: MutableStateFlow<UserAction>) {
            super.userAction = userAction
        }*/

}