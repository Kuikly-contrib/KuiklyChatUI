package com.tencent.kuiklybase.chat.kuiklyai.platform.android

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmChatPlatformServices
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmClipboard
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechListener
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechRecognizer
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechResult
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechSession
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreContextScheduler
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/** Installs real Android capabilities for the shared KuiklyAI demo. */
public object AndroidLlmPlatformRegistry {
    private var picker: AndroidLlmAttachmentPicker? = null
    private var speech: AndroidLlmSpeechRecognizer? = null

    public fun install(activity: Activity): LlmChatPlatformServices {
        val attachmentPicker = AndroidLlmAttachmentPicker(activity)
        val speechRecognizer = AndroidLlmSpeechRecognizer(activity)
        picker = attachmentPicker
        speech = speechRecognizer
        return LlmChatPlatformServices(
            clipboard = AndroidLlmClipboard(activity),
            attachmentPicker = attachmentPicker,
            speechRecognizer = speechRecognizer
        )
    }

    public fun dispatchActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean =
        picker?.onActivityResult(requestCode, resultCode, data) == true

    public fun dispatchPermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        val pickerHandled = picker?.onPermissionResult(requestCode, grantResults) == true
        val speechHandled = speech?.onPermissionResult(requestCode, grantResults) == true
        return pickerHandled || speechHandled
    }

    public fun clear() {
        picker = null
        speech = null
    }
}

private class AndroidLlmClipboard(context: Context) : LlmClipboard {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override suspend fun copyText(label: String, text: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

private class AndroidLlmAttachmentPicker(
    private val activity: Activity
) : LlmAttachmentPicker {
    override val supportedSources: Set<LlmAttachmentSource> = setOf(
        LlmAttachmentSource.PHOTO_LIBRARY,
        LlmAttachmentSource.CAMERA,
        LlmAttachmentSource.FILE
    )

    private var pending: kotlinx.coroutines.CancellableContinuation<List<LlmAttachment>>? = null
    private var pendingSource: LlmAttachmentSource? = null
    private var pendingLimit: Int = 9
    private var pendingCameraUri: android.net.Uri? = null
    private var pendingCameraFile: java.io.File? = null

    override suspend fun pick(source: LlmAttachmentSource): List<LlmAttachment> = pick(source, 9)

    override suspend fun pick(source: LlmAttachmentSource, maxCount: Int): List<LlmAttachment> =
        suspendCancellableCoroutine { continuation ->
            if (pending != null) {
                continuation.resume(emptyList())
                return@suspendCancellableCoroutine
            }
            pending = continuation
            pendingSource = source
            pendingLimit = maxCount.coerceIn(1, 9)
            continuation.invokeOnCancellation {
                if (pending === continuation) {
                    discardCameraCapture(activity, pendingCameraUri, pendingCameraFile)
                    pendingCameraUri = null
                    pendingCameraFile = null
                    pending = null
                    pendingSource = null
                }
            }
            launchPicker(source)
        }

    private fun launchPicker(source: LlmAttachmentSource) {
        val intent = when (source) {
            LlmAttachmentSource.CAMERA -> {
                val capture = createCameraCaptureFile(activity)
                if (capture == null) {
                    finish(emptyList(), IllegalStateException("Camera output is unavailable"))
                    return
                }
                val (file, uri) = capture
                pendingCameraFile = file
                pendingCameraUri = uri
                cameraCaptureIntent(activity, uri)
            }
            LlmAttachmentSource.PHOTO_LIBRARY -> galleryPickerIntent(pendingLimit)
            LlmAttachmentSource.FILE -> documentPickerIntent()
        }
        runCatching { activity.startActivityForResult(intent, REQUEST_CODE) }
            .onFailure { error ->
                discardCameraCapture(activity, pendingCameraUri, pendingCameraFile)
                pendingCameraUri = null
                pendingCameraFile = null
                finish(emptyList(), error)
            }
    }

    fun onPermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != CAMERA_PERMISSION_REQUEST) return false
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingSource?.let(::launchPicker)
        } else {
            finish(emptyList(), SecurityException("Camera permission denied"))
        }
        return true
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE) return false
        val source = pendingSource
        val cameraUri = pendingCameraUri
        val cameraFile = pendingCameraFile
        pendingCameraUri = null
        pendingCameraFile = null
        if (source == LlmAttachmentSource.CAMERA) {
            if (resultCode != Activity.RESULT_OK || cameraUri == null) {
                discardCameraCapture(activity, cameraUri, cameraFile)
                finish(emptyList(), null)
            } else {
                resumeOnContextThread(runCatching {
                    val attachments = listOfNotNull(readAttachment(cameraUri, source, 0))
                    discardCameraCapture(activity, cameraUri, cameraFile)
                    attachments
                })
            }
            return true
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            finish(emptyList(), null)
            return true
        }
        val uris = buildList {
            data.clipData?.let { clip ->
                for (index in 0 until clip.itemCount) add(clip.getItemAt(index).uri)
            }
            data.data?.let { uri -> if (isEmpty()) add(uri) }
        }
        resumeOnContextThread(runCatching {
            uris.mapIndexedNotNull { index, uri -> readAttachment(uri, source, index) }
        })
        return true
    }

    private fun finish(value: List<LlmAttachment>, error: Throwable?) {
        val result = if (error == null) Result.success(value) else Result.failure(error)
        resumeOnContextThread(result)
    }

    /**
     * Activity results arrive on the Android main thread. Kuikly `setTimeout` and
     * observables must run on the context queue, so resume the picker continuation there.
     */
    private fun resumeOnContextThread(result: Result<List<LlmAttachment>>) {
        val continuation = pending ?: return
        pending = null
        pendingSource = null
        KuiklyRenderCoreContextScheduler.scheduleTask(0L, Runnable {
            if (continuation.isActive) {
                continuation.resumeWith(result)
            }
        })
    }

    private fun readAttachment(
        uri: android.net.Uri,
        source: LlmAttachmentSource?,
        index: Int
    ): LlmAttachment? {
        val bytes = runCatching {
            activity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        val mime = activity.contentResolver.getType(uri).orEmpty()
        val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
            .ifEmpty { if (source == LlmAttachmentSource.PHOTO_LIBRARY) "image-$index" else "file-$index" }
        val kind = when {
            mime.startsWith("image/") -> LlmAttachmentKind.IMAGE
            mime.startsWith("audio/") -> LlmAttachmentKind.AUDIO
            else -> LlmAttachmentKind.FILE
        }
        val attachmentId = "android-file-${System.nanoTime()}-$index"
        val thumbnail = if (kind == LlmAttachmentKind.IMAGE) createThumbnailJpeg(bytes) else null
        return LlmAttachment(
            id = attachmentId,
            kind = kind,
            name = name,
            mimeType = mime,
            bytesBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            thumbnailBase64 = thumbnail?.let { Base64.encodeToString(it, Base64.NO_WRAP) }.orEmpty(),
            localPreviewUri = if (kind == LlmAttachmentKind.IMAGE) {
                writePreviewFile(attachmentId, bytes)
            } else {
                ""
            },
            sizeBytes = bytes.size.toLong()
        )
    }

    private fun createThumbnailJpeg(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, THUMBNAIL_SIZE)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, output)
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun writePreviewFile(id: String, jpeg: ByteArray): String {
        return runCatching {
            val dir = File(activity.cacheDir, "chat-previews").apply { mkdirs() }
            val file = File(dir, "$id.jpg")
            file.writeBytes(jpeg)
            "file://${file.absolutePath}"
        }.getOrDefault("")
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= targetSize &&
            height / (sampleSize * 2) >= targetSize
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val THUMBNAIL_SIZE = 256
        private const val THUMBNAIL_QUALITY = 78

        private const val REQUEST_CODE = 4821
        private const val CAMERA_PERMISSION_REQUEST = 4823
    }
}

private class AndroidLlmSpeechRecognizer(
    private val activity: Activity
) : LlmSpeechRecognizer {
    private var pendingRequest: PendingSpeechRequest? = null

    override suspend fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(activity)

    override fun start(listener: LlmSpeechListener): LlmSpeechSession {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            val session = PendingSpeechSession()
            pendingRequest = PendingSpeechRequest(listener, session)
            activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST)
            return session
        }
        return startRecognition(listener)
    }

    fun onPermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != PERMISSION_REQUEST) return false
        val request = pendingRequest
        pendingRequest = null
        if (request != null) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                if (!request.session.isCanceled) {
                    request.session.delegate = startRecognition(request.listener)
                }
            } else {
                request.listener.onError("请先授予麦克风权限")
                request.session.cancel()
            }
        }
        return true
    }

    private fun startRecognition(listener: LlmSpeechListener): LlmSpeechSession {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        val session = AndroidSpeechSession(recognizer)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onPartialResults(results: Bundle?) {
                resultText(results)?.let { listener.onResult(LlmSpeechResult(it, false)) }
            }
            override fun onResults(results: Bundle?) {
                resultText(results)?.let { listener.onResult(LlmSpeechResult(it, true)) }
                session.destroy()
            }
            override fun onError(error: Int) {
                listener.onError("语音识别失败（$error）")
                session.destroy()
            }
        })
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
        return session
    }

    private fun resultText(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private class AndroidSpeechSession(
        private val recognizer: SpeechRecognizer
    ) : LlmSpeechSession {
        private var closed = false

        override fun stop() {
            if (closed) return
            recognizer.stopListening()
        }

        override fun cancel() {
            if (closed) return
            recognizer.cancel()
            destroy()
        }

        fun destroy() {
            if (closed) return
            closed = true
            recognizer.destroy()
        }
    }

    private data class PendingSpeechRequest(
        val listener: LlmSpeechListener,
        val session: PendingSpeechSession
    )

    private class PendingSpeechSession : LlmSpeechSession {
        var delegate: LlmSpeechSession? = null
        var isCanceled: Boolean = false

        override fun stop() {
            delegate?.stop()
        }

        override fun cancel() {
            isCanceled = true
            delegate?.cancel()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST = 4822
    }
}
