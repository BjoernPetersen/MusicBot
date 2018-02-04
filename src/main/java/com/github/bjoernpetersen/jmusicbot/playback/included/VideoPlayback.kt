package com.github.bjoernpetersen.jmusicbot.playback.included

import com.github.bjoernpetersen.jmusicbot.playback.FilePlaybackFactory
import com.google.common.annotations.Beta

@Beta
interface MkvPlaybackFactory : FilePlaybackFactory

@Beta
interface WmvPlaybackFactory : FilePlaybackFactory

@Beta
interface Mp4PlaybackFactory : FilePlaybackFactory

@Beta
interface AviPlaybackFactory : FilePlaybackFactory
