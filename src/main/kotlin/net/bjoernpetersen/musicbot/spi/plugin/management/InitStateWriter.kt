package net.bjoernpetersen.musicbot.spi.plugin.management

@Deprecated(
    "Has been renamed to ProgressFeedback",
    ReplaceWith(
        "ProgressFeedback",
        "net.bjoernpetersen.musicbot.spi.plugin.management.ProgressFeedback"
    ),
    level = DeprecationLevel.ERROR,
)
interface InitStateWriter : ProgressFeedback
