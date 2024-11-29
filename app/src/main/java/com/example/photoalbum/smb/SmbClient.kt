package com.example.photoalbum.smb

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.enums.ThumbnailsPath
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.ConnectResult
import com.example.photoalbum.utils.decodeSampledBitmapFromStream
import com.example.photoalbum.utils.getThumbnailName
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare

class SmbClient {

    private lateinit var smbClient: SMBClient

    private lateinit var connection: Connection

    private lateinit var session: Session

    private lateinit var diskShare: DiskShare

    private val pathStack: SnapshotStateList<String> = mutableStateListOf()

    private var popPath: String? = null

    fun connect(
        ip: String,
        user: String,
        pwd: String?,
        shared: String,
        reconnection: Boolean = false
    ): ConnectResult {
        if (!reconnection) {
            pathStack.clear()
            popPath = null
        }
        smbClient = SMBClient()
        val connectionTest = connect(ip)
        if (connectionTest is Exception) return ConnectResult.IPError(connectionTest.message!!)
        connection = connectionTest as Connection

        val sessionTest = authenticate(connection, user, pwd)
        if (sessionTest is Exception) return ConnectResult.AuthenticateError(sessionTest.message!!)
        session = sessionTest as Session

        val diskShareTest = connectShare(session, shared)
        if (diskShareTest is Exception) return ConnectResult.SharedError(diskShareTest.message!!)
        diskShare = diskShareTest as DiskShare

        return ConnectResult.Success
    }

    private fun connect(ip: String): Any {
        val connection: Connection?
        try {
            connection = smbClient.connect(ip)
        } catch (e: Exception) {
            return e
        }
        return connection
    }

    private fun authenticate(connection: Connection, user: String, pwd: String?): Any {
        val session: Session?
        try {
            session = connection.authenticate(
                AuthenticationContext(
                    user,
                    pwd?.toCharArray() ?: CharArray(0),
                    null
                )
            )
        } catch (e: Exception) {
            return e
        }
        return session
    }

    private fun connectShare(session: Session, shared: String): Any {
        val diskShare: DiskShare
        try {
            diskShare = session.connectShare(shared) as DiskShare
        } catch (e: Exception) {
            return e
        }
        return diskShare
    }

    fun pathStackSize(): Int {
        return pathStack.size
    }

    fun back(): String {
        if (pathStack.size == 1) return ""
        popPath = pathStack.removeLast()
        return pathStack.last()
    }

    fun getPath(): String {
        return pathStack.joinToString(separator = "") {
            if (it.isNotEmpty()) {
                "$it/"
            } else {
                ""
            }
        }
    }

    private fun getFilePath(name: String): String{
        return getPath().plus(name)
    }

    fun rollback() {
        popPath?.let {
            pathStack.add(it)
            popPath = null
        }
    }

    fun getList(path: String): MutableList<MediaItem> {
        val directoryList: MutableList<MediaItem> = mutableListOf()
        val fileList: MutableList<MediaItem> = mutableListOf()

        //popPath是null代表前进路径入栈,非空代表后退
        if (popPath == null) {
            pathStack.add(path)
        } else {
            popPath = null
        }

        val testPath = getPath()
        if (!diskShare.folderExists(testPath)) {
            println("错误:目录不存在$testPath")
            return directoryList
        }

        val all = diskShare.openDirectory(
            testPath,
            setOf(AccessMask.FILE_LIST_DIRECTORY, AccessMask.GENERIC_READ),
            null,
            setOf(SMB2ShareAccess.FILE_SHARE_READ),
            null,
            null
        )
        val allList = all.filterNot { it.fileName in listOf(".", "..") }
        for (temp in allList) {
            val isFile = diskShare.fileExists("$testPath/${temp.fileName}")
            if (!isFile) {
                directoryList.add(
                    MediaItem(
                        id = temp.fileId,
                        displayName = temp.fileName,
                        data = "$path/${temp.fileName}",
                        type = ItemType.DIRECTORY,
                        mimeType = "",
                        local = false
                    )
                )
            } else {
                if (checkFileFormat(temp.fileName) == ItemType.IMAGE) {
                    fileList.add(
                        MediaItem(
                            id = temp.fileId,
                            displayName = temp.fileName,
                            data = "$path/${temp.fileName}",
                            thumbnailPath = "${ThumbnailsPath.LOCAL_NET_STORAGE.path}/${
                                getThumbnailName(temp.fileName)
                            }",
                            type = ItemType.IMAGE,
                            mimeType = "image/*",
                            fileSize = temp.allocationSize,
                            local = false
                        )
                    )
                }
            }
        }
        directoryList.addAll(fileList)
        return directoryList
    }

    fun getImageThumbnail(name: String): Bitmap? {
        var thumbnail: Bitmap? = null
        val path = getFilePath(name)
        try {
            if (diskShare.fileExists(path)){
                val file = diskShare.openFile(
                    path,
                    setOf(AccessMask.FILE_READ_DATA),
                    null,
                    setOf(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    setOf(SMB2CreateOptions.FILE_SEQUENTIAL_ONLY)
                )
                var byteArray : ByteArray?
                file.use {
                    it.inputStream.use { inputStream ->
                        byteArray = inputStream.readBytes()
                    }
                }
                System.gc()
                byteArray?.let {
                    thumbnail = decodeSampledBitmapFromStream(it)
                }
            }
        }catch (e: Exception){
            println(e)
        }
        System.gc()
        return thumbnail
    }

    private fun checkFileFormat(name: String): ItemType {
        return when {
            name.endsWith(".png") -> ItemType.IMAGE
            name.endsWith(".jpg") -> ItemType.IMAGE
            name.endsWith(".jpeg") -> ItemType.IMAGE
            name.endsWith(".bmp") -> ItemType.IMAGE
            name.endsWith(".gif") -> ItemType.IMAGE
            name.endsWith(".webp") -> ItemType.IMAGE
            name.endsWith(".tiff") -> ItemType.IMAGE
            name.endsWith(".tif") -> ItemType.IMAGE
            else -> ItemType.ERROR
        }
    }

    fun isConnect(): Boolean {
        return connection.isConnected
    }
    //endOfFile: 13679182731 实际大小（byte）
    //allocationSize: 13679190016 占用硬盘空间（byte）
    /*val testFolder = diskShare.openDirectory(
                   "动漫",
                   setOf(AccessMask.FILE_LIST_DIRECTORY),
                   null,
                   setOf(SMB2ShareAccess.FILE_SHARE_READ),
                   SMB2CreateDisposition.FILE_OPEN,
                   setOf(SMB2CreateOptions.FILE_SEQUENTIAL_ONLY)
               )*/
}