package aster.amo.hykot.extension

import aster.amo.hykot.coroutines.HytaleDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

/**
 * Extension functions for Hytale World and Universe operations.
 *
 * In Hytale's architecture:
 * - A [Universe] represents the entire server (all Worlds, all player data)
 * - A [World] is a single game world within the Universe
 * - Players are accessed via [PlayerRef] from the Universe
 *
 * These extensions provide Kotlin-idiomatic access to world functionality
 * with coroutine support for async operations.
 *
 * @see com.hypixel.hytale.server.core.universe.Universe
 * @see com.hypixel.hytale.server.core.universe.World
 */

/**
 * Creates a coroutine dispatcher for this world.
 *
 * Operations dispatched to this context will run on the world's thread,
 * which is useful for world-specific operations in multi-world setups.
 *
 * Example:
 * ```kotlin
 * withContext(world.dispatcher) {
 *     // Operations run on this world's thread
 *     spawnEntity(location)
 * }
 * ```
 *
 * @param executor the executor for this world's thread
 * @return a dispatcher bound to the world's execution context
 */
fun worldDispatcher(executor: Executor): CoroutineDispatcher {
    return executor.asCoroutineDispatcher()
}

/**
 * Executes a block on the main server thread.
 *
 * @param block the code to execute
 * @return the result of the block
 */
suspend fun <T> onServerThread(block: suspend () -> T): T {
    return withContext(HytaleDispatchers.Main) { block() }
}

/**
 * Schedules block operations to run on the appropriate thread.
 *
 * @param block the operations to perform
 */
suspend fun runBlockOperation(block: suspend () -> Unit) {
    withContext(HytaleDispatchers.Main) { block() }
}

/**
 * Executes chunk-related operations safely on the correct thread.
 *
 * @param chunkX the chunk X coordinate
 * @param chunkZ the chunk Z coordinate
 * @param operation the operation to perform on the chunk
 */
suspend fun <T> chunkOperation(
    chunkX: Int,
    chunkZ: Int,
    operation: suspend () -> T
): T {
    return withContext(HytaleDispatchers.Main) {
        operation()
    }
}

/**
 * Data class representing a location in a world.
 *
 * @property worldName the name of the world
 * @property x the X coordinate
 * @property y the Y coordinate
 * @property z the Z coordinate
 * @property yaw the yaw rotation (horizontal)
 * @property pitch the pitch rotation (vertical)
 */
data class Location(
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    /**
     * Creates a new location offset by the given amounts.
     *
     * @param dx the X offset
     * @param dy the Y offset
     * @param dz the Z offset
     * @return a new location with the applied offset
     */
    fun offset(dx: Double, dy: Double, dz: Double): Location {
        return copy(x = x + dx, y = y + dy, z = z + dz)
    }

    /**
     * Gets the block coordinates of this location.
     *
     * @return a triple of (blockX, blockY, blockZ)
     */
    fun toBlockCoordinates(): Triple<Int, Int, Int> {
        return Triple(x.toInt(), y.toInt(), z.toInt())
    }

    /**
     * Calculates the distance to another location.
     *
     * @param other the other location
     * @return the distance between the two locations
     */
    fun distanceTo(other: Location): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculates the squared distance to another location.
     *
     * More efficient than [distanceTo] when you only need to compare distances.
     *
     * @param other the other location
     * @return the squared distance between the two locations
     */
    fun distanceSquaredTo(other: Location): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }
}

/**
 * Creates a location from coordinates.
 *
 * @param worldName the world name
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @return a new Location instance
 */
fun location(worldName: String, x: Double, y: Double, z: Double): Location {
    return Location(worldName, x, y, z)
}

/**
 * Creates a location from integer coordinates.
 *
 * @param worldName the world name
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 * @return a new Location instance
 */
fun location(worldName: String, x: Int, y: Int, z: Int): Location {
    return Location(worldName, x.toDouble(), y.toDouble(), z.toDouble())
}
