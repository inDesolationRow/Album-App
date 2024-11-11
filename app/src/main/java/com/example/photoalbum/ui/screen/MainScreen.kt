package com.example.photoalbum.ui.screen

import android.os.Bundle
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photoalbum.enums.NavType
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.res.stringResource
import com.example.photoalbum.R
import com.example.photoalbum.ui.action.UserAction

@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    println("测试:重组mainScreen")
    var getNavHostHeight by rememberSaveable { mutableStateOf(false) }
    var hostHeight by rememberSaveable(saver = dpSaver) { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val navHost = rememberNavController()
    val userAction = viewModel.userAction.collectAsState()
    when(val action = userAction.value){
        is UserAction.ExpandStatusBarAction ->{viewModel.expand = action.expand}
        is UserAction.ScanAction -> {
        }
        is UserAction.NoneAction -> {}
    }
    val bottomBarAnimateDp: State<Dp>? = if(getNavHostHeight){
        animateDpAsState(targetValue = if (viewModel.expand) hostHeight else 0.dp,
            animationSpec = tween(durationMillis = 600),
            label = "隐藏或显示bottomBar")
    }else{null}
    Scaffold(bottomBar = {
        NavigationBar(modifier = Modifier.then(
            if (getNavHostHeight){
                Modifier.height(bottomBarAnimateDp!!.value)
            }else{
                Modifier
            }
        ).onGloballyPositioned {layout->
            with(density) {
                if (!getNavHostHeight){
                    hostHeight = layout.size.height.toDp()
                    getNavHostHeight = true
                }
            }
        }) {
            NavigationBarItem(
                icon = { Icon(painter = rememberVectorPainter(Icons.Filled.Folder), contentDescription = "") },
                label = { Text(text = stringResource(R.string.bar_title_media_list)) },
                selected = viewModel.selectPage == NavType.MEDIA_LIST,
                onClick = {
                    viewModel.selectPage = NavType.MEDIA_LIST
                    navHost.navigate(route = NavType.MEDIA_LIST.name)
                }
            )
            NavigationBarItem(
                icon = { Icon(painter = rememberVectorPainter(Icons.Filled.AutoAwesome), contentDescription = "") },
                label = { Text(text = stringResource(R.string.bar_title_my_favorite)) },
                selected = viewModel.selectPage == NavType.FAVORITE,
                onClick = {
                    viewModel.selectPage = NavType.FAVORITE
                    navHost.navigate(route = NavType.FAVORITE.name)
                }
            )
            NavigationBarItem(
                icon = { Icon(painter = rememberVectorPainter(Icons.Filled.SettingsSuggest), contentDescription = "") },
                label = { Text(text = stringResource(R.string.bar_title_settings))},
                selected = viewModel.selectPage == NavType.SETTINGS,
                onClick = {
                    viewModel.selectPage = NavType.SETTINGS
                    navHost.navigate(route = NavType.SETTINGS.name)
                }
            )
        }
    },
        modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(navController = navHost, startDestination = NavType.MEDIA_LIST.name) {
            composable(route = NavType.MEDIA_LIST.name){ MediaListScreen(viewModel = viewModel.mediaListScreenViewModel, modifier = Modifier.padding(innerPadding)) }
            composable(route = NavType.FAVORITE.name){ FavoriteScreen(viewModel = viewModel.favoriteScreenViewModel, modifier = Modifier.padding(innerPadding)) }
            composable(route = NavType.SETTINGS.name){ SettingsScreen(viewModel = viewModel.settingsScreenViewModel, modifier = Modifier.padding(innerPadding)) }
        }
    }
}

val dpSaver = Saver<MutableState<Dp>, Bundle>(
    save = { state ->
        Bundle().apply {
            putFloat("value", state.value.value) // 提取 Dp 的值
        }
    },
    restore = { bundle ->
        mutableStateOf(bundle.getFloat("value").dp) // 恢复为 MutableState<Dp>
    }
)