# Overview

MusicBot is a library that allows you to create a collaborative music queue using songs from
several providers like Spotify, YouTube or Google Play Music.

**Note:** This site is still in an early stage and not exhaustive yet. Feel free to create a
[GitHub issue](https://github.com/BjoernPetersen/MusicBot/issues) if you have questions or
suggestions.

## Why you should use this

There are several alternatives that provide at least the some of this project's features, for
example Spotify Connect, Google Cast or simply using Bluetooth.
This section will list a few key features that make MusicBot superior to those alternatives.

### Multiple users

Multiple users can connect to the bot with their own, personal devices and enqueue songs.
This is not only more convenient than sharing one device, but also adds some
accountability to the situation:

You no longer have to worry about some half-wit skipping the Piña Colada Song you added to the queue
or wonder who the hilarious dude that snuck Never Gonna Give You Up into the queue for
the millionth time is.

### Multiple song providers

You are not bound to a single provider like Spotify or Google Play Music. You can add songs from
various different providers to the queue and they will be played without any disruption.

With alternatives, you can only play songs from one provider at once. What often happens then is
that somebody wants to listen to a song that's only available somewhere else (typically YouTube).
Following is usually a short disruption, a series of YouTube songs/videos of decreasing quality
and then silence, because nobody went back and resumed the previous playback.

### Independent from remote

If the bot is running on a stationary device (PC, Laptop, Raspberry Pi), no one can run off with
the bot and disrupt the music, as would be the case with simple Bluetooth streaming from a phone.

## Concept

This library defines a common set of interfaces through which the different parts of the bot can
communicate with each other. This includes base interfaces for plugins as well as various interfaces
for server implementations.

The core tries to be compatible with any operating system that supports Java.
This especially includes Android, which only has (limited) Java 8 compatibility.

Plugins are used for everything that's either OS dependent or too volatile to remain stable over a
long time period. Most importantly this includes access to [music provider APIs](plugins/#provider).
