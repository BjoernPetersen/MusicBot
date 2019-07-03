@file:Suppress("unused")

package net.bjoernpetersen.musicbot.api.config

import java.io.File
import java.nio.file.Path

@ExperimentalConfigDsl
class ActionButtonConfiguration<T> {
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
        if (!::label.isInitialized)
            throw IllegalStateException("label not set")
        if (!::descriptor.isInitialized)
            throw IllegalStateException("descriptor not set")
        if (!::onAction.isInitialized)
            throw IllegalStateException("action not set")

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
@ExperimentalConfigDsl
fun <T> actionButton(configure: ActionButtonConfiguration<T>.() -> Unit): ActionButton<T> {
    val config = ActionButtonConfiguration<T>()
    config.configure()
    return config.toNode()
}

/**
 * Create and use an [ActionButton].
 */
@ExperimentalConfigDsl
fun <T> SerializedConfiguration<T>.actionButton(
    configure: ActionButtonConfiguration<T>.() -> Unit
) {
    val config = ActionButtonConfiguration<T>()
    config.configure()
    uiNode = config.toNode()
}

@ExperimentalConfigDsl
class PathChooserConfiguration(private val isOpen: Boolean) {
    private var isDirectorySet = false
    private var isDirectory: Boolean = false
        set(value) {
            isDirectorySet = true
            field = value
        }

    /**
     * Let the user select a directory instead of files.
     * @see PathChooser.isDirectory
     */
    fun directory() {
        if (!isOpen) throw IllegalStateException("Can't create save dialog for directory")
        isDirectory = true
    }

    /**
     * Let the user select a files instead of directories.
     * @see PathChooser.isDirectory
     */
    fun file() {
        isDirectory = false
    }

    internal fun toNode(): PathChooser {
        if (!isDirectorySet)
            throw IllegalStateException("")
        return PathChooser(
            isDirectory = isDirectory,
            isOpen = isOpen
        )
    }
}

/**
 * Create a [PathChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun openPath(configure: PathChooserConfiguration.() -> Unit): PathChooser {
    val config = PathChooserConfiguration(true)
    config.configure()
    return config.toNode()
}

/**
 * Use a [PathChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<Path>.openPath(configure: PathChooserConfiguration.() -> Unit) {
    val config = PathChooserConfiguration(true)
    config.configure()
    uiNode = config.toNode()
}

/**
 * Create a [PathChooser] for saving files.
 */
@ExperimentalConfigDsl
fun savePath(configure: PathChooserConfiguration.() -> Unit): PathChooser {
    val config = PathChooserConfiguration(false)
    config.configure()
    return config.toNode()
}

/**
 * Use a [PathChooser] for saving files.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<Path>.savePath(configure: PathChooserConfiguration.() -> Unit) {
    val config = PathChooserConfiguration(false)
    config.configure()
    uiNode = config.toNode()
}

@ExperimentalConfigDsl
class FileChooserConfiguration(internal val isOpen: Boolean) {
    private var isDirectorySet = false
    private var isDirectory: Boolean = false
        set(value) {
            isDirectorySet = true
            field = value
        }

    /**
     * Let the user select a directory instead of files.
     */
    fun directory() {
        if (!isOpen) throw IllegalStateException("Can't create save dialog for directory")
        isDirectory = true
    }

    /**
     * Let the user select a files instead of directories.
     */
    fun file() {
        isDirectory = false
    }

    internal fun toNode(): FileChooser {
        if (!isDirectorySet)
            throw IllegalStateException("")
        return FileChooser(
            isDirectory = isDirectory,
            isOpen = isOpen
        )
    }
}

/**
 * Create a [FileChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun openFile(configure: FileChooserConfiguration.() -> Unit): FileChooser {
    val config = FileChooserConfiguration(true)
    config.configure()
    return config.toNode()
}

/**
 * Use a [FileChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<File>.openFile(configure: FileChooserConfiguration.() -> Unit) {
    val config = FileChooserConfiguration(true)
    config.configure()
    uiNode = config.toNode()
}

/**
 * Create a [FileChooser] for saving files.
 */
@ExperimentalConfigDsl
fun saveFile(configure: FileChooserConfiguration.() -> Unit): FileChooser {
    val config = FileChooserConfiguration(false)
    config.configure()
    return config.toNode()
}

/**
 * Use a [FileChooser] for saving files.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<File>.saveFile(configure: FileChooserConfiguration.() -> Unit) {
    val config = FileChooserConfiguration(false)
    config.configure()
    uiNode = config.toNode()
}

@ExperimentalConfigDsl
class ChoiceBoxConfiguration<T> {
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
        if (!::descriptor.isInitialized)
            throw IllegalStateException("descriptor not set")
        if (!::onRefresh.isInitialized)
            throw IllegalStateException("action not set")

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
@ExperimentalConfigDsl
fun <T> choiceBox(configure: ChoiceBoxConfiguration<T>.() -> Unit): ChoiceBox<T> {
    val config = ChoiceBoxConfiguration<T>()
    config.configure()
    return config.toNode()
}

/**
 * Create and use a [ChoiceBox].
 */
@ExperimentalConfigDsl
fun <T> SerializedConfiguration<T>.choiceBox(
    configure: ChoiceBoxConfiguration<T>.() -> Unit
) {
    val config = ChoiceBoxConfiguration<T>()
    config.configure()
    uiNode = config.toNode()
}
