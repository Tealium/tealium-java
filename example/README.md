# Tealium Java Example App

A small command-line app that exercises the Tealium Java SDK (`com.tealium:java`)
against real Tealium Collect. It's a subproject of the root build and depends on
the root project (the SDK) directly, so it always runs against the SDK source in
this checkout — no published artifact is needed. It is not published itself.

## Running

From the repository root, using the root Gradle wrapper:

```bash
./gradlew :example:run --args="help"
./gradlew :example:run --args="track button_click color=red"
./gradlew :example:run --args="view product_page sku=abc123 tag=a tag=b"
./gradlew :example:run --args="persist add user_id=123"
./gradlew :example:run --args="persist show"
./gradlew :example:run --args="persist delete user_id"
```

Global options go before the subcommand, e.g.:

```bash
./gradlew :example:run --args="--account mycompany --profile main --env prod track app_open"
```

Building only:

```bash
./gradlew :example:build
```

## Commands

- `track <title> [key=value ...]` — sends an `activity` event.
- `view <title> [key=value ...]` — sends a `view` event.
- `persist show` — prints the current persistent data as JSON.
- `persist add key=value ...` — adds/overwrites persistent data.
- `persist delete key ...` — removes persistent data keys.
- `help` — prints usage (also printed, with exit code 2, on no args or bad args).

A key passed more than once (e.g. `a=1 a=2`) is sent as a list value.

## Global options

| Option | Default | Notes |
|---|---|---|
| `--account` | `tealiummobile` | Tealium account |
| `--profile` | `demo` | Tealium profile |
| `--env` | `dev` | Tealium environment |
| `--datasource` | (none) | Tealium datasource id |
| `--timeout` | SDK default (5000ms) | Dispatch timeout in milliseconds |
| `--log-level` | `VERBOSE` | One of `VERBOSE`, `WARNINGS`, `ERRORS`, `NONE` |

## Persistent data file

Persistent data (used by `persist`, and merged into every `track`/`view` call)
is stored at `~/.tealium/{account}.{profile}.data`. With the default account
and profile, that's `~/.tealium/tealiummobile.demo.data`.

## Network access

`track` and `view` make a real HTTP POST to `https://collect.tealiumiq.com/event`.
They need network access to succeed; dispatch success/failure is printed, and
the process exits `0` on dispatch success, `1` on dispatch failure.
