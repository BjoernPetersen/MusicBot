package net.bjoernpetersen.musicbot.spi.plugin

import net.bjoernpetersen.musicbot.api.plugin.ActiveBase

/**
 * A generic plugin.
 *
 * Implementations can be used to do any number of things, but here are some examples:
 *
 * - A plugin providing authentication for a specific service.
 * Other plugins can depend on it and that way, authentication for that service is handled by this
 * single, central plugin.
 * - A plugin providing literally any non-plugin dependency.
 * - A plugin that actively changes the bot-behavior.
 * The plugin can request various bot-internals by dependency injection and provide features like
 * (for example) media key support.
 * Such a plugin would be enabled even if no other plugin depends on it (see [ActiveBase]).
 */
interface GenericPlugin : Plugin
