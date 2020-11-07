package net.bjoernpetersen.musicbot.spi.plugin.management

import net.bjoernpetersen.musicbot.spi.plugin.Plugin

/**
 * Feedback object for plugins to use while they are [initializing][Plugin.initialize].
 */
interface ProgressFeedback {
    /**
     * Tell the user about what you're currently doing.
     */
    fun state(state: String)

    /**
     * Warn the user about unusual events which might not be fatal.
     */
    fun warning(warning: String)
}

@Deprecated(
    "Use ProgressFeedback instead",
    replaceWith = ReplaceWith("ProgressFeedback")
)
typealias InitStateWriter = ProgressFeedback
