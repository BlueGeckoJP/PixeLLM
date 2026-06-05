package me.bluegecko.pixellm

import android.app.Application

class PixeLLMApplication : Application() {
    lateinit var llmManager: LlmManager private set

    override fun onCreate() {
        super.onCreate()

        llmManager = LlmManager(this)
    }
}
