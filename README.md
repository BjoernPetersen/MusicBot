# MusicBot [![GitHub (pre-)release](https://img.shields.io/github/release/BjoernPetersen/MusicBot/all.svg)](https://github.com/BjoernPetersen/MusicBot/releases) [![CircleCI branch](https://img.shields.io/circleci/project/github/BjoernPetersen/MusicBot/master.svg)](https://circleci.com/gh/BjoernPetersen/MusicBot/tree/master) [![GitHub license](https://img.shields.io/github/license/BjoernPetersen/MusicBot.svg)](https://github.com/BjoernPetersen/MusicBot/blob/master/LICENSE)

This is the core library of the MusicBot project.
APIs should not be considered stable until version 1.0.0.

If you want to have a working version of the bot, [have a look at the MusicBot-desktop project](https://github.com/BjoernPetersen/MusicBot-desktop/releases).

## Usage

### Gradle

#### Kotlin DSL

`build.gradle.kts`

```kotlin
dependencies {
    // ...
    implementation("com.github.bjoernpetersen:musicbot:${Lib.MUSICBOT}")
    // or
    implementation(
        group = "com.github.bjoernpetersen",
        name = "musicbot",
        version = Lib.MUSICBOT)
}
```

#### Groovy DSL

`build.gradle`

```groovy
dependencies {
    // ...
    implementation 'com.github.bjoernpetersen:musicbot:$musicbotVersion'
}
```

### Maven

`pom.xml`

```xml
<dependency>
    <groupId>com.github.bjoernpetersen</groupId>
    <artifactId>musicbot</artifactId>
    <version>${musicbot.version}</version>
</dependency>
```

### Implementations

This is only the core library for the MusicBot, it needs to be wrapped by an implementation
to actually work.

The only known implementation can be found in the
[MusicBot-desktop](https://github.com/BjoernPetersen/MusicBot-desktop) project.

### Plugins

For documentation on how to implement plugins, have a look at the JavaDocs or
at the [docs hosted on github.io](https://bjoernpetersen.github.io/MusicBot/).

For several "official" plugins, have a look at the [MusicBot-plugins](https://github.com/BjoernPetersen/MusicBot-plugins) project.

## License

This project is released under the MIT License. That includes every file in this repository,
unless explicitly stated otherwise at the top of a file.
A copy of the license text can be found in the [LICENSE file](LICENSE).
