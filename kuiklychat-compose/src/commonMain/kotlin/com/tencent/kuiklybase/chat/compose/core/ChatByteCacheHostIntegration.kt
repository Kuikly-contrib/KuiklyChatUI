package com.tencent.kuiklybase.chat.compose.core

import com.tencent.kuikly.core.module.Module

/**
 * Kuikly-side Module registrations required for [ChatByteCacheModule].
 *
 * Merge into the host pager or Compose container:
 *
 * ```kotlin
 * override fun createExternalModules(): Map<String, Module>? {
 *     val modules = hashMapOf<String, Module>()
 *     modules.putAll(chatByteCacheExternalModules())
 *     return modules
 * }
 * ```
 *
 * The host must also register the matching native module (for example
 * `KRChatByteCacheModule` on Android) via `registerExternalModule`.
 */
public fun chatByteCacheExternalModules(): Map<String, Module> =
    mapOf(ChatByteCacheModule.MODULE_NAME to ChatByteCacheModule())
