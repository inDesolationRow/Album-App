package com.example.photoalbum.smb

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare


class SmbClient {

    lateinit var smbClient: SMBClient

    fun connect(ip: String, user: String, pwd: String?): Boolean {
        try {
            smbClient = SMBClient()
            val connection = smbClient.connect(ip)
            println("测试:开始验证")
            val session = connection.authenticate(
                AuthenticationContext(
                    user,
                    pwd?.toCharArray() ?: CharArray(0),
                    null
                )
            )
            val shape = session.connectShare("shared") as DiskShare
            for (f in shape.list("")) {
                println("File : " + f.fileName)
            }
            println("测试:验证结束")
        } catch (e: Exception) {
            e.stackTrace.firstOrNull()?.let { element ->
                println("异常发生在文件: ${element.fileName}")
                println("异常发生在类: ${element.className}")
                println("异常发生在方法: ${element.methodName} ")
                println("异常发生的行: ${element.lineNumber}")
            }
        }
        return false
    }
}