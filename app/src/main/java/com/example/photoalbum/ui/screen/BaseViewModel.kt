package com.example.photoalbum.ui.screen

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.utils.decodeSampledBitmapFromStream
import com.example.photoalbum.utils.getThumbnailName
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.io.File

abstract class BaseViewModel(
    val application: MediaApplication,
    val userAction: MutableStateFlow<UserAction>,
    var settings: Settings
) : ViewModel() {

    var scanResult by mutableStateOf(ScanResult.NONE)

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

    open fun scanLocalStorage() {
        viewModelScope.launch(context = Dispatchers.IO) {
            //限制最大协程32
            var bigImage = 0
            val testJobs = mutableListOf<Job>()
            val semaphore4k = Semaphore(16)
            val jobs = mutableListOf<Job>()
            val delimiter = "[\\\\/]".toRegex()
            val mutex = Mutex()
            val path = (application.applicationContext.getExternalFilesDir(
                null
            ) ?: application.applicationContext.filesDir).absolutePath.plus(
                ThumbnailsPath.LOCAL_STORAGE.path
            )
            if (checkPermissions()) {
                userAction.value = UserAction.ScanAction(false)
                val startTime = System.currentTimeMillis()
                val lists =
                    application.mediaStoreContainer.imageStoreRepository.getMediaList().chunked(600)
                application.mediaDatabase.runInTransaction {
                    launch(Dispatchers.IO) {
                        try {
                            //清空数据库防止二次插入
                            application.mediaDatabase.directoryDao.clearTable()
                            application.mediaDatabase.mediaFileDao.clearTable()
                            application.mediaDatabase.directoryMediaFileCrossRefDao.clearTable()
                            for (insertList in lists) {
                                val job = viewModelScope.launch(Dispatchers.IO) {
                                    val crossRefList: MutableList<DirectoryMediaFileCrossRef> =
                                        mutableListOf()
                                    for (item in insertList) {
                                        val directories = item.relativePath.split(regex = delimiter)
                                            .map { it.trim() }
                                        //分析目录并插入directory表
                                        var parentId: Long? = null
                                        var directoryId: Long? = null
                                        val trimDirectories = directories.filter { it.isNotBlank() }
                                        for (dir in trimDirectories) {
                                            mutex.withLock {
                                                val queryResult =
                                                    application.mediaDatabase.directoryDao.queryByDisplayName(
                                                        displayName = dir
                                                    )
                                                parentId = queryResult?.directoryId
                                                    ?: application.mediaDatabase.directoryDao.insert(
                                                        directory = Directory(
                                                            displayName = dir,
                                                            parentId = parentId ?: -1,
                                                        )
                                                    )
                                                directoryId = queryResult?.directoryId ?: parentId
                                            }
                                        }

                                        val itemId =
                                            application.mediaDatabase.mediaFileDao.insert(item)
                                        crossRefList.add(
                                            DirectoryMediaFileCrossRef(
                                                directoryId!!,
                                                itemId
                                            )
                                        )

                                        //文件信息插入media_file表 文件大于5242880字节(5m 4k无损压缩图标准大小)生成缩略图
                                        if (item.size > 5242880) {
                                            bigImage += 1
                                            val job = viewModelScope.launch(Dispatchers.IO) {
                                                semaphore4k.acquire()
                                                val fileName = getThumbnailName(item.displayName)
                                                val testFile = File(path, fileName)
                                                if (!testFile.exists()) {
                                                    decodeSampledBitmapFromStream(
                                                        filePath = item.data,
                                                        orientation = item.orientation.toFloat()
                                                    )?.let {
                                                        saveBitmapToPrivateStorage(
                                                            it,
                                                            fileName,
                                                            path
                                                        )
                                                    }
                                                }
                                                semaphore4k.release()
                                            }
                                            testJobs.add(job)
                                        }
                                    }
                                    viewModelScope.launch(context = Dispatchers.IO) {
                                        application.mediaDatabase.directoryMediaFileCrossRefDao.insert(
                                            crossRefList
                                        )
                                    }
                                }
                                jobs.add(job)
                            }
                            jobs.forEach {
                                it.join()
                            }
                            testJobs.forEach {
                                it.join()
                            }
                            val endTime = System.currentTimeMillis()
                            val duration = endTime - startTime
                            userAction.value = UserAction.ScanAction(true)
                            println("Test testSQL took $duration ms")
                            println("测试:大文件 $bigImage 个")
                            scanResult = ScanResult.SUCCESS
                        } catch (e: Exception) {
                            e.printStackTrace()
                            scanResult = ScanResult.FAILED
                        }
                    }
                }

            }
        }
    }

    /*
        suspend fun createThumbnail(
            path: String,
            mediaFileId: Long,
            fileName: String,
        ): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null

            return viewModelScope.async(context = Dispatchers.IO) {
                var image: Bitmap? = null
                try {
                    val thumbnailName = fileName.split(".").first().plus("_thumbnail.png")
                    val thumbnailPath =
                        (application.applicationContext.getExternalFilesDir(null)
                            ?: application.applicationContext.filesDir).absolutePath.plus(ThumbnailsPath.LOCAL_STORAGE.path)
                    val testFile = File(thumbnailPath, thumbnailName)

                    if (testFile.exists()) {
                        application.mediaDatabase.mediaFileDao.updateThumbnail(
                            mediaFileId,
                            testFile.absolutePath
                        )
                        return@async null
                    }

                    decodeSampledBitmapFromStream(path, )?.let {
                        image = it
                        saveBitmapToPrivateStorage(
                            application.applicationContext,
                            it,
                            thumbnailName
                        )?.let { file ->
                            application.mediaDatabase.mediaFileDao.updateThumbnail(
                                mediaFileId,
                                file.absolutePath
                            )
                        }
                    }
                    image
                } catch (e: Exception) {
                    println("打印报错信息${e.message}")
                    return@async null
                }
            }.await()
        }
    */

    companion object {
        class MyViewModelFactory(
            private val application: MediaApplication,
            private val userAction: MutableStateFlow<UserAction>,
            private val settings: Settings,
            private val activity: MainActivity? = null
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

                    modelClass.isAssignableFrom(FavoriteScreenViewModel::class.java) -> {
                        FavoriteScreenViewModel(application, userAction, settings) as T
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