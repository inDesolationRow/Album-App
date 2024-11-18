package com.example.photoalbum.smb

import androidx.test.services.storage.file.HostedFile.FileType
import com.example.photoalbum.enums.ItemType
import com.example.photoalbum.model.MediaItem
import com.example.photoalbum.ui.action.ConnectResult
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import java.io.InputStream

class SmbClient {

    private lateinit var smbClient: SMBClient

    private lateinit var connection: Connection

    private lateinit var session: Session

    private lateinit var diskShare: DiskShare

    private val pathStack: MutableList<String> = mutableListOf()

    fun connect(ip: String, user: String, pwd: String?, shared: String): ConnectResult {
        pathStack.clear()
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

    fun back(): String {
        if (pathStack.size == 1) return ""
        pathStack.removeLast()
        return pathStack.joinToString("/")
    }

    fun getList(path: String?): MutableList<MediaItem> {
        val testPtah = path?:""
        pathStack.add(testPtah)

        val directoryList: MutableList<MediaItem> = mutableListOf()
        val fileList: MutableList<MediaItem> = mutableListOf()

        val all = diskShare.openDirectory(
            testPtah,
            setOf(AccessMask.FILE_LIST_DIRECTORY, AccessMask.GENERIC_READ),
            null,
            setOf(SMB2ShareAccess.FILE_SHARE_READ),
            null,
            null
        )
        val allList = all.filterNot { it.fileName in listOf(".", "..") }
        for (temp in allList) {
            val isFile = diskShare.fileExists("$testPtah/${temp.fileName}")
            if (!isFile) {
                directoryList.add(
                    MediaItem(
                        id = temp.fileId,
                        displayName = temp.fileName,
                        data = "$path/${temp.fileName}",
                        type = ItemType.DIRECTORY,
                        mimeType = ""
                    )
                )
            } else {
                if (checkFileFormat(temp.fileName) == ItemType.IMAGE){
                    fileList.add(MediaItem(
                        id = temp.fileId,
                        displayName = temp.fileName,
                        data = "$path/${temp.fileName}",
                        type = ItemType.IMAGE,
                        mimeType = "image/*"
                    ))
                }
            }
        }
        directoryList.addAll(fileList)
        return directoryList
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