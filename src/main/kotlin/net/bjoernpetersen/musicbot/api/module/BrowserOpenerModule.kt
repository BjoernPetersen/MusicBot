package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import net.bjoernpetersen.musicbot.spi.util.BrowserOpener
import javax.inject.Singleton

/**
 * Guice module which binds an instance of [BrowserOpener].
 *
 * @param browserOpener the instance to bind for the [BrowserOpener] key
 */
class BrowserOpenerModule(private val browserOpener: BrowserOpener) : AbstractModule() {
    /**
     * Supplies a [BrowserOpener].
     */
    @Provides
    @Singleton
    fun provideOpener(): BrowserOpener = browserOpener
}
