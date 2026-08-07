package com.kuikly.kuiklychat.compose

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.core.module.Module
import com.tencent.kuiklybase.chat.compose.core.chatByteCacheExternalModules

/**
 * Registers shared Compose demo modules such as [ChatByteCacheModule].
 */
internal abstract class ComposeChatDemoContainer : ComposeContainer() {
    override fun createExternalModules(): Map<String, Module>? {
        val modules = (super.createExternalModules() as? HashMap<String, Module>) ?: hashMapOf()
        modules.putAll(chatByteCacheExternalModules())
        return modules
    }
}
