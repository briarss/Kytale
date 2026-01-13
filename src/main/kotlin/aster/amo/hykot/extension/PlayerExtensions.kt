package aster.amo.hykot.extension

import aster.amo.hykot.coroutines.HytaleDispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Extension functions for player-related operations.
 *
 * These extensions provide Kotlin-idiomatic access to player functionality
 * with coroutine support for async operations.
 */

/**
 * Executes a block of code on the main thread.
 *
 * Useful for performing operations that must be synchronized with the
 * game loop.
 *
 * Example:
 * ```kotlin
 * onMainThread {
 *     // Safe to modify game state here
 * }
 * ```
 *
 * @param block the code to execute
 * @return the result of the block
 */
suspend fun <T> onMainThread(block: suspend () -> T): T {
    return withContext(HytaleDispatchers.Main) { block() }
}

/**
 * Performs an action if the condition is true.
 *
 * Example:
 * ```kotlin
 * player.ifTrue(player.isOnline) {
 *     sendMessage("Hello!")
 * }
 * ```
 *
 * @param condition the condition to check
 * @param block the action to perform
 */
inline fun <T> T.ifTrue(condition: Boolean, block: T.() -> Unit): T {
    if (condition) {
        block()
    }
    return this
}

/**
 * Performs an action if the condition is false.
 *
 * @param condition the condition to check
 * @param block the action to perform
 */
inline fun <T> T.ifFalse(condition: Boolean, block: T.() -> Unit): T {
    if (!condition) {
        block()
    }
    return this
}

/**
 * Transforms this value if not null, otherwise returns the default.
 *
 * @param default the default value if null
 * @param transform the transformation function
 * @return the transformed value or default
 */
inline fun <T, R> T?.ifNotNull(default: R, transform: (T) -> R): R {
    return if (this != null) transform(this) else default
}

/**
 * Runs the block if this value is not null.
 *
 * @param block the block to run
 * @return this value
 */
inline fun <T> T?.whenNotNull(block: (T) -> Unit): T? {
    if (this != null) block(this)
    return this
}

/**
 * Extension for running code safely with exception handling.
 *
 * Catches any exception and returns null instead of throwing.
 *
 * @param block the code to execute
 * @return the result or null if an exception occurred
 */
inline fun <T> safely(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}

/**
 * Extension for running code safely with a default value.
 *
 * @param default the value to return if an exception occurs
 * @param block the code to execute
 * @return the result or the default value
 */
inline fun <T> safelyOrDefault(default: T, block: () -> T): T {
    return try {
        block()
    } catch (e: Exception) {
        default
    }
}

/**
 * Extension for running code safely with exception logging.
 *
 * @param onError callback for handling the exception
 * @param block the code to execute
 * @return the result or null if an exception occurred
 */
inline fun <T> safelyWithLogging(onError: (Exception) -> Unit, block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        onError(e)
        null
    }
}

/**
 * Validates a UUID string.
 *
 * @return true if this string is a valid UUID format
 */
fun String.isValidUUID(): Boolean {
    return try {
        UUID.fromString(this)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}

/**
 * Parses this string as a UUID, or returns null if invalid.
 *
 * @return the parsed UUID or null
 */
fun String.toUUIDOrNull(): UUID? {
    return try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Formats a player-friendly duration string.
 *
 * @param seconds the duration in seconds
 * @return a human-readable duration string
 */
fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        seconds < 86400 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        else -> "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
    }
}

/**
 * Formats a compact duration string (largest unit only).
 *
 * @param seconds the duration in seconds
 * @return a compact duration string
 */
fun formatDurationCompact(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        seconds < 86400 -> "${seconds / 3600}h"
        else -> "${seconds / 86400}d"
    }
}

/**
 * Formats a detailed duration string.
 *
 * @param millis the duration in milliseconds
 * @return a detailed duration string
 */
fun formatDurationDetailed(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return buildString {
        if (days > 0) append("${days}d ")
        if (hours % 24 > 0) append("${hours % 24}h ")
        if (minutes % 60 > 0) append("${minutes % 60}m ")
        append("${seconds % 60}s")
    }.trim()
}

/**
 * Truncates a string to the specified length with an ellipsis.
 *
 * @param maxLength the maximum length
 * @return the truncated string
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this
    else take(maxLength - 3) + "..."
}

/**
 * Truncates a string to the specified length with a custom suffix.
 *
 * @param maxLength the maximum length
 * @param suffix the suffix to use
 * @return the truncated string
 */
fun String.truncate(maxLength: Int, suffix: String): String {
    return if (length <= maxLength) this
    else take(maxLength - suffix.length) + suffix
}

/**
 * Pads a string to center it within the given width.
 *
 * @param width the total width
 * @param padChar the padding character
 * @return the centered string
 */
fun String.center(width: Int, padChar: Char = ' '): String {
    if (length >= width) return this
    val padding = width - length
    val left = padding / 2
    val right = padding - left
    return padChar.toString().repeat(left) + this + padChar.toString().repeat(right)
}

/**
 * Repeats this string n times.
 *
 * @param n the number of repetitions
 * @return the repeated string
 */
fun String.repeat(n: Int): String = (1..n).joinToString("") { this }

/**
 * Converts a camelCase string to Title Case.
 *
 * @return the title case string
 */
fun String.camelToTitleCase(): String {
    return replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.uppercase() }
}

/**
 * Converts a snake_case string to Title Case.
 *
 * @return the title case string
 */
fun String.snakeToTitleCase(): String {
    return split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

/**
 * Formats a number with thousands separators.
 *
 * @return the formatted number string
 */
fun Long.formatWithCommas(): String {
    return "%,d".format(this)
}

/**
 * Formats a number with thousands separators.
 *
 * @return the formatted number string
 */
fun Int.formatWithCommas(): String {
    return "%,d".format(this)
}

/**
 * Formats a number in a compact form (1K, 1M, 1B).
 *
 * @return the compact formatted string
 */
fun Long.formatCompact(): String {
    return when {
        this >= 1_000_000_000 -> "%.1fB".format(this / 1_000_000_000.0)
        this >= 1_000_000 -> "%.1fM".format(this / 1_000_000.0)
        this >= 1_000 -> "%.1fK".format(this / 1_000.0)
        else -> toString()
    }
}

/**
 * Formats a decimal number with specified precision.
 *
 * @param decimals the number of decimal places
 * @return the formatted string
 */
fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}

/**
 * Converts to a percentage string.
 *
 * @param decimals the number of decimal places
 * @return the percentage string
 */
fun Double.toPercentString(decimals: Int = 1): String {
    return "%.${decimals}f%%".format(this * 100)
}

/**
 * Ordinal suffix for a number (1st, 2nd, 3rd, etc).
 *
 * @return the number with ordinal suffix
 */
fun Int.toOrdinal(): String {
    val suffix = when {
        this % 100 in 11..13 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$this$suffix"
}

/**
 * Pluralizes a word based on count.
 *
 * @param singular the singular form
 * @param plural the plural form (default: singular + "s")
 * @return the appropriately pluralized string
 */
fun Int.pluralize(singular: String, plural: String = "${singular}s"): String {
    return "$this ${if (this == 1) singular else plural}"
}
