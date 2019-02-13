# MusicBot [![GitHub (pre-)release](https://img.shields.io/github/release/BjoernPetersen/JMusicBot/all.svg)](https://github.com/BjoernPetersen/JMusicBot/releases) [![CircleCI branch](https://img.shields.io/circleci/project/github/BjoernPetersen/JMusicBot/master.svg)](https://circleci.com/gh/BjoernPetersen/JMusicBot/tree/master) [![GitHub license](https://img.shields.io/github/license/BjoernPetersen/JMusicBot.svg)](https://github.com/BjoernPetersen/JMusicBot/blob/master/LICENSE)

This is the core library of the JMusicBot project.
APIs should not be considered stable until version 1.0.0.

If you want to have a working version of the bot, [have a look at the JMusicBot-desktop project](https://github.com/BjoernPetersen/JMusicBot-desktop/releases).

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

This is only the core library for the MusicBot, it needs to be wrapped an implementation
to actually work.

The only known implementation can be found in the
[JMusicBot-desktop](https://github.com/BjoernPetersen/JMusicBot-desktop) project.

### Plugins

For documentation on how to implement plugins, have a look at the JavaDocs or
at the [docs hosted on github.io](https://bjoernpetersen.github.io/JMusicBot/).
