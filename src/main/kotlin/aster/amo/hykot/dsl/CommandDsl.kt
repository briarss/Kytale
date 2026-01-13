package aster.amo.hykot.dsl

import aster.amo.hykot.coroutines.HytaleDispatchers
import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

/**
 * Creates and registers a command using a builder DSL.
 *
 * This DSL provides a Kotlin-idiomatic way to define commands with
 * automatic async execution and coroutine support.
 *
 * Example:
 * ```kotlin
 * command("spawn", "Teleport to spawn") {
 *     aliases("s", "home")
 *
 *     executes { ctx ->
 *         val player = ctx.source
 *         // Teleport logic
 *     }
 *
 *     subcommand("set", "Set spawn location") {
 *         executes { ctx ->
 *             // Set spawn logic
 *         }
 *     }
 * }
 * ```
 *
 * @param name the command name (without leading slash)
 * @param description a brief description of the command
 * @param builder the command configuration block
 */
inline fun JavaPlugin.command(
    name: String,
    description: String,
    builder: CommandBuilder.() -> Unit
) {
    val commandBuilder = CommandBuilder(name, description, this)
    commandBuilder.apply(builder)
    commandRegistry.registerCommand(commandBuilder.build())
}

/**
 * Builder for constructing commands with a fluent DSL.
 *
 * @property name the command name
 * @property description the command description
 * @property plugin the owning plugin
 */
class CommandBuilder(
    val name: String,
    val description: String,
    @PublishedApi internal val plugin: JavaPlugin
) {
    @PublishedApi internal val aliases = mutableListOf<String>()
    @PublishedApi internal val subcommands = mutableListOf<CommandBuilder>()
    @PublishedApi internal var executor: (suspend (CommandContext) -> Unit)? = null

    /**
     * Adds aliases for this command.
     *
     * Example:
     * ```kotlin
     * aliases("tp", "warp", "goto")
     * ```
     *
     * @param names alternative names that can invoke this command
     */
    fun aliases(vararg names: String) {
        aliases.addAll(names)
    }

    /**
     * Sets the execution handler for this command.
     *
     * The handler receives the command context and executes as a coroutine.
     *
     * Example:
     * ```kotlin
     * executes { ctx ->
     *     delay(1000) // Can use suspend functions
     *     ctx.source.sendMessage("Done!")
     * }
     * ```
     *
     * @param block the suspend function to execute
     */
    fun executes(block: suspend (context: CommandContext) -> Unit) {
        executor = block
    }

    /**
     * Adds a subcommand to this command.
     *
     * Example:
     * ```kotlin
     * subcommand("reload", "Reload configuration") {
     *     executes { ctx ->
     *         // Reload logic
     *     }
     * }
     * ```
     *
     * @param name the subcommand name
     * @param description the subcommand description
     * @param builder configuration for the subcommand
     */
    inline fun subcommand(
        name: String,
        description: String,
        builder: CommandBuilder.() -> Unit
    ) {
        val sub = CommandBuilder(name, description, plugin)
        sub.apply(builder)
        subcommands.add(sub)
    }

    /**
     * Builds the command instance.
     *
     * @return the constructed AbstractCommand
     */
    @PublishedApi
    internal fun build(): AbstractCommand {
        val exec = executor
        val subs = subcommands.toList()
        val aliasArray = aliases.toTypedArray()

        return object : AbstractCommand(name, description) {
            init {
                if (aliasArray.isNotEmpty()) {
                    addAliases(*aliasArray)
                }
                subs.forEach { sub ->
                    addSubCommand(sub.build())
                }
            }

            override fun execute(context: CommandContext): CompletableFuture<Void>? {
                if (exec == null) {
                    return CompletableFuture.completedFuture(null)
                }
                val scope = CoroutineScope(HytaleDispatchers.Async)
                return scope.future(HytaleDispatchers.Async) {
                    exec(context)
                    null
                }.thenApply { null }
            }
        }
    }
}

/**
 * Executes a suspending block and returns a CompletableFuture.
 *
 * Useful for bridging Kotlin coroutines with Java's async command patterns.
 *
 * Example:
 * ```kotlin
 * override fun execute(ctx: CommandContext): CompletableFuture<Void>? {
 *     return asyncCommand {
 *         performAsyncOperation()
 *     }
 * }
 * ```
 *
 * @param block the suspend function to execute
 * @return a CompletableFuture that completes when the block finishes
 */
fun asyncCommand(block: suspend () -> Unit): CompletableFuture<Void> {
    val scope = CoroutineScope(HytaleDispatchers.Async)
    return scope.future(HytaleDispatchers.Async) {
        block()
        null
    }.thenApply { null }
}

/**
 * Executes a suspending block with a result and returns a CompletableFuture.
 *
 * @param block the suspend function to execute
 * @return a CompletableFuture that completes with the result
 */
fun <T> asyncResult(block: suspend () -> T): CompletableFuture<T> {
    val scope = CoroutineScope(HytaleDispatchers.Async)
    return scope.future(HytaleDispatchers.Async) { block() }
}

/**
 * Awaits this CompletableFuture in a suspend context.
 *
 * @return the result of the CompletableFuture
 */
suspend fun <T> CompletableFuture<T>.awaitResult(): T {
    return this.await()
}
