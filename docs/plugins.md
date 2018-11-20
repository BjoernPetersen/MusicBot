# Plugins

JMusicBot allows you to write a variety of plugins. There are four base classes, which all implement
the same basic `Plugin` contract:

- `GenericPlugin`
- [`PlaybackFactory`](#playbackfactory)
- [`Provider`](#provider)
- `Suggester`

## Lifecycle

Plugins have a well defined lifecycle, and it is extremely important that you understand it in order
to create your own implementation.

### Creation

Every plugin is instantiated by the bot using a public no-arg constructor.
The constructor should do no more than basic object setup, it should especially not bind resources.

While the plugin is in this state, it's dependencies are statically evaluated and the only property
that will be accessed is its `name` property.
The end user chooses which plugins to actually activate and how dependencies are satisfied.

### Configuration

The plugin's dependencies are injected and its configuration methods are called:

- `createConfigEntries(Config): List<Config.Entry<*>>`
- `createSecretEntries(Config): List<Config.Entry<*>>`
- `createStateEntries(Config)`

The three methods receive different `Config` objects appropriate for the specific kind of data.

Only the configuration entries returned by the methods are shown to the user to configure, but you
are free to create more.

In this state the plugin may temporarily do work if configuration is requested. For example, if
a list of choices for a Playlist to play is requested in the config UI, the plugin may connect to
a web service and retrieve a list of possibilities.

### Initialization

The `initialize(InitStateWriter)` method is called and the plugin may allocate resources, start
long-running jobs etc. The plugin remains in this state until it is explicitly closed.

### Destruction

The `close()` method is called and the plugin must release all resources. The instance will never be
reinitialized or used again.

## PlaybackFactory

A `PlaybackFactory` provides a way to actually play a specific kind of song, for example MP3 files
or YouTube videos.

A few restrictions are imposed on Playback factories:

1. It must be possible to pause and resume playback
1. It must be possible to abort playback prematurely
1. The end of a song must be automatically detected and reported to the bot

Note that it is not a requirement to keep track of the duration a song has played.

### Playback

A `PlaybackFactory` creates a new `Playback` object for every song it should play.
The `Playback` object is used to play a song exactly once.

To implement `Playback`, you basically only need to implement two methods: `play()` and `pause()`.

In addition to that, there is a `waitForFinish()` method that is supposed to block any callers until
the song has finished playing. If you choose to extend the `AbstractPlayback` interface 
(recommended) you only don't have to implement the method, but only have to call the `markDone()` 
method once the song has finished.

While this design has some limitations for the bot, it allows you to create implementations for a
variety of services.
For example, it wouldn't be possible to play Spotify songs if the only possibility were to directly
stream songs, because Spotify doesn't allow that. Instead, the Spotify plugin simply controls the
playback of an official Spotify client (which typically runs on the same machine as the bot).

## Provider

Providers are the integral part to integrating music providers into JMusicBot.
They provide access to the songs of a specific source (Spotify, local hard disk, YouTube, etc.)
by offering a `search` by query and a `lookup` by song ID.

A provider also typically declares a [`PlaybackFactory`](#playbackfactory) dependency, which it
delegates to whenever the `Playback` for a song is actually requested via
the `getPlaybackSupplier()` method.
