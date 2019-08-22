package net.bjoernpetersen.musicbot.spi.plugin.predefined

import net.bjoernpetersen.musicbot.api.plugin.Base

/**
 * Marks all video-playback-related interfaces as experimental.
 */
@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalVideoFilePlayback

/**
 * PlaybackFactory capable of playing `.avi` files.
 */
@ExperimentalVideoFilePlayback
@Base
interface AviPlaybackFactory : FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.mkv` files.
 */
@ExperimentalVideoFilePlayback
@Base
interface MkvPlaybackFactory : FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.mp4` files.
 */
@ExperimentalVideoFilePlayback
@Base
interface Mp4PlaybackFactory : FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.wmv` files.
 */
@ExperimentalVideoFilePlayback
@Base
interface WmvPlaybackFactory : FilePlaybackFactory
