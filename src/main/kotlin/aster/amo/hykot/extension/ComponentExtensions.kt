package aster.amo.hykot.extension

/**
 * DSL for building text components with formatting.
 *
 * Provides a Kotlin-idiomatic way to create formatted text messages
 * for use in chat, action bars, and other UI elements.
 *
 * Example:
 * ```kotlin
 * val message = text {
 *     +"Welcome "
 *     bold { +"to the server!" }
 *     newline()
 *     color(TextColor.GOLD) { +"Enjoy your stay." }
 * }
 * ```
 */

/**
 * Represents text colors available for formatting.
 */
enum class TextColor(val code: String) {
    BLACK("0"),
    DARK_BLUE("1"),
    DARK_GREEN("2"),
    DARK_AQUA("3"),
    DARK_RED("4"),
    DARK_PURPLE("5"),
    GOLD("6"),
    GRAY("7"),
    DARK_GRAY("8"),
    BLUE("9"),
    GREEN("a"),
    AQUA("b"),
    RED("c"),
    LIGHT_PURPLE("d"),
    YELLOW("e"),
    WHITE("f")
}

/**
 * Builder for creating formatted text components.
 */
class TextBuilder {
    private val parts = mutableListOf<TextPart>()

    /**
     * Appends plain text.
     *
     * @param text the text to append
     */
    operator fun String.unaryPlus() {
        parts.add(TextPart.Plain(this))
    }

    /**
     * Appends text with a specific color.
     *
     * @param color the text color
     * @param block builder for the colored content
     */
    fun color(color: TextColor, block: TextBuilder.() -> Unit) {
        val nested = TextBuilder().apply(block)
        parts.add(TextPart.Colored(color, nested.build()))
    }

    /**
     * Appends bold text.
     *
     * @param block builder for the bold content
     */
    fun bold(block: TextBuilder.() -> Unit) {
        val nested = TextBuilder().apply(block)
        parts.add(TextPart.Bold(nested.build()))
    }

    /**
     * Appends italic text.
     *
     * @param block builder for the italic content
     */
    fun italic(block: TextBuilder.() -> Unit) {
        val nested = TextBuilder().apply(block)
        parts.add(TextPart.Italic(nested.build()))
    }

    /**
     * Appends underlined text.
     *
     * @param block builder for the underlined content
     */
    fun underline(block: TextBuilder.() -> Unit) {
        val nested = TextBuilder().apply(block)
        parts.add(TextPart.Underline(nested.build()))
    }

    /**
     * Appends strikethrough text.
     *
     * @param block builder for the strikethrough content
     */
    fun strikethrough(block: TextBuilder.() -> Unit) {
        val nested = TextBuilder().apply(block)
        parts.add(TextPart.Strikethrough(nested.build()))
    }

    /**
     * Appends obfuscated (scrambled) text.
     *
     * @param block builder for the obfuscated content
     */
    fun obfuscated(block: TextBuilder.() -> Unit) {
        val nested = TextBuilder().apply(block)
        parts.add(TextPart.Obfuscated(nested.build()))
    }

    /**
     * Appends a newline.
     */
    fun newline() {
        parts.add(TextPart.Plain("\n"))
    }

    /**
     * Appends a space.
     */
    fun space() {
        parts.add(TextPart.Plain(" "))
    }

    /**
     * Builds the final formatted string.
     *
     * @return the formatted text string
     */
    fun build(): String {
        return parts.joinToString("") { it.render() }
    }
}

/**
 * Sealed class representing different text formatting parts.
 */
sealed class TextPart {
    abstract fun render(): String

    data class Plain(val text: String) : TextPart() {
        override fun render() = text
    }

    data class Colored(val color: TextColor, val content: String) : TextPart() {
        override fun render() = "§${color.code}$content§r"
    }

    data class Bold(val content: String) : TextPart() {
        override fun render() = "§l$content§r"
    }

    data class Italic(val content: String) : TextPart() {
        override fun render() = "§o$content§r"
    }

    data class Underline(val content: String) : TextPart() {
        override fun render() = "§n$content§r"
    }

    data class Strikethrough(val content: String) : TextPart() {
        override fun render() = "§m$content§r"
    }

    data class Obfuscated(val content: String) : TextPart() {
        override fun render() = "§k$content§r"
    }
}

/**
 * Creates a formatted text component using the builder DSL.
 *
 * Example:
 * ```kotlin
 * val greeting = text {
 *     color(TextColor.GREEN) { +"Hello, " }
 *     bold { +playerName }
 *     +"!"
 * }
 * ```
 *
 * @param block the builder configuration
 * @return the formatted text string
 */
fun text(block: TextBuilder.() -> Unit): String {
    return TextBuilder().apply(block).build()
}

/**
 * Creates a simple colored text string.
 *
 * @param color the text color
 * @param content the text content
 * @return the colored text string
 */
fun coloredText(color: TextColor, content: String): String {
    return "§${color.code}$content§r"
}

/**
 * Strips all color codes from a string.
 *
 * @return the string without color codes
 */
fun String.stripColors(): String {
    return this.replace(Regex("§[0-9a-fk-or]"), "")
}

/**
 * Translates alternate color codes to Minecraft-style codes.
 *
 * @param altCode the alternate code character (default: '&')
 * @return the string with translated color codes
 */
fun String.translateColorCodes(altCode: Char = '&'): String {
    val chars = toCharArray()
    for (i in 0 until chars.size - 1) {
        if (chars[i] == altCode && "0123456789abcdefklmnor".contains(chars[i + 1], ignoreCase = true)) {
            chars[i] = '§'
            chars[i + 1] = chars[i + 1].lowercaseChar()
        }
    }
    return String(chars)
}
