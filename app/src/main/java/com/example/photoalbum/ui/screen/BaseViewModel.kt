package com.example.photoalbum.ui.screen

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.photoalbum.MainActivity
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.enums.ScanResult
import com.example.photoalbum.enums.UserState
import com.example.photoalbum.data.model.Directory
import com.example.photoalbum.data.model.DirectoryMediaFileCrossRef
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.ImageSize
import com.example.photoalbum.enums.ScanMode
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.utils.decodeSampledBitmap
import com.example.photoalbum.utils.getMiddleFrame
import com.example.photoalbum.utils.getPaths
import com.example.photoalbum.utils.getThumbnailName
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File

abstract class BaseViewModel(
    val application: MediaApplication,
    val userAction: MutableStateFlow<UserAction>,
    var settings: Settings,
) : ViewModel() {

    var expand by mutableStateOf(true)

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

    open fun scanLocalStorage(scanMode: ScanMode) {
        viewModelScope.launch(context = Dispatchers.IO) {
            var bigImage = 0
            val semaphore = Semaphore(16)
            val path = (application.applicationContext.getExternalFilesDir(null) ?: application.applicationContext.filesDir)
                .absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)
            val imageSize = when (scanMode.mode) {
                ScanMode.MODE_1.mode -> ImageSize.M_10
                ScanMode.MODE_2.mode -> ImageSize.M_5
                ScanMode.MODE_3.mode -> ImageSize.M_1
                else -> ImageSize.M_5
            }
            val highThumbnail = settings.highPixelThumbnail
            val map: HashMap<String, Long> = HashMap()

            if (checkPermissions()) {
                userAction.value = UserAction.ScanAction(ScanResult.SCANNING)
                val startTime = System.currentTimeMillis()
                val lists = application.mediaStoreContainer.imageStoreRepository.getMediaList()
                application.mediaDatabase.runInTransaction {
                    launch(Dispatchers.IO) {
                        try {
                            //清空数据库防止二次插入
                            application.mediaDatabase.directoryDao.clearTable()
                            application.mediaDatabase.mediaFileDao.clearTable()
                            application.mediaDatabase.directoryMediaFileCrossRefDao.clearTable()
                            val scanJob = viewModelScope.launch(Dispatchers.IO) {
                                val crossRefList: MutableList<DirectoryMediaFileCrossRef> = mutableListOf()
                                val jobs = mutableListOf<Job>()
                                for (item in lists) {
                                    //分析目录并插入directory表
                                    var directoryId: Long?
                                    val paths = getPaths(item.relativePath)
                                    for ((index, directoryPath) in paths.withIndex()) {
                                        val inserted = map.contains(directoryPath)
                                        val parentId = if (index - 1 >= 0)
                                            map[paths[index - 1]] ?: -1
                                        else
                                            -1
                                        if (!inserted) {
                                            val id = application.mediaDatabase.directoryDao.insert(
                                                Directory(
                                                    path = directoryPath,
                                                    parentId = parentId
                                                )
                                            )
                                            map[directoryPath] = id
                                        }
                                    }

                                    directoryId = map[paths.last()]
                                    val itemId = application.mediaDatabase.mediaFileDao.insert(item)
                                    crossRefList.add(
                                        DirectoryMediaFileCrossRef(
                                            directoryId!!,
                                            itemId
                                        )
                                    )
                                    if (item.mimeType.contains("image") && item.size > imageSize.size) {
                                        val createJob = viewModelScope.launch(Dispatchers.IO) {
                                            semaphore.acquire()
                                            bigImage += 1
                                            val fileName = getThumbnailName(item.displayName, otherStr = itemId.toString())
                                            val testFile = File(path, fileName)
                                            if (!testFile.exists()) {
                                                decodeSampledBitmap(
                                                    filePath = item.data,
                                                    orientation = item.orientation.toFloat(),
                                                    reqHeight = if (highThumbnail) 400 else 300,
                                                    reqWidth = if (highThumbnail) 400 else 300
                                                )?.let {
                                                    saveBitmapToPrivateStorage(
                                                        it,
                                                        fileName,
                                                        path
                                                    )
                                                }
                                            }
                                            semaphore.release()
                                        }
                                        jobs.add(createJob)
                                    } else if ((item.mimeType.contains("video"))) {
                                        val createJob = viewModelScope.launch(Dispatchers.IO) {
                                            semaphore.acquire()
                                            val fileName = getThumbnailName(item.displayName, otherStr = itemId.toString())
                                            val testFile = File(path, fileName)
                                            if (!testFile.exists()) {
                                                getMiddleFrame(
                                                    context = application.baseContext ?: application.applicationContext,
                                                    videoUri = item.data.toUri(),
                                                    duration = item.duration,
                                                    reqHeight = if (highThumbnail) 400 else 300,
                                                    reqWidth = if (highThumbnail) 400 else 300
                                                )?.let {
                                                    saveBitmapToPrivateStorage(
                                                        it,
                                                        fileName,
                                                        path
                                                    )
                                                }
                                            }
                                            semaphore.release()
                                        }
                                        jobs.add(createJob)
                                    }
                                }
                                jobs.joinAll()
                                viewModelScope.launch(context = Dispatchers.IO) {
                                    application.mediaDatabase.directoryMediaFileCrossRefDao.insert(crossRefList)
                                }
                            }
                            scanJob.join()

                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            println("Test testSQL took $duration ms")
                            println("测试:大文件 $bigImage 个")
                            userAction.value = UserAction.ScanAction(ScanResult.SUCCESS)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            userAction.value = UserAction.ScanAction(ScanResult.FAILED)
                        }
                    }
                }

            }
        }
    }

    companion object {
        class MyViewModelFactory(
            private val application: MediaApplication,
            private val userAction: MutableStateFlow<UserAction>,
            private val settings: Settings,
            private val activity: MainActivity? = null,
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when {
                    modelClass.isAssignableFrom(MainScreenViewModel::class.java) -> {
                        MainScreenViewModel(application, userAction, settings, activity!!) as T
                    }

                    modelClass.isAssignableFrom(MediaListScreenViewModel::class.java) -> {
                        MediaListScreenViewModel(application, userAction, settings) as T
                    }

                    modelClass.isAssignableFrom(GroupingScreenViewModel::class.java) -> {
                        GroupingScreenViewModel(application, userAction, settings) as T
                    }

                    modelClass.isAssignableFrom(SettingsScreenViewModel::class.java) -> {
                        SettingsScreenViewModel(application, userAction, settings) as T
                    }

                    modelClass.isAssignableFrom(ViewMediaFileViewModel::class.java) -> {
                        ViewMediaFileViewModel(application, userAction, settings) as T
                    }

                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }

}