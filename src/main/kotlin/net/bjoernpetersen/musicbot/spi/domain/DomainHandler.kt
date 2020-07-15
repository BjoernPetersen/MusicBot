package net.bjoernpetersen.musicbot.spi.domain

/**
 * Keeps track of IP addresses and domains the MusicBot is accessible by.
 */
interface DomainHandler {
    /**
     * Returns a map of Ip address strings to domain names.
     * Used to register the MusicBot at a registry.
     */
    fun getDomainByIp(): Map<String, String>
}
