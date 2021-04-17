package net.bjoernpetersen.musicbot.spi.plugin.management

import net.bjoernpetersen.musicbot.spi.plugin.Plugin

@Deprecated(
    "Has been renamed to ProgressFeedback",
    ReplaceWith(
        "ProgressFeedback",
        "net.bjoernpetersen.musicbot.spi.plugin.management.ProgressFeedback"
    ),
    level = DeprecationLevel.WARNING,
)
/**
 * Feedback object for plugins to use while they are [initializing][Plugin.initialize].
 */
interface InitStateWriter : ProgressFeedback
