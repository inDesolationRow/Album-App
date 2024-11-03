package com.example.photoalbum.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enum.NavType
import kotlinx.coroutines.flow.MutableStateFlow

class MainScreenViewModel(private val application: MediaApplication): BaseViewModel(application) {

    var selectPage by mutableStateOf(NavType.MEDIA_LIST)

}