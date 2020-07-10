package net.bjoernpetersen.musicbot.spi.domain

/**
 * Keeps track of IpAddresses and Domains the MusicBot is accessible by.
 */
interface DomainHandler {
    /**
     * Returns a map of IpAddress strings to domain names.
     * Used to register the MusicBot at a registry.
     */
    val ipDomains: Map<String, String>
}
