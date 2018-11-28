package net.bjoernpetersen.musicbot.spi.plugin.predefined

import net.bjoernpetersen.musicbot.spi.plugin.Base

/**
 * PlaybackFactory capable of playing `.aac` and `.m4a` files.
 */
@Base
interface AacPlabackFactory :
    FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.flac` files.
 */
@Base
interface FlacPlaybackFactory :
    FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.mp3` files.
 */
@Base
interface Mp3PlaybackFactory :
    FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.opus` files.
 */
@Base
interface OpusPlaybackFactory :
    FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.ogg` and `.oga` files.
 */
@Base
interface VorbisPlaybackFactory :
    FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.wav` and `.wave` files.
 */
@Base
interface WavePlaybackFactory :
    FilePlaybackFactory

/**
 * PlaybackFactory capable of playing `.wma` files.
 */
@Base
interface WmaPlaybackFactory :
    FilePlaybackFactory
