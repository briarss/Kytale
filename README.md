# HyKot

Kotlin language loader for Hytale server plugins.

Bundles Kotlin runtime with package relocation and provides idiomatic DSLs for plugin development.

## Features

- Kotlin stdlib, reflect, and coroutines (2.2.0)
- kotlinx.serialization for JSON configuration
- Event DSL with reified types
- Command DSL with async/coroutine support
- Config DSL with property delegates
- Scheduler DSL for coroutine-based tasks
- Utility extensions for caching, cooldowns, validation, and math
- Message builder DSL for formatted text

## Quick Start

Add HyKot as a dependency in your `manifest.json`:

```json
{
  "Dependencies": {
    "AmoAster:HyKot": "*"
  }
}
```

Create a plugin extending `KotlinPlugin`:

```kotlin
class MyPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    override fun setup() {
        super.setup()

        // Register events
        events {
            on<PlayerConnectEvent> { event ->
                logger.info { "Player connected: ${event.playerRef.uuid}" }
            }
        }

        // Register commands
        command("greet", "Greet the player") {
            executes { ctx ->
                ctx.sendMessage(Message.raw("Hello!"))
            }
        }
    }

    override fun start() {
        super.start()
        logger.info { "Plugin started!" }
    }

    override fun shutdown() {
        logger.info { "Plugin shutting down..." }
        super.shutdown()
    }
}
```

## Plugin Lifecycle

HyKot plugins follow Hytale's standard lifecycle:

1. **Constructor** - Basic initialization
2. **setup()** - Register events, commands, and entity systems
3. **start()** - Post-setup initialization (all plugins loaded)
4. **shutdown()** - Cleanup when plugin is unloaded

## DSL Examples

### Events

```kotlin
events {
    on<PlayerConnectEvent> { event ->
        // Handle player connection
    }

    on<PlayerChatEvent>(filter = { it.message.startsWith("!") }) { event ->
        // Handle chat commands
    }
}
```

### Commands

```kotlin
command("teleport", "Teleport commands") {
    aliases("tp")

    subcommand("home", "Teleport home") {
        executes { ctx ->
            // Coroutine-enabled execution
            delay(100)
            ctx.sendMessage(Message.raw("Teleporting..."))
        }
    }
}
```

### Configuration

```kotlin
@Serializable
data class MyConfig(
    val maxPlayers: Int = 100,
    val welcomeMessage: String = "Welcome!"
)

val config by jsonConfig<MyConfig>("config") { MyConfig() }
```

### Scheduling

```kotlin
// One-time delayed task
schedule(delay = 5.seconds) {
    logger.info { "Task executed!" }
}

// Repeating task
scheduleRepeating(period = 1.minutes) {
    saveData()
}
```

### Message Formatting

```kotlin
val msg = message {
    colored(Colors.GREEN) { +"Welcome " }
    +playerName
    +"!"
}
player.sendMessage(msg)
```

## License

MIT License
