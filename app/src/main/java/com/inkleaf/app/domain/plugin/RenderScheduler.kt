package com.inkleaf.app.domain.plugin

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class RenderPriority {
    P0_PROSE,
    P1_SVG_IMAGE,
    P2_HEAVY_PLUGIN
}

class RenderScheduler(
    private val maxConcurrentP2Jobs: Int = 2
) {
    // Semaphore to enforce global concurrency ceiling on heavy WebView/KaTeX jobs (P2)
    private val p2Semaphore = Semaphore(maxConcurrentP2Jobs)

    private val schedulerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun <T> scheduleRender(
        priority: RenderPriority,
        renderBlock: suspend () -> T
    ): T {
        return when (priority) {
            RenderPriority.P0_PROSE -> {
                // Immediate run on thread pool
                withContext(Dispatchers.Default) {
                    renderBlock()
                }
            }
            RenderPriority.P1_SVG_IMAGE -> {
                // Background execution, normal pool
                withContext(Dispatchers.Default) {
                    renderBlock()
                }
            }
            RenderPriority.P2_HEAVY_PLUGIN -> {
                // Controlled execution throttled by semaphore
                p2Semaphore.withPermit {
                    withContext(Dispatchers.Default) {
                        renderBlock()
                    }
                }
            }
        }
    }

    fun cancelAll() {
        schedulerScope.coroutineContext.cancelChildren()
    }
}
