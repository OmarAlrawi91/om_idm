package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.DownloadItem
import java.io.File

/**
 * MX Player Integration:
 * Launches media files directly in MX Player (Free or Pro edition) with an explicit Intent,
 * granting read URI permissions with ClipData to completely eliminate "Can't play this link" errors,
 * falling back gracefully to the Android system chooser if MX Player is not installed.
 */
object MxPlayerHelper {
    const val MX_PLAYER_FREE = "com.mxtech.videoplayer.ad"
    const val MX_PLAYER_PRO = "com.mxtech.videoplayer.pro"
    private const val TAG = "MxPlayerHelper"

    fun playWithMxPlayer(context: Context, item: DownloadItem) {
        val file = File(item.savePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
            return
        }

        val contentUri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "FileProvider failed: ${e.message}", e)
            Toast.makeText(context, "File access error: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = when {
            item.mimeType.isNotBlank() && item.mimeType != "application/octet-stream" && !item.mimeType.contains("html") -> item.mimeType
            item.fileName.endsWith(".mp3", ignoreCase = true) ||
                    item.fileName.endsWith(".m4a", ignoreCase = true) ||
                    item.fileName.endsWith(".wav", ignoreCase = true) ||
                    item.fileName.endsWith(".flac", ignoreCase = true) -> "audio/*"
            item.fileName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            item.fileName.endsWith(".avi", ignoreCase = true) -> "video/x-msvideo"
            item.fileName.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            item.fileName.endsWith(".webm", ignoreCase = true) -> "video/webm"
            else -> "video/mp4"
        }

        val pm = context.packageManager
        val isFreeInstalled = isPackageInstalled(pm, MX_PLAYER_FREE)
        val isProInstalled = isPackageInstalled(pm, MX_PLAYER_PRO)

        val targetPackage = when {
            isProInstalled -> MX_PLAYER_PRO
            isFreeInstalled -> MX_PLAYER_FREE
            else -> null
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("Media", contentUri)
        }

        // Grant URI permission explicitly to potential candidate packages
        try {
            val resolveList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveList) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error granting package URI permissions: ${e.message}")
        }

        if (targetPackage != null) {
            context.grantUriPermission(targetPackage, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setPackage(targetPackage)
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch targeted MX Player package: ${e.message}")
            }
        }

        Toast.makeText(context, "Opening in media player (MX Player not detected)", Toast.LENGTH_SHORT).show()
        val chooser = Intent.createChooser(intent, "Play Media with MX Player").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("Media", contentUri)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No media player found: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
