package com.kuikly.kuiklychat.a2ui

import android.app.Application
import com.tencent.kuikly.a2ui.adapter.A2UIIconRegistry
import com.tencent.kuikly.a2ui.render.android.A2UIAndroidCustomViewRegistry
import com.tencent.kuikly.a2ui.render.android.A2UIAndroidNativeMethodHandler
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport

/** Lucide SVGs under assets/a2ui_icons, one file per A2UI Icon name. */
private const val ICON_ASSET_DIR = "a2ui_icons"

/** Wires the published KuiklyUI-A2UI Android render module into the Kuikly page host. */
internal object A2uiHostSupport {
    /** Picker dialogs, functionCall and custom functions for embedded surfaces. */
    fun install(application: Application) {
        A2UIAndroidNativeMethodHandler.install(application)
        registerLucideIcons(application)
    }

    /**
     * Same as `A2UIKuiklyBaseViewSupport.wrap()`; the render activity is its own
     * delegate, so it attaches the export directly.
     */
    fun attach(export: IKuiklyRenderExport) {
        A2UIAndroidCustomViewRegistry.attachExport(export)
    }

    /**
     * Switches A2UI Icon from emoji glyphs to images. The registry is all or
     * nothing, so `_fallback.svg` covers names the model invents.
     */
    private fun registerLucideIcons(application: Application) {
        val files = application.assets.list(ICON_ASSET_DIR).orEmpty().filter { it.endsWith(".svg") }
        A2UIIconRegistry.registerIcons(
            files.associate { file -> file.removeSuffix(".svg") to "assets://$ICON_ASSET_DIR/$file" },
        )
    }
}
