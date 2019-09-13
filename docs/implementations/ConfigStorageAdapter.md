# Config storage

The MusicBot config is a simple mapping from `String` keys to `String` values, but there are
many choices when it comes to storing it. The implementation is responsible for providing one or
more adapters for this job.

The easiest implementation would just store the mapping in a config file
(`JSON`/`YAML`/`Properties`),but more complex solutions are of course possible.
 The implementation may choose to encrypt the values or store them on a remote server.
