package com.agentapp

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AgentApp : Application() {
    /** 应用级协程作用域，不绑定 UI 生命周期 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    // appScope 随进程自动清理，无需手动 cancel

    companion object {
        lateinit var instance: AgentApp
            private set
    }
}
