package net.bjoernpetersen.musicbot.spi.plugin.management

interface InitStateWriter {
    /**
     * Tell the user about what you're currently doing.
     */
    fun state(state: String)

    /**
     * Warn the user about unusual events which might not be fatal.
     */
    fun warning(warning: String)
}
