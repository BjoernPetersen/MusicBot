@file:Suppress("unused", "TooManyFunctions")

package net.bjoernpetersen.musicbot.api.config

import java.io.File
import java.nio.file.Path

/**
 * Configuration object for DSL.
 *
 * ### Required
 *
 * - [label]
 * - [describe]
 * - [action]
 */
class ActionButtonConfiguration<T> internal constructor() {
    /**
     * @see ActionButton.label
     */
    lateinit var label: String
    /**
     * @see ActionButton.descriptor
     */
    private lateinit var descriptor: (T) -> String
    private lateinit var onAction: suspend (Config.Entry<T>) -> Boolean

    /**
     * Describe the entry value as a string.
     *
     * @see ActionButton.descriptor
     */
    fun describe(descriptor: (T) -> String) {
        this.descriptor = descriptor
    }

    /**
     * @see ActionButton.action
     */
    fun action(action: suspend (Config.Entry<T>) -> Boolean) {
        this.onAction = action
    }

    internal fun toNode(): ActionButton<T> {
        val exceptionMessage = when {
            !::label.isInitialized -> "label not set"
            !::descriptor.isInitialized -> "descriptor not set"
            !::onAction.isInitialized -> "action not set"
            else -> null
        }
        if (exceptionMessage != null) throw IllegalStateException(exceptionMessage)

        return ActionButton(
            label = label,
            descriptor = descriptor,
            action = onAction
        )
    }
}

/**
 * Create an [ActionButton].
 */
fun <T> actionButton(configure: ActionButtonConfiguration<T>.() -> Unit): ActionButton<T> {
    val config = ActionButtonConfiguration<T>()
    config.configure()
    return config.toNode()
}

/**
 * Create and use an [ActionButton].
 */
fun <T> SerializedConfiguration<T>.actionButton(
    configure: ActionButtonConfiguration<T>.() -> Unit
) {
    val config = ActionButtonConfiguration<T>()
    config.configure()
    uiNode = config.toNode()
}

/**
 * Create and use an [ActionButton].
 */
fun StringConfiguration.actionButton(
    configure: ActionButtonConfiguration<String>.() -> Unit
) {
    val config = ActionButtonConfiguration<String>()
    config.configure()
    uiNode = config.toNode()
}

/**
 * Create a [PathChooser] for opening files.
 */
fun openFile(): PathChooser {
    return PathChooser(isDirectory = false, isOpen = true)
}

/**
 * Use a [PathChooser] for opening files.
 */
fun SerializedConfiguration<Path>.openFile() {
    uiNode = PathChooser(isDirectory = false, isOpen = true)
}

/**
 * Create a [PathChooser] for opening directories.
 */
fun openDirectory(): PathChooser {
    return PathChooser(isDirectory = true, isOpen = true)
}

/**
 * Use a [PathChooser] for opening directories.
 */
fun SerializedConfiguration<Path>.openDirectory() {
    uiNode = PathChooser(isDirectory = true, isOpen = true)
}

/**
 * Create a [PathChooser] for saving files.
 */
fun saveFile(): PathChooser {
    return PathChooser(isDirectory = false, isOpen = false)
}

/**
 * Use a [PathChooser] for saving files.
 */
fun SerializedConfiguration<Path>.saveFile() {
    uiNode = PathChooser(isDirectory = false, isOpen = false)
}

/**
 * Create a [FileChooser] for opening files.
 */
fun openLegacyFile(): FileChooser {
    return FileChooser(isDirectory = false, isOpen = true)
}

/**
 * Use a [FileChooser] for opening files.
 */
fun SerializedConfiguration<File>.openLegacyFile() {
    uiNode = FileChooser(isDirectory = false, isOpen = true)
}

/**
 * Create a [FileChooser] for opening directories.
 */
fun openLegacyDirectory(): FileChooser {
    return FileChooser(isDirectory = true, isOpen = true)
}

/**
 * Use a [FileChooser] for opening directories.
 */
fun SerializedConfiguration<File>.openLegacyDirectory() {
    uiNode = FileChooser(isDirectory = true, isOpen = true)
}

/**
 * Create a [FileChooser] for saving files.
 */
fun saveLegacyFile(): FileChooser {
    return FileChooser(isDirectory = false, isOpen = false)
}

/**
 * Use a [FileChooser] for saving files.
 */
fun SerializedConfiguration<File>.saveLegacyFile() {
    uiNode = FileChooser(isDirectory = false, isOpen = false)
}

/**
 * Configuration object for DSL.
 *
 * ### Required
 *
 * - [describe]
 * - [refresh]
 *
 * ### Optional
 *
 * - [lazy]
 */
class ChoiceBoxConfiguration<T> internal constructor() {
    private lateinit var descriptor: (T) -> String
    private lateinit var onRefresh: suspend () -> List<T>?
    private var lazy: Boolean = false

    /**
     * Set a function to describe a selected element.
     *
     * @see ChoiceBox.descriptor
     */
    fun describe(descriptor: (T) -> String) {
        this.descriptor = descriptor
    }

    /**
     *  The function to call when the choice items are updated.
     *
     *  @see ChoiceBox.refresh
     */
    fun refresh(action: suspend () -> List<T>?) {
        this.onRefresh = action
    }

    /**
     * Load the choice items lazily.
     *
     * @see ChoiceBox.lazy
     */
    fun lazy() {
        lazy = true
    }

    internal fun toNode(): ChoiceBox<T> {
        val exceptionMessage = when {
            !::descriptor.isInitialized -> "descriptor not set"
            !::onRefresh.isInitialized -> "action not set"
            else -> null
        }
        if (exceptionMessage != null) throw IllegalStateException(exceptionMessage)

        return ChoiceBox(
            descriptor = descriptor,
            refresh = onRefresh,
            lazy = lazy
        )
    }
}

/**
 * Create a [ChoiceBox].
 */
fun <T> choiceBox(configure: ChoiceBoxConfiguration<T>.() -> Unit): ChoiceBox<T> {
    val config = ChoiceBoxConfiguration<T>()
    config.configure()
    return config.toNode()
}

/**
 * Create and use a [ChoiceBox].
 */
fun <T> SerializedConfiguration<T>.choiceBox(
    configure: ChoiceBoxConfiguration<T>.() -> Unit
) {
    val config = ChoiceBoxConfiguration<T>()
    config.configure()
    uiNode = config.toNode()
}
