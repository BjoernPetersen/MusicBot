package net.bjoernpetersen.musicbot.spi.plugin.predefined

import net.bjoernpetersen.musicbot.api.plugin.Base

/**
 * PlaybackFactory capable of playing `.avi` files.
 */
@Base
interface AviPlaybackFactory : FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.mkv` files.
 */
@Base
interface MkvPlaybackFactory : FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.mp4` files.
 */
@Base
interface Mp4PlaybackFactory : FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.wmv` files.
 */
@Base
interface WmvPlaybackFactory : FilePlaybackFactory
