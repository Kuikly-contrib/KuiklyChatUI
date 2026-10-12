package com.tencent.kuiklybase.chat.kuiklyai.platform.android

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

internal fun galleryPickerIntent(maxCount: Int): Intent {
    val count = maxCount.coerceIn(1, 9)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "image/*"
            if (count > 1) {
                putExtra(
                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                    minOf(count, MediaStore.getPickImagesMaxLimit()),
                )
            }
        }
    } else {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, count > 1)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
}

internal fun documentPickerIntent(): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addCategory(Intent.CATEGORY_OPENABLE)
    }

internal fun createCameraCaptureFile(activity: Activity): Pair<File, Uri>? {
    val dir = File(activity.cacheDir, "chat-camera").apply { mkdirs() }
    val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
    if (!file.createNewFile() && !file.exists()) return null
    val uri = runCatching {
        FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.kuiklychat.fileprovider",
            file,
        )
    }.getOrNull() ?: return null
    return file to uri
}

internal fun cameraCaptureIntent(activity: Activity, output: Uri): Intent {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, output)
        clipData = ClipData.newRawUri("camera", output)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    val targets = activity.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    targets.forEach { info ->
        activity.grantUriPermission(info.activityInfo.packageName, output, flags)
    }
    return intent
}

internal fun discardCameraCapture(activity: Activity, uri: Uri?, file: File?) {
    runCatching {
        if (uri != null) {
            activity.revokeUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    runCatching { file?.delete() }
}
