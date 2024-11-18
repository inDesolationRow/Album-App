package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.NavType
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class MainScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings
) : BaseViewModel(application, userAction, settings) {

    var selectPage by mutableStateOf(NavType.MEDIA_LIST)

    val mediaListScreenViewModel: MediaListScreenViewModel = MediaListScreenViewModel(
        application,
        userAction,
        settings
    )

    val favoriteScreenViewModel: FavoriteScreenViewModel = FavoriteScreenViewModel(
        application,
        userAction,
        settings
    )

    val settingsScreenViewModel: SettingsScreenViewModel = SettingsScreenViewModel(
        application,
        userAction,
        settings
    )
}