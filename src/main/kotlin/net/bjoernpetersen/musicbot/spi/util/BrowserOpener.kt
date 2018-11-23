package net.bjoernpetersen.musicbot.spi.util

import java.net.URL

/**
 * An object capable of opening a URL in the user's preferred web browser.
 */
interface BrowserOpener {

    /**
     * Open the specified URL in the user's default web browser.
     */
    fun openDocument(url: URL)
}
