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

/**
 * Create and use an [ActionButton].
 */
@ExperimentalConfigDsl
fun <T> StringConfiguration.actionButton(
    configure: ActionButtonConfiguration<String>.() -> Unit
) {
    val config = ActionButtonConfiguration<String>()
    config.configure()
    uiNode = config.toNode()
}

@ExperimentalConfigDsl
class PathChooserConfiguration {
    private var isDirectorySet = false
    private var isDirectory: Boolean = false
        set(value) {
            isDirectorySet = true
            field = value
        }
    private var isOpen: Boolean = false
    private var initialDir: Path? = null
    private var initialFilename: String? = null

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

    inner class SaveConfiguration {
        init {
            isOpen = false
        }

        inner class FileConfiguration {

            /**
             * @see PathChooser.initialFilename
             */
            var initialFilename: String?
                get() = this@PathChooserConfiguration.initialFilename
                set(value) {
                    this@PathChooserConfiguration.initialFilename = value
                }
        }

        /**
         * @see PathChooser.initialDir
         */
        fun initialDir(path: Path) {
            initialDir = path
        }

        fun file(configure: FileConfiguration.() -> Unit) {
            isDirectory = false
            FileConfiguration().configure()
        }

        fun directory() {
            isDirectory = true
        }
    }

    inner class OpenConfiguration {
        init {
            isOpen = true
        }

        /**
         * @see PathChooser.initialDir
         */
        fun initialDir(path: Path) {
            initialDir = path
        }

        fun file() {
            isDirectory = false
        }

        fun directory() {
            isDirectory = true
        }
    }

    internal fun toNode(): PathChooser {
        if (!isDirectorySet)
            throw IllegalStateException("")
        return PathChooser(
            isDirectory = isDirectory,
            isOpen = isOpen,
            initialDir = initialDir,
            initialFilename = initialFilename
        )
    }
}

/**
 * Create a [PathChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun openPath(configure: PathChooserConfiguration.OpenConfiguration.() -> Unit): PathChooser {
    val config = PathChooserConfiguration()
    config.OpenConfiguration().configure()
    return config.toNode()
}

/**
 * Use a [PathChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<Path>.openPath(
    configure: PathChooserConfiguration.OpenConfiguration.() -> Unit
) {
    val config = PathChooserConfiguration()
    config.OpenConfiguration().configure()
    uiNode = config.toNode()
}

/**
 * Create a [PathChooser] for saving files.
 */
@ExperimentalConfigDsl
fun savePath(configure: PathChooserConfiguration.SaveConfiguration.() -> Unit): PathChooser {
    val config = PathChooserConfiguration()
    config.SaveConfiguration().configure()
    return config.toNode()
}

/**
 * Use a [PathChooser] for saving files.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<Path>.savePath(
    configure: PathChooserConfiguration.SaveConfiguration.() -> Unit
) {
    val config = PathChooserConfiguration()
    config.SaveConfiguration().configure()
    uiNode = config.toNode()
}

@ExperimentalConfigDsl
class FileChooserConfiguration {
    private var isDirectorySet = false
    private var isDirectory: Boolean = false
        set(value) {
            isDirectorySet = true
            field = value
        }
    private var isOpen: Boolean = false
    private var initialDir: File? = null
    private var initialFilename: String? = null

    /**
     * Let the user select a directory instead of files.
     * @see FileChooser.isDirectory
     */
    fun directory() {
        if (!isOpen) throw IllegalStateException("Can't create save dialog for directory")
        isDirectory = true
    }

    /**
     * Let the user select a files instead of directories.
     * @see FileChooser.isDirectory
     */
    fun file() {
        isDirectory = false
    }

    inner class SaveConfiguration {
        init {
            isOpen = false
        }

        inner class FileConfiguration {
            /**
             * @see FileChooser.initialFilename
             */
            var initialFilename: String?
                get() = this@FileChooserConfiguration.initialFilename
                set(value) {
                    this@FileChooserConfiguration.initialFilename = value
                }
        }

        /**
         * @see FileChooser.initialDir
         */
        fun initialDir(path: File) {
            initialDir = path
        }

        fun file(configure: FileConfiguration.() -> Unit) {
            isDirectory = false
            FileConfiguration().configure()
        }

        fun directory() {
            isDirectory = true
        }
    }

    inner class OpenConfiguration {
        init {
            isOpen = true
        }

        /**
         * @see FileChooser.initialDir
         */
        fun initialDir(path: File) {
            initialDir = path
        }

        fun file() {
            isDirectory = false
        }

        fun directory() {
            isDirectory = true
        }
    }

    internal fun toNode(): FileChooser {
        if (!isDirectorySet)
            throw IllegalStateException("")
        return FileChooser(
            isDirectory = isDirectory,
            isOpen = isOpen,
            initialDir = initialDir,
            initialFilename = initialFilename
        )
    }
}

/**
 * Create a [FileChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun openFile(configure: FileChooserConfiguration.OpenConfiguration.() -> Unit): FileChooser {
    val config = FileChooserConfiguration()
    config.OpenConfiguration().configure()
    return config.toNode()
}

/**
 * Use a [FileChooser] for opening files/directories.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<File>.openFile(
    configure: FileChooserConfiguration.OpenConfiguration.() -> Unit
) {
    val config = FileChooserConfiguration()
    config.OpenConfiguration().configure()
    uiNode = config.toNode()
}

/**
 * Create a [FileChooser] for saving files.
 */
@ExperimentalConfigDsl
fun saveFile(configure: FileChooserConfiguration.SaveConfiguration.() -> Unit): FileChooser {
    val config = FileChooserConfiguration()
    config.SaveConfiguration().configure()
    return config.toNode()
}

/**
 * Use a [FileChooser] for saving files.
 */
@ExperimentalConfigDsl
fun SerializedConfiguration<File>.saveFile(
    configure: FileChooserConfiguration.SaveConfiguration.() -> Unit
) {
    val config = FileChooserConfiguration()
    config.SaveConfiguration().configure()
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
