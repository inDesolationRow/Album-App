package com.example.photoalbum

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        val viewModel = MainScreenViewModel(myapplication)
        val myActivity = this
        viewModel.expand = MutableStateFlow(true)
        CoroutineScope(context = Dispatchers.Main).launch {
            viewModel.expand.collect() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    if (it) {
                        controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    } else {
                        controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    }
                } else {
                    if (it) {
                        window.decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    } else {
                        window.decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    }
                }
            }
        }
        CoroutineScope(context = Dispatchers.IO).launch {
            val isFirstRun = viewModel.checkFirstRunApp()
            if (isFirstRun) {
                viewModel.checkAndRequestPermissions(myActivity)
                viewModel.setFirstRunState()
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
