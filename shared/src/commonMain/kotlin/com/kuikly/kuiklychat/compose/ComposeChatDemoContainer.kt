package com.kuikly.kuiklychat.compose

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.core.module.Module
import com.tencent.kuiklybase.chat.compose.core.chatByteCacheExternalModules
import com.tencent.kuiklybase.chat.kuiklyai.platform.chatAttachmentPickerExternalModules

/**
 * Registers shared Compose demo modules for cache and native attachment picking.
 */
internal abstract class ComposeChatDemoContainer : ComposeContainer() {
    override fun createExternalModules(): Map<String, Module>? {
        val modules = (super.createExternalModules() as? HashMap<String, Module>) ?: hashMapOf()
        modules.putAll(chatByteCacheExternalModules())
        modules.putAll(chatAttachmentPickerExternalModules())
        return modules
    }
}
