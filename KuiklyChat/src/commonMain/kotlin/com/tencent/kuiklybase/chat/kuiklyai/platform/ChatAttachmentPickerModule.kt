package com.tencent.kuiklybase.chat.kuiklyai.platform

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Kuikly Module bridge for a host-provided native attachment picker.
 *
 * The matching native module is provided by `KuiklyChatIOS` and `KuiklyChatOhos`.
 */
open class ChatAttachmentPickerModule : Module() {
    override fun moduleName(): String = MODULE_NAME

    fun supportedSources(): Set<LlmAttachmentSource> {
        val value = syncToNativeMethod(
            METHOD_SUPPORTED_SOURCES,
            emptyArray<Any>(),
            null,
        ) as? String ?: return emptySet()
        return value
            .split(',')
            .mapNotNull { source ->
                runCatching { LlmAttachmentSource.valueOf(source) }.getOrNull()
            }
            .toSet()
    }

    suspend fun pick(source: LlmAttachmentSource): List<LlmAttachment> =
        suspendCancellableCoroutine { continuation ->
            val params = JSONObject().apply {
                put("source", source.name)
            }
            asyncToNativeMethod(METHOD_PICK, params) { result ->
                if (!continuation.isActive) {
                    return@asyncToNativeMethod
                }
                continuation.resumeWith(
                    runCatching {
                        val error = result?.optString("error").orEmpty()
                        check(error.isEmpty()) { error }
                        parseAttachments(result)
                    },
                )
            }
            continuation.invokeOnCancellation {
                asyncToNativeMethod(METHOD_CANCEL, null, null)
            }
        }

    private fun parseAttachments(result: JSONObject?): List<LlmAttachment> {
        val attachments = result?.optJSONArray("attachments") ?: return emptyList()
        return buildList {
            for (index in 0 until attachments.length()) {
                val item = attachments.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val kind = runCatching {
                    LlmAttachmentKind.valueOf(item.optString("kind"))
                }.getOrNull()
                if (id.isEmpty() || kind == null) {
                    continue
                }
                add(
                    LlmAttachment(
                        id = id,
                        kind = kind,
                        name = item.optString("name"),
                        mimeType = item.optString("mimeType"),
                        url = item.optString("url"),
                        bytesBase64 = item.optString("bytesBase64"),
                        thumbnailBase64 = item.optString("thumbnailBase64"),
                        sizeBytes = item.optLong("sizeBytes", 0L),
                    ),
                )
            }
        }
    }

    companion object {
        const val MODULE_NAME = "KRChatAttachmentPickerModule"
        private const val METHOD_SUPPORTED_SOURCES = "supportedSources"
        private const val METHOD_PICK = "pick"
        private const val METHOD_CANCEL = "cancel"
    }
}

private class ModuleLlmAttachmentPicker(
    private val module: ChatAttachmentPickerModule,
    override val supportedSources: Set<LlmAttachmentSource>,
) : LlmAttachmentPicker {
    override suspend fun pick(source: LlmAttachmentSource): List<LlmAttachment> {
        require(source in supportedSources) { "Unsupported attachment source: $source" }
        return module.pick(source)
    }
}

/**
 * Returns the native attachment picker when the current host supplies the matching module.
 */
fun createChatAttachmentPicker(pager: IPager): LlmAttachmentPicker? {
    val module = pager.getModule<ChatAttachmentPickerModule>(
        ChatAttachmentPickerModule.MODULE_NAME,
    ) ?: return null
    val sources = runCatching { module.supportedSources() }.getOrDefault(emptySet())
    return if (sources.isEmpty()) null else ModuleLlmAttachmentPicker(module, sources)
}

/** Kotlin-side registration required by the native attachment picker module. */
fun chatAttachmentPickerExternalModules(): Map<String, Module> =
    mapOf(ChatAttachmentPickerModule.MODULE_NAME to ChatAttachmentPickerModule())
