package com.example.photoalbum.smb

import com.example.photoalbum.ui.action.ConnectResult
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

    fun connect(ip: String, user: String, pwd: String?, shared: String): ConnectResult {
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

    fun getCurrentList() {
        for (f in diskShare.list("")) {
//            if (diskShare.open())
//            println("File : " + f.fileName)
            println("File : " + f.fileId)
        }
    }

}