package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enums.NavType
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class MainScreenViewModel(
    val application: MediaApplication,
    userAction: MutableStateFlow<UserAction>
) : BaseViewModel(application, userAction) {

    var selectPage by mutableStateOf(NavType.MEDIA_LIST)

    val mediaListScreenViewModel: MediaListScreenViewModel = MediaListScreenViewModel(
        application,
        userAction
    )

    val favoriteScreenViewModel: FavoriteScreenViewModel = FavoriteScreenViewModel(
        application,
        userAction
    )

    val settingsScreenViewModel: SettingsScreenViewModel = SettingsScreenViewModel(
        application,
        userAction
    )
}