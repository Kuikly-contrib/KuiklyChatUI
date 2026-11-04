package com.kuikly.kuiklychat

import android.app.Application
import com.kuikly.kuiklychat.a2ui.A2uiHostSupport

class KRApplication : Application() {

    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        A2uiHostSupport.install(this)
    }

    companion object {
        lateinit var application: Application
    }
}