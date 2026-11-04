package com.kuikly.kuiklychat.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.io.File
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

/**
 * 全屏预览图。Kuikly Android 渲染层不派发 pinch，所以缩放和拖动在这里处理。
 */
class ChatZoomImageView(context: Context) : View(context), IKuiklyRenderViewExport {

    private var closeCallback: KuiklyRenderCallback? = null
    private var bitmap: Bitmap? = null
    private var userScale = 1f
    private var transX = 0f
    private var transY = 0f
    private var loadToken = 0
    private var gesturePinched = false
    private var moved = false
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                gesturePinched = true
                return bitmap != null
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val image = bitmap ?: return false
                val old = userScale
                val next = (old * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                val fit = fitScale(image)
                if (fit > 0f && old > 0f) {
                    val bx = (detector.focusX - width / 2f - transX) / (fit * old)
                    val by = (detector.focusY - height / 2f - transY) / (fit * old)
                    userScale = next
                    transX = detector.focusX - width / 2f - bx * fit * next
                    transY = detector.focusY - height / 2f - by * fit * next
                } else {
                    userScale = next
                }
                if (userScale <= 1.01f) {
                    resetTransform()
                } else {
                    clamp(image)
                }
                invalidate()
                return true
            }
        },
    )

    init {
        isClickable = true
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            "src" -> {
                load(propValue as String)
                true
            }
            "close" -> {
                closeCallback = propValue as KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val image = bitmap ?: return
        if (width == 0 || height == 0) return
        val drawScale = fitScale(image) * userScale
        canvas.save()
        canvas.translate(width / 2f + transX, height / 2f + transY)
        canvas.scale(drawScale, drawScale)
        canvas.translate(-image.width / 2f, -image.height / 2f)
        canvas.drawBitmap(image, 0f, 0f, paint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gesturePinched = false
                moved = false
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val remaining = if (event.actionIndex == 0) 1 else 0
                lastX = event.getX(remaining)
                lastY = event.getY(remaining)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1 && userScale > 1.01f) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (abs(dx) + abs(dy) > 0f) moved = true
                    transX += dx
                    transY += dy
                    bitmap?.let { clamp(it) }
                    invalidate()
                } else if (abs(event.x - downX) + abs(event.y - downY) > touchSlop) {
                    moved = true
                }
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (!moved && !gesturePinched && userScale <= 1.01f) {
                    closeCallback?.invoke(null)
                }
            }
        }
        return true
    }

    private fun load(src: String) {
        val token = ++loadToken
        resetTransform()
        bitmap = null
        invalidate()
        thread(name = "chat-zoom-image") {
            val decoded = decode(src)
            post {
                if (token != loadToken) {
                    decoded?.recycle()
                    return@post
                }
                bitmap = decoded
                invalidate()
            }
        }
    }

    private fun decode(src: String): Bitmap? {
        val bytes = readBytes(src) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_BITMAP_SIDE)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun readBytes(src: String): ByteArray? {
        return runCatching {
            when {
                src.startsWith("data:image") -> {
                    val comma = src.indexOf(',')
                    if (comma < 0) null else Base64.decode(src.substring(comma + 1), Base64.DEFAULT)
                }
                src.startsWith("file:") -> {
                    val path = Uri.parse(src).path ?: return null
                    File(path).takeIf { it.isFile }?.readBytes()
                }
                src.startsWith("content:") -> {
                    context.contentResolver.openInputStream(Uri.parse(src))?.use { it.readBytes() }
                }
                src.startsWith("/") -> File(src).takeIf { it.isFile }?.readBytes()
                src.startsWith("http://") || src.startsWith("https://") -> {
                    URL(src).openStream().use { it.readBytes() }
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun fitScale(image: Bitmap): Float {
        if (width == 0 || height == 0) return 0f
        return min(width / image.width.toFloat(), height / image.height.toFloat())
    }

    private fun clamp(image: Bitmap) {
        val fit = fitScale(image)
        if (fit <= 0f) return
        val drawnW = image.width * fit * userScale
        val drawnH = image.height * fit * userScale
        val maxX = ((drawnW - width) / 2f).coerceAtLeast(0f)
        val maxY = ((drawnH - height) / 2f).coerceAtLeast(0f)
        transX = transX.coerceIn(-maxX, maxX)
        transY = transY.coerceIn(-maxY, maxY)
    }

    private fun resetTransform() {
        userScale = 1f
        transX = 0f
        transY = 0f
    }

    private fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= maxSide && height / (sample * 2) >= maxSide) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 4f
        const val MAX_BITMAP_SIDE = 2048
    }
}
