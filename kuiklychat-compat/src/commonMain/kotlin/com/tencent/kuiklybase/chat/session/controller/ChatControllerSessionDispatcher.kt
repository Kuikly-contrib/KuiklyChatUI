package com.tencent.kuiklybase.chat.session.controller

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable

internal class KuiklyPagerDispatcher(
    private val schedule: (Runnable) -> Unit,
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        schedule(block)
    }
}
