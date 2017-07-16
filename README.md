# JMusicBot

This is the core library of the JMusicBot project.    
APIs should not be considered stable until version 1.0.0.

If you want to have a working version of the bot, [have a look at the JMusicBot-desktop project](https://github.com/BjoernPetersen/JMusicBot-desktop/releases).

## Usage

### Maven
To use this library, simply add the following to your pom.xml:
```xml
<dependency>
    <groupId>com.github.bjoernpetersen</groupId>
    <artifactId>musicbot</artifactId>
    <version>${musicbot.version}</version>
</dependency>
```

### Implementations
For the JMusicBot to run properly,
this library must be wrapped by an implementation for a specific OS.

Implementations provide a way to configure the bot before launching it and
implement various OS dependent interfaces.
If you want to implement your own wrapper, the <code>MusicBot.Builder</code> class
is a good starting point.

### Plugins
TODO
