package com.example.photoalbum.ui.screen

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enum.ScanResult
import com.example.photoalbum.enum.UserState
import com.example.photoalbum.database.model.Directory
import com.example.photoalbum.database.model.DirectoryMediaFileCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class BaseViewModel(private val application: MediaApplication) : ViewModel() {

    var scanResult by mutableStateOf(ScanResult.NONE)

    lateinit var expand : MutableStateFlow<Boolean>

    fun checkAndRequestPermissions(activity: Activity) {
        return application.appPermission.checkAndRequestPermissions(activity)
    }

    fun checkPermissions(): Boolean {
        return application.appPermission.checkPermissions()
    }

    suspend fun checkFirstRunApp(): Boolean {
        return application.dataStore.data.map {
            it[booleanPreferencesKey(UserState.FIRST_RUN_APP.name)] ?: true
        }.first()
    }

    suspend fun setFirstRunState() {
        application.dataStore.edit {
            it[booleanPreferencesKey(UserState.FIRST_RUN_APP.name)] = false
        }
    }

    open fun scanLocalStorage() {
        viewModelScope.launch(context = Dispatchers.IO) {
            val delimiter = "[\\\\/]".toRegex()
            if (checkPermissions()) {
                val startTime = System.currentTimeMillis()
                val lists =
                    application.mediaStoreContainer.imageStoreRepository.getMediaList().chunked(900)
                application.mediaDatabase.runInTransaction {
                    launch {
                        try {
                            //清空数据库防止二次插入
                            application.mediaDatabase.directoryDao.clearTable()
                            application.mediaDatabase.mediaFileDao.clearTable()
                            application.mediaDatabase.directoryMediaFileCrossRefDao.clearTable()
                            val directoryList: MutableList<String> = mutableListOf()

                            for (insertList in lists) {
                                val crossRefList: MutableList<DirectoryMediaFileCrossRef> =
                                    mutableListOf()
                                for (item in insertList) {
                                    val directories =
                                        item.relativePath.split(regex = delimiter)
                                            .map { it.trim() }
                                    var parentId: Long? = null
                                    var directoryId: Long? = null
                                    for (dir in directories) {
                                        if (dir.isNotEmpty() && dir.isNotBlank()){
                                            val queryResult =
                                                application.mediaDatabase.directoryDao.queryByDisplayName(
                                                    displayName = dir
                                                )
                                            if (queryResult == null) {
                                                parentId =
                                                    application.mediaDatabase.directoryDao.insert(
                                                        directory = Directory(
                                                            displayName = dir,
                                                            parentId = parentId ?: -1,
                                                        )
                                                    )
                                                if (!directoryList.contains(dir))
                                                    directoryList.add(dir)
                                            }else{
                                                parentId = queryResult.directoryId
                                            }
                                            directoryId = queryResult?.directoryId ?: parentId!!
                                        }
                                    }
                                    val itemId = application.mediaDatabase.mediaFileDao.insert(item)
                                    crossRefList.add(
                                        DirectoryMediaFileCrossRef(
                                            directoryId!!,
                                            itemId
                                        )
                                    )
                                }
                                application.mediaDatabase.directoryMediaFileCrossRefDao.insert(
                                    crossRefList
                                )
                                val queryResult =
                                    application.mediaDatabase.directoryDao.queryDirectoryWithMediaFile()
                                var fileSize = 0
                                for (directory in queryResult!!) {
                                    fileSize += directory.mediaFileList.size
                                }
                                println("目录数:${directoryList.size} 关系表中目录数:${queryResult.size} \n 媒体文件数目:${insertList.size} 关系表中媒体文件数目:$fileSize")
                            }
                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            println("Test testSQL took $duration ms")
                            scanResult = ScanResult.SUCCESS
                        } catch (e: Exception) {
                            scanResult = ScanResult.FAILED
                        }
                    }
                }
            }
        }
    }

    companion object {
        inline fun <reified T : ViewModel> Factory(
            modelClass: Class<T>,
            crossinline creator: (application: MediaApplication) -> BaseViewModel
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val mediaApplication = this[APPLICATION_KEY] as MediaApplication
                val model = when {
                    modelClass.isAssignableFrom(MediaListScreenViewModel::class.java) -> {
                        creator(mediaApplication) as T
                    }

                    modelClass.isAssignableFrom(FavoriteScreenViewModel::class.java) -> {
                        creator(mediaApplication) as T
                    }

                    modelClass.isAssignableFrom(SettingsScreenViewModel::class.java) -> {
                        creator(mediaApplication) as T
                    }

                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
                model
            }
        }
    }

}