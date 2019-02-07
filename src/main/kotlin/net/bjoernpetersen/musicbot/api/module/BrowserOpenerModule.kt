package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import net.bjoernpetersen.musicbot.spi.util.BrowserOpener
import javax.inject.Singleton

class BrowserOpenerModule(private val browserOpener: BrowserOpener) : AbstractModule() {
    @Provides
    @Singleton
    fun provideOpener(): BrowserOpener = browserOpener
}
