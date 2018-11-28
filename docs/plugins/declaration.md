# Declaration

## Bases

A plugin may implement several base interfaces, which are annotated either by `@Base`, `@ActiveBase`
or `@IdBase`. Superclasses which are not annotated with either of those are ignored.

### `@Base`

Use this to annotate a base class or interface which other plugins might depend on.
It is recommended to create a base interface or class for your plugin, even if you don't plan on
creating multiple implementations yourself.

### `@IdBase`

If your plugin implements `Provider` or `Suggester`, your plugin will be looked up by its ID.
The ID is derived from its most specific base, which is identified by the `@IdBase` annotation.

If, for some reason, you absolutely don't want to create a base interface, you may also annotate 
your implementation with `@IdBase` directly.

A plugin must not have more than one ID base.

### `@ActiveBase`

This annotation is reserved for predefined base interfaces (`Provider` and `Suggester`).

It marks bases which are actively used by the bot and may have multiple active implementations.

Plugins implementing an active base must also have an ID base.

### Examples

#### Provider

A `Provider` for Spotify songs may have a base interface `SpotifyProvider`, which is declared
as an `@IdBase`.

All implementations of the `SpotifyProvider` base must use the same song IDs to be compatible.

```kotlin
@IdBase
interface SpotifyProvider : Provider
class SpotifyProviderImpl : SpotifyProvider
```

#### PlaybackFactory

The `PlaybackFactory` interface does not have value in itself, so instead, 
just specializations of the `PlaybackFactory` interface are annotated with `@Base`,
for example `Mp3PlaybackFactory` or `YouTubePlaybackFactory`.

---

An implementation using VLC to play a variety of file formats may look like this:

```kotlin
interface VlcPlaybackFactory : Mp3PlaybackFactory, AacPlaybackFactory, FlacPlaybackFactory
class VlcPlaybackFactoryImpl : VlcPlaybackFactory
```

Note that the used playback factories are predefined in the core library.

---

An implementation for Spotify songs may look like this:

```kotlin
@Base
interface SpotifyPlaybackFactory : PlaybackFactory {
    fun getPlayback(spotifySongId: String)
}
class SpotifyProviderImpl : SpotifyPlaybackFactory
```

Note that in this case, the base interface declares the crucial `getPlayback(...)` method itself.
Without it, a `PlaybackFactory` would not be not useful at all.

## Service

In order for the bot to find your plugin, you have to create a file with the fully qualified name
of the implemented specific base interface (e.g. `net.bjoernpetersen.musicbot.spi.plugin.Provider`)
in the `META-INF/services` directory. The file should contain all your implementations of the
base interface, each on its own line.
