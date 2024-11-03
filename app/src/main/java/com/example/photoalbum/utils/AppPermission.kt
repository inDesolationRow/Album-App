package com.example.photoalbum.utils
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val context: Context) {

    /**
     * 检查并弹出授权
     *
     * @param activity
     * @param onlyCheck 检查是否能弹出授权框但不申请授权
     * @return return true 代表弹出授权框 return false 代表无法弹出请手动转跳设置
     */
    fun checkAndRequestPermissions(activity: Activity, onlyCheck: Boolean = false): Boolean {
        val permissionsToRequest = mutableListOf<String>()

        when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            Build.VERSION.SDK_INT in Build.VERSION_CODES.R..Build.VERSION_CODES.S -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            for(permission in permissionsToRequest){
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // 权限被永久拒绝，显示提示，并引导用户前往系统设置
                    return false
                }
            }
            if (!onlyCheck){
                ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), REQUEST_CODE)
            }
        }
        return true
    }

    // 需要权限时仅检查权限
    fun checkPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT in Build.VERSION_CODES.R..Build.VERSION_CODES.S -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            }
            else -> true
        }
    }

    fun goToSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}