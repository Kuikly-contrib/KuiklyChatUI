package com.tencent.kuiklybase.chat.android

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.tencent.kuikly.core.render.android.KuiklyRenderViewContext
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import java.io.IOException
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Default Kuikly image adapter for Compose Chat (HTTP/assets/file + SVG).
 *
 * Register once at app startup:
 * ```
 * KuiklyChatAndroid.registerDefaultImageAdapter(applicationContext)
 * ```
 */
class KRChatImageAdapter(
    private val context: Context,
) : IKRImageAdapter {

    init {
        Glide.get(context).registry.prepend(
            InputStream::class.java,
            SVG::class.java,
            SvgDecoder(),
        )
        Log.d(TAG, "KRChatImageAdapter initialized")
    }

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        Log.d(
            TAG,
            "fetchDrawable src=${imageLoadOption.src} assets=${imageLoadOption.isAssets()} " +
                "file=${imageLoadOption.isFile()} web=${imageLoadOption.isWebUrl()} " +
                "base64=${imageLoadOption.isBase64()}",
        )
        if (imageLoadOption.isBase64()) {
            loadFromBase64(imageLoadOption, callback)
        } else if (imageLoadOption.isWebUrl() || imageLoadOption.isAssets() || imageLoadOption.isFile()) {
            requestImage(imageLoadOption, callback)
        } else {
            Log.w(TAG, "fetchDrawable unhandled src=${imageLoadOption.src}")
            callback.invoke(null)
        }
    }

    override fun getDrawableWidth(
        kuiklyRenderViewContext: KuiklyRenderViewContext,
        drawable: Drawable,
    ): Float = drawable.intrinsicWidth.toFloat()

    override fun getDrawableHeight(
        kuiklyRenderViewContext: KuiklyRenderViewContext,
        drawable: Drawable,
    ): Float = drawable.intrinsicHeight.toFloat()

    private fun requestImage(
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        val src = if (imageLoadOption.isAssets()) {
            val assetPath = imageLoadOption.src.substring(HRImageLoadOption.SCHEME_ASSETS.length)
            "file:///android_asset/$assetPath"
        } else {
            imageLoadOption.src
        }
        if (src.substringBefore('?').endsWith(".svg", ignoreCase = true)) {
            Log.d(TAG, "requestSvgImage src=$src")
            requestSvgImage(src, imageLoadOption, callback)
            return
        }
        Log.d(TAG, "requestImage raster src=$src")
        val requestBuilder = if (src.endsWith(".gif")) {
            Glide.with(context).asGif().load(src) as RequestBuilder<Drawable>
        } else {
            Glide.with(context).asDrawable().load(src)
        }

        if (imageLoadOption.needResize) {
            requestBuilder.override(imageLoadOption.requestWidth, imageLoadOption.requestHeight)
            when (imageLoadOption.scaleType) {
                ImageView.ScaleType.CENTER_CROP -> requestBuilder.centerCrop()
                ImageView.ScaleType.FIT_CENTER -> requestBuilder.fitCenter()
                else -> Unit
            }
        }
        requestBuilder.into(object : CustomTarget<Drawable>() {
            override fun onLoadCleared(placeholder: Drawable?) {
                callback.invoke(null)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                Log.w(TAG, "requestImage failed src=$src")
                callback.invoke(null)
            }

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?,
            ) {
                Log.d(
                    TAG,
                    "requestImage ok src=$src size=${resource.intrinsicWidth}x${resource.intrinsicHeight}",
                )
                callback.invoke(resource)
            }
        })
    }

    private fun requestSvgImage(
        src: String,
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        val requestBuilder = Glide.with(context).`as`(SVG::class.java).load(src)
        if (imageLoadOption.needResize) {
            requestBuilder.override(imageLoadOption.requestWidth, imageLoadOption.requestHeight)
        }
        requestBuilder.into(object : CustomTarget<SVG>() {
            override fun onLoadCleared(placeholder: Drawable?) {
                callback.invoke(null)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                Log.w(TAG, "requestSvgImage failed src=$src")
                callback.invoke(null)
            }

            override fun onResourceReady(
                resource: SVG,
                transition: Transition<in SVG>?,
            ) {
                val picture = resource.renderToPicture()
                val drawable = PictureDrawable(picture).apply {
                    setBounds(0, 0, picture.width, picture.height)
                }
                Log.d(
                    TAG,
                    "requestSvgImage ok src=$src size=${picture.width}x${picture.height}",
                )
                callback.invoke(drawable)
            }
        })
    }

    private fun loadFromBase64(
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        Thread {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            val bytes = Base64.decode(imageLoadOption.src.split(",")[1], Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            try {
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                options.inJustDecodeBounds = false
                try {
                    options.inSampleSize = calculateInSampleSize(
                        options,
                        imageLoadOption.requestWidth,
                        imageLoadOption.requestHeight,
                    )
                } catch (error: ArithmeticException) {
                    Log.w(TAG, "loadFromBase64 sample size failed: $error")
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                callback.invoke(BitmapDrawable(Resources.getSystem(), bitmap))
            } catch (error: OutOfMemoryError) {
                Log.w(TAG, "loadFromBase64 oom: $error")
                callback.invoke(null)
            }
        }.start()
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        if (reqWidth == 0 || reqHeight == 0 || reqWidth == -1 || reqHeight == -1) {
            return 1
        }
        var height = options.outHeight
        var width = options.outWidth
        var inSampleSize = 1
        while (height > reqHeight && width > reqWidth) {
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()
            val ratio = if (heightRatio > widthRatio) heightRatio else widthRatio
            if (ratio < 2) break
            width = width shr 1
            height = height shr 1
            inSampleSize = inSampleSize shl 1
        }
        return inSampleSize
    }

    private class SvgDecoder : ResourceDecoder<InputStream, SVG> {
        override fun handles(source: InputStream, options: Options): Boolean = true

        override fun decode(
            source: InputStream,
            width: Int,
            height: Int,
            options: Options,
        ): Resource<SVG>? {
            return try {
                val normalized = normalizeSvgColor(source.readBytes().decodeToString())
                SimpleResource(SVG.getFromString(normalized))
            } catch (exception: SVGParseException) {
                throw IOException("Cannot load SVG from stream", exception)
            }
        }

        private fun normalizeSvgColor(svg: String): String =
            svg.replace(Regex("currentColor", RegexOption.IGNORE_CASE), "#1E293B")
    }

    private companion object {
        private const val TAG = "KRChatImageAdapter"
    }
}

/** Host registration helpers for KuiklyChatAndroid. */
object KuiklyChatAndroid {
    fun registerDefaultImageAdapter(context: Context) {
        KuiklyRenderAdapterManager.krImageAdapter = KRChatImageAdapter(context.applicationContext)
        Log.d("KRChatImageAdapter", "registerDefaultImageAdapter")
    }
}
