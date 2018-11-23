# Lifecycle

Plugins have a well defined lifecycle, and it is extremely important that you understand it in order
to create your own implementation.

## Creation

Every plugin is instantiated by the bot using a public no-arg constructor.
The constructor should do no more than basic object setup, it should especially not bind resources.

While the plugin is in this state, it's dependencies are statically evaluated and the only property
that will be accessed is its `name` property.
The end user chooses which plugins to actually activate and how dependencies are satisfied.

## Configuration

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

## Initialization

The `initialize(InitStateWriter)` method is called and the plugin may allocate resources, start
long-running jobs etc. The plugin remains in this state until it is explicitly closed.

## Destruction

The `close()` method is called and the plugin must release all resources. The instance will never be
reinitialized or used again.
