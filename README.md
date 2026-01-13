# HyKot

Kotlin language loader for Hytale server plugins.

Bundles Kotlin runtime with package relocation and provides idiomatic DSLs for plugin development.

## Features

- Kotlin stdlib, reflect, and coroutines (2.2.0)
- Event DSL with reified types
- Command DSL with async/coroutine support
- Config DSL with property delegates
- Scheduler DSL for coroutine-based tasks
- Utility extensions for caching, cooldowns, validation, and math

## Quick Start

Add to your `manifest.json`:

```json
{
  "Dependencies": {
    "AmoAster:HyKot": "*"
  }
}
```

```kotlin
class MyPlugin(init: JavaPluginInit) : KotlinPlugin(init) {
    override fun onEnable() {
        event<PlayerConnectEvent> { e ->
            logger.atInfo().log("Welcome %s!", e.player.name)
        }
    }
}
```

## License

CC0 1.0 Universal
