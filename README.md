# Kytale

Kotlin framework for Hytale server plugin development.

Bundles Kotlin runtime with package relocation and provides idiomatic DSLs for plugin development, UI building, and gameplay systems.

## Features

- **Kotlin Runtime** - stdlib, reflect, coroutines (2.2.0), kotlinx.serialization
- **Event DSL** - Type-safe event subscriptions with reified generics
- **Command DSL** - Hierarchical commands with async/coroutine support
- **Config DSL** - JSON configuration with property delegates
- **Scheduler DSL** - Coroutine-based task scheduling
- **UI DSL** - Compile-time UI generation from Kotlin code
- **Interactive UI DSL** - Server-side event handling for UI elements
- **Hexweave** - Optional helper layer for player events, commands, tasks, and ECS systems
- **Extension Functions** - Utilities for entities, vectors, velocity, targeting, damage

## Project Structure
You can find the template mod [here](https://github.com/briarss/KytaleTemplateMod)
```
Kytale/
├── src/main/kotlin/          # Core Kytale library
│   └── aster/amo/kytale/
│       ├── KotlinPlugin.kt   # Base plugin class
│       ├── coroutines/       # Coroutine utilities
│       ├── dsl/              # Event, Command, Config, Scheduler DSLs
│       ├── extension/        # Entity, Vector, Velocity extensions
│       ├── ui/               # UI DSL and Interactive UI
│       └── util/             # Cache, Cooldowns, Validation, Math
├── hexweave/                 # Optional gameplay helper layer
├── gradle-plugin/            # UI compiler Gradle plugin
└── example/                  # Example plugin
```

## Quick Start

### Gradle Setup

Add the repositories and Kytale dependency to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("hytale-mod") version "0.+"

    // Optional: UI DSL compiler (see UI section below)
    id("aster.amo.kytale.ui") version "1.1.0"
}

repositories {
    mavenCentral()
    maven("https://maven.pokeskies.com/releases")
    maven("https://maven.hytale-modding.info/releases")
}

dependencies {
    // Kytale core library
    compileOnly("aster.amo:kytale:1.1.0")

    // Optional: Hexweave helper layer
    compileOnly("aster.amo:hexweave:0.1.0")
}
```

### Plugin Management for Gradle Plugin

To use the UI compiler plugin, add the PokeSkies repository to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.pokeskies.com/releases")
        maven("https://maven.hytale-modding.info/releases")
    }
}

rootProject.name = "my-mod"
```

### Development Setup with Composite Build

For local development, use a composite build in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.pokeskies.com/releases")
        maven("https://maven.hytale-modding.info/releases")
    }
}

rootProject.name = "my-mod"

// Include Kytale for development
includeBuild("../Kytale") {
    dependencySubstitution {
        substitute(module("aster.amo:kytale")).using(project(":"))
        substitute(module("aster.amo:hexweave")).using(project(":hexweave"))
    }
}
```

Then in `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly("aster.amo:kytale")
    runtimeOnly("aster.amo:kytale")

    // Optional: Hexweave helper layer
    compileOnly("aster.amo:hexweave")
    runtimeOnly("aster.amo:hexweave")
}
```

### Runtime Dependency

Add Kytale as a dependency in your `manifest.json`:

```json
{
  "Dependencies": {
    "AmoAster:Kytale": "*"
  }
}
```

### Plugin Setup

Create a plugin extending `KotlinPlugin`:

```kotlin
class MyPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    override fun setup() {
        super.setup()

        // Register events
        event<PlayerConnectEvent> { event ->
            logger.info { "Player connected: ${event.playerRef.uuid}" }
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

1. **Constructor** - Basic initialization
2. **setup()** - Register events, commands, UI pages, and entity systems
3. **start()** - Post-setup initialization (all plugins loaded)
4. **shutdown()** - Cleanup when plugin is unloaded

## DSL Reference

### Events

```kotlin
// Simple event subscription
event<PlayerConnectEvent> { event ->
    logger.info { "Player ${event.playerRef.uuid} connected" }
}

// With filter
event<PlayerChatEvent>(filter = { it.message.startsWith("!") }) { event ->
    handleCommand(event)
}
```

### Commands

```kotlin
command("teleport", "Teleport commands") {
    aliases("tp")

    subcommand("home", "Teleport home") {
        executes { ctx ->
            // Async execution with coroutines
            delay(100)
            ctx.sendMessage(Message.raw("Teleporting..."))
        }
    }

    subcommand("spawn", "Teleport to spawn") {
        executesSync { ctx ->
            // Synchronous execution
            ctx.sendMessage(Message.raw("Teleporting to spawn..."))
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

// Auto-loading JSON config
val config by jsonConfig<MyConfig>("config") { MyConfig() }

// Access values
logger.info { "Max players: ${config.maxPlayers}" }
```

### Scheduling

```kotlin
// One-time delayed task
pluginScope.schedule(delay = 5.seconds) {
    logger.info { "Task executed!" }
}

// Repeating task
pluginScope.scheduleRepeating(period = 1.minutes) {
    saveData()
}
```

## UI DSL

Kytale provides a compile-time UI DSL that generates `.ui` files from Kotlin code.

### Setting Up the Gradle Plugin

1. **Add the plugin** to your `build.gradle.kts`:

```kotlin
plugins {
    id("aster.amo.kytale.ui") version "1.1.0"
}
```

2. **Configure the plugin** (optional):

```kotlin
kytaleUi {
    // Limit scanning to specific packages (faster builds)
    packages.set(listOf("com.example.mymod.ui"))

    // Output directory (default: src/main/resources/Common/UI/Custom/Pages)
    outputDir.set(file("src/main/resources/Common/UI/Custom/Pages"))

    // Run before processResources (default: false)
    compileBeforeProcessResources.set(true)
}
```

3. **Run the compiler**:

```bash
./gradlew compileUi
```

### Creating UI Definitions

Create a class annotated with `@UiDefinition` that has a `registerAll()` function:

```kotlin
@UiDefinition
object MyGameUi {

    fun registerAll() {
        // Register static pages
        UiRegistry.register("MyGame/MainMenu", mainMenuPage)

        // Register interactive pages
        InteractiveUiRegistry.register("MyGame/Settings", settingsPage)
    }

    // Static UI page (no server-side event handling)
    val mainMenuPage = uiPage("MainMenu") {
        width = 500
        height = 400

        title {
            label {
                text = "MAIN MENU"
                style = UiLabelStyle(fontSize = 24, textColor = "#ffffff")
            }
        }

        content {
            textButton("PlayButton") {
                text = "PLAY"
                style = UiButtonStyle(
                    defaultBackground = "#2d5a3d",
                    hoveredBackground = "#3d7a4d"
                )
            }
        }
    }
}
```

### Interactive UI with Event Handling

For UI elements that need server-side event handling (buttons, sliders, etc.):

```kotlin
@UiDefinition
object SettingsUi {

    fun registerAll() {
        InteractiveUiRegistry.register("MyGame/Settings", settingsPage)
    }

    val settingsPage = interactivePage("Settings") {
        width = 500
        height = 400

        title {
            label {
                text = "SETTINGS"
                style = UiLabelStyle(fontSize = 24, textColor = "#ffffff")
            }
        }

        content {
            // Button with click handler
            textButton("SaveButton") {
                primaryButton("SAVE")
                onClick = {
                    // 'player' and 'playerRef' are available in context
                    player.sendMessage(Message.raw("Settings saved!"))
                }
            }

            // Slider with value change handler
            slider("VolumeSlider") {
                min = 0
                max = 100
                value = 50
                onChange = {
                    // 'intValue', 'floatValue', 'stringValue' available
                    player.sendMessage(Message.raw("Volume: $intValue%"))
                }
            }

            // Checkbox with toggle handler
            checkBox("MuteCheckbox") {
                value = false
                onChange = {
                    val muted = boolValue ?: false
                    player.sendMessage(Message.raw("Muted: $muted"))
                }
            }

            // Text field with input handler
            textField("NameField") {
                placeholderText = "Enter name..."
                onChange = {
                    logger.info { "Name changed to: $stringValue" }
                }
                onSubmit = {
                    player.sendMessage(Message.raw("Name set: $stringValue"))
                }
            }
        }
    }
}
```

### Registering UI Pages in Your Plugin

```kotlin
class MyPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    override fun setup() {
        super.setup()

        // Register UI pages
        MyGameUi.registerAll()
        SettingsUi.registerAll()
    }
}
```

### Opening Pages for Players

```kotlin
// Using the extension function
player.openPage("MyGame/Settings", playerRef)

// Or manually
val page = InteractiveUiRegistry.createPage("MyGame/Settings", playerRef)
if (page != null) {
    player.pageManager.openCustomPage(ref, store, page)
}
```

### Available Interactive Elements

| Element | Event | Value Type |
|---------|-------|------------|
| `textButton` | `onClick` | - |
| `button` | `onClick` | - |
| `slider` | `onChange` | `Int` |
| `floatSlider` | `onChange` | `Float` |
| `checkBox` | `onChange` | `Boolean` |
| `textField` | `onChange`, `onSubmit` | `String` |
| `numberField` | `onChange` | `Float` |
| `dropdownBox` | `onChange` | `Int` (index) |
| `colorPicker` | `onChange` | `String` (hex) |
| `itemSlotButton` | `onClick` | - |

## Hexweave Helper Layer

Hexweave is an optional middleware layer that simplifies common plugin patterns like player events, commands, scheduled tasks, and ECS event systems.

### Setup

```kotlin
dependencies {
    compileOnly("aster.amo:hexweave:0.1.0")
    runtimeOnly("aster.amo:hexweave:0.1.0")
}
```

### Usage

```kotlin
class MyPlugin(init: JavaPluginInit) : KotlinPlugin(init) {

    override fun setup() {
        super.setup()

        enableHexweave {
            // Player lifecycle hooks
            players {
                onJoin {
                    logger.info { "Player joined: ${playerRef.uuid}" }
                    playerRef.sendMessage(Message.raw("Welcome to the server!"))
                }
                onLeave {
                    logger.info { "Player left: ${playerRef.uuid}" }
                }
                onChat {
                    if (chatEvent.content.trim().equals("!hello", ignoreCase = true)) {
                        chatEvent.sender.sendMessage(Message.raw("Hello!"))
                    }
                }
            }

            // Commands with player context
            commands {
                literal("greet", "Greet the player") {
                    executesPlayer {
                        sendMessage(Message.raw("Hello, ${player.name}!"))
                    }
                }

                literal("admin", "Admin commands") {
                    subcommand("status", "Show server status") {
                        executesPlayer {
                            sendMessage(Message.raw("Server is running!"))
                        }
                    }
                }
            }

            // Scheduled tasks
            tasks {
                repeating("heartbeat", every = 30.seconds) {
                    logger.info { "Heartbeat tick" }
                }
            }

            // ECS event systems
            systems {
                // Damage system with enriched context
                damageSystem("fall-handler") {
                    filter { it.cause == DamageCause.FALL }
                    onDamage {
                        cancelDamage()
                        playerRef?.sendMessage(Message.raw("Fall damage prevented!"))
                    }
                }

                // Tick system for per-frame entity processing
                tickSystem("velocity-monitor") {
                    query {
                        ArchetypeQuery.builder<EntityStore>()
                            .require(Velocity.getComponentType())
                            .build()
                    }
                    every = 20  // Run every 20 ticks
                    onTick {
                        // Process entities with velocity
                    }
                }

                // Generic ECS event systems (works with ANY EcsEvent type)
                entityEventSystem<EntityStore, PlaceBlockEvent>("block-place-handler") {
                    priority = 10
                    filter { !it.isCancelled }
                    onEvent {
                        // EntityEventContext: index, chunk, store, commandBuffer, event
                        val blockType = event.blockType
                        logger.info { "Block placed: $blockType" }
                    }
                }

                worldEventSystem<EntityStore, TimeChangeEvent>("time-handler") {
                    filter { it.newTime == DayTime.DAWN }
                    onEvent {
                        // WorldEventContext: store, commandBuffer, event
                        logger.info { "Dawn has arrived!" }
                    }
                }
            }
        }
    }
}
```

### Generic ECS Event Systems

Hexweave provides a fully abstract event system DSL that works with **ANY** `EcsEvent` type. When Hytale adds new event types, they automatically work without requiring Hexweave updates.

#### Entity Event Systems

Entity event systems process events for each matching entity:

```kotlin
systems {
    entityEventSystem<EntityStore, SomeEcsEvent>("handler-id") {
        // Optional: execution priority (lower = earlier)
        priority = 10

        // Optional: filter which entities process this event
        query {
            ArchetypeQuery.builder<EntityStore>()
                .require(Health.getComponentType())
                .build()
        }

        // Optional: filter which events to process
        filter { it.someCondition }

        // Optional: execution ordering relative to other systems
        dependencies {
            before<SomeOtherSystem>()
            after<AnotherSystem>()
        }

        // Required: the handler
        onEvent {
            // EntityEventContext provides:
            // - index: Entity index within chunk
            // - chunk: The archetype chunk
            // - store: The entity store
            // - commandBuffer: For queuing modifications
            // - event: The event being processed
            val component = commandBuffer.get<SomeComponent>(chunk, index)
        }
    }
}
```

#### World Event Systems

World event systems handle world-level events (called once per event, not per entity):

```kotlin
systems {
    worldEventSystem<EntityStore, WorldWideEvent>("world-handler") {
        priority = 5
        filter { it.affectsAllPlayers }

        onEvent {
            // WorldEventContext provides:
            // - store: The entity store
            // - commandBuffer: For queuing modifications
            // - event: The event being processed
            logger.info { "World event: ${event.description}" }
        }
    }
}
```

#### When to Use Each Type

| System Type | Use Case |
|-------------|----------|
| `entityEventSystem` | Processing events for specific entities (damage, movement, abilities) |
| `worldEventSystem` | Reacting to global events (time changes, weather, world state) |
| `tickSystem` | Per-tick entity processing |
| `damageSystem` | Damage handling with enriched context (player ref, cancel support) |

## Extension Functions

### Entity Extensions

```kotlin
// Safe component access
val velocity = playerRef.velocity
val transform = playerRef.transform
val position = playerRef.position

// Entity reference
val entityRef = playerRef.entityRef
```

### Vector Extensions

```kotlin
// Factory functions
val v = vec3(1.0, 2.0, 3.0)

// Operators
val sum = v1 + v2
val diff = v1 - v2
val scaled = v * 2.0

// Utilities
val normalized = v.normalized()
val length = v.length()
val horizontal = v.horizontal()  // Zero out Y
val withNewY = v.withY(10.0)
```

### Velocity Extensions

```kotlin
// Simple velocity manipulation
velocity.set(vec3(1.0, 0.0, 0.0))
velocity.add(vec3(0.0, 5.0, 0.0))
velocity.dash(direction, magnitude = 25.0, upBoost = 0.2)
velocity.launch(upwardForce = 10.0)
```

## License

MIT License
