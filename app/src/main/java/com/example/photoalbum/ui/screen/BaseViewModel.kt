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
import com.example.photoalbum.utils.decodeSampledBitmapFromStream
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File

abstract class BaseViewModel(private val application: MediaApplication) : ViewModel() {

    var scanResult by mutableStateOf(ScanResult.NONE)

    lateinit var expand: MutableStateFlow<Boolean>

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
            val semaphore = Semaphore(32)
            //限制事务中大规模的插入操作，解决并发问题
            val transactionSemaphore = Semaphore(1)
            val jobs = mutableListOf<Job>()
            val delimiter = "[\\\\/]".toRegex()
            if (checkPermissions()) {
                val startTime = System.currentTimeMillis()
                val lists =
                    application.mediaStoreContainer.imageStoreRepository.getMediaList().chunked(600)
                application.mediaDatabase.runInTransaction {
                    launch {
                        try {
                            //清空数据库防止二次插入
                            application.mediaDatabase.directoryDao.clearTable()
                            application.mediaDatabase.mediaFileDao.clearTable()
                            application.mediaDatabase.directoryMediaFileCrossRefDao.clearTable()
                            val directoryList: MutableList<String> = mutableListOf()
                            for (insertList in lists) {
                                val job = viewModelScope.launch(Dispatchers.IO) {
                                    val crossRefList: MutableList<DirectoryMediaFileCrossRef> =
                                        mutableListOf()
                                    for (item in insertList) {
                                        val directories =
                                            item.relativePath.split(regex = delimiter)
                                                .map { it.trim() }
                                        var parentId: Long? = null
                                        var directoryId: Long? = null

                                        //分析目录并插入directory表
                                        for (dir in directories) {
                                            if (dir.isNotEmpty() && dir.isNotBlank()) {
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
                                                } else {
                                                    parentId = queryResult.directoryId
                                                }
                                                directoryId = queryResult?.directoryId ?: parentId
                                            }
                                        }

                                        //文件信息插入media_file表 文件大于5242880字节(5m 4k无损压缩图标准大小)生成缩略图
                                        if (item.size > 5242880) {
                                            viewModelScope.launch(Dispatchers.IO) {
                                                semaphore.acquire()
                                                val fileName = item.displayName.split(".").first()
                                                    .plus("_thumbnail.png")
                                                val path =
                                                    (application.applicationContext.getExternalFilesDir(null)
                                                        ?: application.applicationContext.filesDir).absolutePath.plus("/Thumbnail")
                                                val testFile = File(path, fileName)
                                                if(!testFile.exists()){
                                                    decodeSampledBitmapFromStream(item.data)?.let {
                                                        item.thumbnail = saveBitmapToPrivateStorage(
                                                            application.applicationContext,
                                                            it,
                                                            fileName
                                                        )?.absolutePath ?: testFile.absolutePath
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
                                                semaphore.release()
                                            }
                                        } else {
                                            val itemId =
                                                application.mediaDatabase.mediaFileDao.insert(item)
                                            crossRefList.add(
                                                DirectoryMediaFileCrossRef(
                                                    directoryId!!,
                                                    itemId
                                                )
                                            )
                                        }
                                    }
                                    transactionSemaphore.acquire()
                                    application.mediaDatabase.directoryMediaFileCrossRefDao.insert(
                                        crossRefList
                                    )
                                    transactionSemaphore.release()
                                    /*
                                                                        val queryResult =
                                                                            application.mediaDatabase.directoryDao.queryDirectoryWithMediaFile()
                                                                        var fileSize = 0
                                                                        for (directory in queryResult!!) {
                                                                            fileSize += directory.mediaFileList.size
                                                                        }
                                                                        println("目录数:${directoryList.size} 关系表中目录数:${queryResult.size} \n 媒体文件数目:${insertList.size} 关系表中媒体文件数目:$fileSize")
                                    */
                                }
                                jobs.add(job)
                            }
                            jobs.forEach() {
                                it.join()
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