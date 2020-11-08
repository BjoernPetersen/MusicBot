package net.bjoernpetersen.musicbot.api.config

import java.io.File
import java.nio.file.Path

/**
 * Base class for all UI node implementations.
 *
 * @param T the value type the associated config entry should have
 */
sealed class UiNode<T>

/**
 * A simple, editable text box. For sensitive data, consider using [PasswordBox].
 */
object TextBox : UiNode<String>()

/**
 * An editable text box that hides the actual value.
 */
object PasswordBox : UiNode<String>()

/**
 * A CheckBox. Used by default for [Config.BooleanEntry].
 */
object CheckBox : UiNode<Boolean>()

/**
 * A button that triggers some action.
 *
 * The actual entry value is shown in a read-only textbox.
 *
 * Note that, other than other node types, this one doesn't inherently change the value of the
 * associated entry. It may actively change the value of any entry, or even multiple entries.
 *
 * The [action] is executed on a non-UI thread.
 * Button implementations should make an effort to prevent the action from
 * running multiple times at once.
 *
 * ### Example: "refresh OAuth token" button:
 *
 * - the label is "Refresh"
 * - the backing entry contains an expiration date
 *   - the expiration date is displayed in the read-only textbox
 * - the actual token is stored in another entry
 * - the action performs an OAuth flow and updates the expiration date as well as the actual token entry
 *
 * @param label the label to display on the button
 * @param descriptor a function that converts the entry value to a human-readable form
 * @param action an action to perform when the button is clicked, returns true on success
 */
data class ActionButton<T>(
    val label: String,
    val descriptor: (T) -> String,
    val action: suspend (Config.Entry<T>) -> Boolean
) : UiNode<T>()

/**
 * Some form of input box that only accepts numbers.
 *
 * @param min the minimum value
 * @param max the maximum value
 */
data class NumberBox @JvmOverloads constructor(val min: Int = 0, val max: Int = 100) : UiNode<Int>()

/**
 * A combination of a "choose file/dir" button and a read-only textbox showing the chosen path.
 *
 * @param isDirectory whether a directory is chosen (otherwise a file is chosen)
 * @param isOpen whether to show an open or a save dialog. Must be true if [isDirectory] is true.
 */
data class FileChooser(
    val isDirectory: Boolean = true,
    val isOpen: Boolean = true
) : UiNode<File>() {

    init {
        if (isDirectory && !isOpen)
            throw IllegalArgumentException("isDirectory requires isOpen")
    }
}

/**
 * A combination of a "choose file/dir" button and a read-only textbox showing the chosen path.
 *
 * @param isDirectory whether a directory is chosen (otherwise a file is chosen)
 * @param isOpen whether to show an open or a save dialog. Must be true if [isDirectory] is true.
 */
data class PathChooser(
    val isDirectory: Boolean = true,
    val isOpen: Boolean = true
) : UiNode<Path>() {

    init {
        if (isDirectory && !isOpen)
            throw IllegalArgumentException("isDirectory requires isOpen")
    }
}

/**
 * A dropdown box containing multiple predefined items with no manual input option.
 *
 * The [refresh] function will not be called on a UI thread.
 *
 * @param descriptor converts items to a human-readable string
 * @param refresh a function that is called to update the list of items.
 * May return `null` if it fails to indicate that the old items should be kept.
 * @param lazy whether to only load items if manually requested.
 * Recommended for expensive refresh functions.
 */
data class ChoiceBox<T> @JvmOverloads constructor(
    val descriptor: (T) -> String,
    val refresh: suspend () -> List<T>?,
    val lazy: Boolean = false
) : UiNode<T>()
