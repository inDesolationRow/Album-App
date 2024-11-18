package com.example.photoalbum

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.screen.MainScreen
import com.example.photoalbum.ui.screen.MainScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myapplication = application as MediaApplication
        val viewModel = MainScreenViewModel(myapplication, MutableStateFlow(UserAction.NoneAction), Settings())
        val myActivity = this
        CoroutineScope(context = Dispatchers.IO).launch {
            viewModel.userAction.collect(){
                when(it){
                    is UserAction.ExpandStatusBarAction -> {
                        CoroutineScope(context = Dispatchers.Main).launch {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val controller = window.insetsController
                                if (it.expand) {
                                    controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                                } else {
                                    controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                                }
                            } else {
                                if (it.expand) {
                                    window.decorView.systemUiVisibility =
                                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                } else {
                                    window.decorView.systemUiVisibility =
                                        View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                }
                            }
                        }
                    }
                    is UserAction.ScanAction ->{}
                    UserAction.NoneAction -> {}
                }
            }
        }
        CoroutineScope(context = Dispatchers.IO).launch {
            val isFirstRun = viewModel.checkFirstRunApp()
            if (isFirstRun) {
                viewModel.checkAndRequestPermissions(myActivity)
                viewModel.setFirstRunState()
                viewModel.application.mediaDatabase.settingsDao.insertOrUpdate(viewModel.settings)
            }else{
                val settings = viewModel.application.mediaDatabase.settingsDao.getUserSettings()
                viewModel.settings.openLocalNetStorageThumbnail = settings?.openLocalNetStorageThumbnail ?: true
                viewModel.settings.gridColumnNumState.intValue = settings?.gridColumnNum ?: 3
            }
        }
        enableEdgeToEdge()
        setContent {
            PhotoAlbumTheme {
                MainScreen(viewModel)
            }
        }
    }
}
