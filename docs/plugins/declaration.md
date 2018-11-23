# Declaration

## Bases

A plugin must declare its "bases",
i.e. the plugin interfaces for which it provides an implementation.

### `@IdBase`

The identifying base of the plugin. This might be the implementation class itself, but it is
recommended to create an extra interface or base class for each plugin so it might be replaced
by an alternative implementation.

The ID base must also be included in the bases.

### `@Bases`

These are the interfaces the plugins provides an implementation for.

If any of these interfaces are required by another plugin, this plugin may be chosen by the user
as the implementation.
Note that `Active` plugins, i.e. providers and suggesters, are always implicitly required.

### Examples

#### Provider

A `Provider` for Spotify songs may have a base interface `SpotifyProvider`, which it declares
as its `@IdBase`.
The specific base interface is `Provider`, which has a use in itself, so it is also added
to the bases.

```kotlin
interface SpotifyProvider : Provider
@IdBase(SpotifyProvider::class)
@Bases(SpotifyProvider::class, Provider::class)
class SpotifyProviderImpl : SpotifyProvider
```

#### PlaybackFactory

The `PlaybackFactory` interface does not have value in itself, so it is not included in the bases.
Instead, just specializations of the `PlaybackFactory` interface are included,
for example `Mp3PlaybackFactory` or `YouTubePlaybackFactory`.

---

An implementation for Spotify songs may look like this:

```kotlin
interface SpotifyPlaybackFactory : PlaybackFactory {
    fun getPlayback(spotifySongId: String)
}
@IdBase(SpotifyPlaybackFactory::class)
@Bases(SpotifyPlaybackFactory::class)
class SpotifyProviderImpl : SpotifyPlaybackFactory
```

Note that the base interface declares the crucial `getPlayback(...)` method
without which a `PlaybackFactory` is not useful at all.

---

An implementation using VLC to play a variety of file formats may look like this:

```kotlin
interface VlcPlaybackFactory : Mp3PlaybackFactory, AacPlaybackFactory, FlacPlaybackFactory
@IdBase(VlcPlaybackFactory::class)
@Bases(
    VlcPlaybackFactory::class,
    Mp3PlaybackFactory::class,
    AacPlaybackFactory::class,
    FlacPlaybackFactory::class)
class VlcPlaybackFactoryImpl : VlcPlaybackFactory
```

## Service

In order for the bot to find your plugin, you have to create a file with the fully qualified name
of the implemented specific base interface (e.g. `com.github.bjoernpetersen.musicbot.Provider`)
in the `META-INF/services` directory. The file should contain all your implementations of the
base interface, each on its own line.
