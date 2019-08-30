package net.bjoernpetersen.musicbot.spi.plugin.predefined

import net.bjoernpetersen.musicbot.api.plugin.Base

/**
 * PlaybackFactory capable of playing `.aac` and `.m4a` files.
 */
@Base
interface AacStreamPlaybackFactory : StreamPlaybackFactory

/**
 * PlaybackFactory capable of playing `.flac` files.
 */
@Base
interface FlacStreamPlaybackFactory : StreamPlaybackFactory

/**
 * PlaybackFactory capable of playing `.mp3` files.
 */
@Base
interface Mp3StreamPlaybackFactory : StreamPlaybackFactory

/**
 * PlaybackFactory capable of playing `.opus` files.
 */
@Base
interface OpusStreamPlaybackFactory : StreamPlaybackFactory

/**
 * PlaybackFactory capable of playing `.ogg` and `.oga` files.
 */
@Base
interface VorbisStreamPlaybackFactory : StreamPlaybackFactory

/**
 * PlaybackFactory capable of playing `.wav` and `.wave` files.
 */
@Base
interface WaveStreamPlaybackFactory : StreamPlaybackFactory
