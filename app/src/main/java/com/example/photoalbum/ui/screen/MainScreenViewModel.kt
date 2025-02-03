package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.example.photoalbum.MainActivity
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.NavType
import com.example.photoalbum.ui.action.UserAction
import kotlinx.coroutines.flow.MutableStateFlow

class MainScreenViewModel(
    application: MediaApplication,
    userAction: MutableStateFlow<UserAction>,
    settings: Settings,
    activity: MainActivity
) : BaseViewModel(application, userAction, settings) {

    var selectPage by mutableStateOf(NavType.MEDIA_LIST)

    var mediaListScreenViewModel: MediaListScreenViewModel = ViewModelProvider.create(
        owner = activity, factory = Companion.MyViewModelFactory(
            application,
            userAction,
            settings
        )
    )[MediaListScreenViewModel::class.java]

    var favoriteScreenViewModel: GroupingScreenViewModel = ViewModelProvider.create(
        owner = activity, Companion.MyViewModelFactory(
            application,
            userAction,
            settings
        )
    )[GroupingScreenViewModel::class.java]
}