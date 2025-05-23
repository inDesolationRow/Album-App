package com.example.photoalbum.data

import android.content.Context
import androidx.core.net.toUri
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.photoalbum.MediaApplication
import com.example.photoalbum.data.model.Directory
import com.example.photoalbum.data.model.DirectoryMediaFileCrossRef
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.enums.WorkTag
import com.example.photoalbum.utils.decodeSampledBitmap
import com.example.photoalbum.utils.getMiddleFrame
import com.example.photoalbum.utils.getPaths
import com.example.photoalbum.utils.getThumbnailName
import com.example.photoalbum.utils.saveBitmapToPrivateStorage
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.io.File

class SyncDatabaseWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val workManager = WorkManager.getInstance(applicationContext)

    private val myapplication = applicationContext as MediaApplication

    private val coroutine = CoroutineScope(Dispatchers.IO)

    private val semaphore = Semaphore(16)

    private val mutex = Mutex()

    private var syncJob: Job? = null

    private val path = (myapplication.applicationContext.getExternalFilesDir(
        null
    ) ?: myapplication.applicationContext.filesDir).absolutePath.plus(
        ThumbnailsPath.LOCAL_STORAGE.path
    )

    override fun doWork(): Result {
        return runBlocking {
            try {
                println("work log:doWork. workId:$this.id")
                val startTime = System.currentTimeMillis()
                syncJob = coroutine.launch(Dispatchers.IO) {
                    val added = myapplication.mediaDatabase.mediaFileDao.getMaxGenerationAdded()

                    added?.let {
                        val result = myapplication.mediaStoreContainer.imageStoreRepository.updateMediaList(added)
                        println("同步${result.size}个文件")
                        if (result.isNotEmpty()) {
                            val crossRefList: MutableList<DirectoryMediaFileCrossRef> = mutableListOf()
                            val jobs = mutableListOf<Job>()
                            result.forEach { item ->
                                //分析目录并插入directory表
                                val paths = getPaths(item.relativePath)
                                var directoryId: Long? = myapplication.mediaDatabase.directoryDao.queryByPath(paths.last())?.directoryId
                                if (directoryId == null) {
                                    for ((index, directoryPath) in paths.withIndex()) {
                                        val inserted = myapplication.mediaDatabase.directoryDao.queryByPath(directoryPath)
                                        if (inserted == null) {
                                            val parentId =
                                                if (index - 1 >= 0)
                                                    myapplication.mediaDatabase.directoryDao.queryByPath(paths[index - 1])?.directoryId ?: -1
                                                else
                                                    -1
                                            directoryId = mutex.withLock {
                                                myapplication.mediaDatabase.directoryDao.insert(
                                                    Directory(
                                                        path = directoryPath,
                                                        parentId = parentId
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                val itemId = myapplication.mediaDatabase.mediaFileDao.insert(item)
                                crossRefList.add(DirectoryMediaFileCrossRef(directoryId!!, itemId))

                                if (item.mimeType.contains("image")) {
                                    val job = coroutine.launch(Dispatchers.IO) {
                                        semaphore.acquire()
                                        val fileName = getThumbnailName(item.displayName, itemId.toString())
                                        val testFile = File(path, fileName)
                                        if (!testFile.exists()) {
                                            decodeSampledBitmap(
                                                filePath = item.data,
                                                orientation = item.orientation.toFloat(),
                                                reqWidth = if (myapplication.settings?.highPixelThumbnail == true) 400 else 300,
                                                reqHeight = if (myapplication.settings?.highPixelThumbnail == true) 400 else 300,
                                                width = item.width,
                                                height = item.height
                                            )?.let {
                                                saveBitmapToPrivateStorage(it, fileName, path)
                                            }
                                        }
                                        semaphore.release()
                                    }
                                    jobs.add(job)
                                } else if ((item.mimeType.contains("video"))) {
                                    val job = coroutine.launch(Dispatchers.IO) {
                                        semaphore.acquire()
                                        val fileName = getThumbnailName(item.displayName, otherStr = itemId.toString())
                                        val testFile = File(path, fileName)
                                        if (!testFile.exists()) {
                                            getMiddleFrame(
                                                context = applicationContext,
                                                videoUri = item.data.toUri(),
                                                duration = item.duration,
                                                reqWidth = if (myapplication.settings?.highPixelThumbnail == true) 400 else 300,
                                                reqHeight = if (myapplication.settings?.highPixelThumbnail == true) 400 else 300
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
                                    jobs.add(job)
                                }
                            }
                            myapplication.mediaDatabase.directoryMediaFileCrossRefDao.insert(crossRefList)
                            jobs.forEach {
                                it.join()
                            }
                        }
                    }

                    val mediaInfoSet = myapplication.mediaStoreContainer.imageStoreRepository.getMediaList().map { it.data }.toSet()
                    val delList = myapplication.mediaDatabase.mediaFileDao.query()?.filter { !mediaInfoSet.contains(it.data) }
                    delList?.map { it.mediaFileId }?.let {
                        val count = myapplication.mediaDatabase.mediaFileDao.deleteByIds(it)
                        myapplication.mediaDatabase.albumMediaFileCrossRefDao.deleteByMediaFileIds(it)
                        println("删除$count 行")
                    }
                }
                yield()
                syncJob?.join()
                val runningTime = System.currentTimeMillis() - startTime
                println("work log:运行时间 :$runningTime")
                if (runningTime < 1000 * 10) {
                    delay(1000 * 10 - runningTime)
                }
                if (!isStopped)
                    again()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("work log:异常流程再次运行")
                again()
                Result.failure()
            }
        }
    }

    override fun onStopped() {
        super.onStopped()
        coroutine.launch {
            println("work log:取消该work")
            syncJob?.cancel()
        }
    }

    private fun again() {
        println("work log:再次运行")
        val work = OneTimeWorkRequest.Builder(SyncDatabaseWork::class.java)
            .addTag(WorkTag.SYNC_DATABASE.value)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueue(work)
    }
}