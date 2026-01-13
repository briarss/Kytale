package aster.amo.hykot

import aster.amo.hykot.coroutines.HytaleDispatchers
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Abstract base class for Hytale plugins written in Kotlin.
 *
 * Provides a built-in [CoroutineScope] tied to the plugin's lifecycle,
 * with automatic cancellation when the plugin is unloaded.
 *
 * Example usage:
 * ```kotlin
 * class MyPlugin(init: JavaPluginInit) : KotlinPlugin(init) {
 *
 *     init {
 *         logger.atInfo().log("Plugin initializing!")
 *
 *         launch {
 *             // Coroutine runs on async dispatcher
 *             performAsyncTask()
 *         }
 *     }
 * }
 * ```
 *
 * @param init the plugin initialization context provided by the server
 */
abstract class KotlinPlugin(init: JavaPluginInit) : JavaPlugin(init), CoroutineScope {

    private val supervisorJob = SupervisorJob()

    /**
     * The coroutine context for this plugin's scope.
     *
     * Uses a [SupervisorJob] to prevent child coroutine failures from
     * cancelling sibling coroutines, combined with the async dispatcher.
     */
    override val coroutineContext: CoroutineContext
        get() = supervisorJob + HytaleDispatchers.Async

    /**
     * Cancels all coroutines launched in this plugin's scope.
     *
     * Call this when the plugin is being unloaded to clean up resources.
     *
     * @param message optional cancellation message for debugging
     */
    protected fun cancelCoroutines(message: String = "Plugin unloaded") {
        supervisorJob.cancel(message)
    }
}
