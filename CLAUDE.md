# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Tealium Java library (`com.tealium:java`) — sends events to Tealium Collect / AudienceStream. Single runtime dependency: `jackson-jr-objects` (JSON). Published to an S3 Maven repo (`s3://maven.tealiumiq.com/java/releases/`) via Gradle's `maven-publish` plugin, with `AwsCredentials` resolved from the Gradle properties `tealiumAccessKey` / `tealiumSecretKey` / `tealiumSessionToken`.

## Commands

Use the Gradle wrapper (`./gradlew`); no system Gradle install required.

```bash
./gradlew build                                      # build jar (runs tests)
./gradlew test                                        # all tests (JUnit 4)
./gradlew test --tests TealiumTests                   # single test class
./gradlew test --tests 'TealiumTests.testTealiumInit' # single test method
```

No lint, checkstyle, or static-analysis tooling is configured.

`CollectDispatcherTests` and `TealiumTests` make **real HTTP requests** to `https://collect.tealiumiq.com/event` (plus an intentionally unreachable host), so they need network access.

## Compatibility constraints

- `build.gradle.kts` sets `options.release = 8` on `JavaCompile`, which produces Java 8 bytecode and has `javac` enforce the Java 8 API surface (no APIs added after Java 8). Building itself requires JDK 17+, since that's what runs Gradle.
- The library runs on JDK 8 and later. Don't use APIs that later JDKs removed (e.g. the Java EE modules dropped in JDK 9/11). 1.4.0 removed a `javax.xml.ws.http.HTTPException` reference for exactly this reason.
- `LibraryContext.version` (sent as `tealium_library_version`) is generated: the `generateBuildConfig` task in `build.gradle.kts` writes `com/tealium/BuildConfig.java` from the project's `version`, and `LibraryContext.version` reads `BuildConfig.VERSION`. A release only needs to bump `version` in `build.gradle.kts` plus add a "Change Log" entry in `README.md`.

## Architecture

Everything lives in the single package `com.tealium`. `Tealium` and `Udo` are the main public entry points; additional public types include `DataManager`, `TextStorage`, `LogLevel`, and several exception classes.

Data flow for `Tealium.track(eventType, title, Udo data, DispatchCallback)`:

1. **`Tealium.Builder`** (`new Tealium.Builder(account, profile).setEnvironment(...).build()`) creates the immutable `LibraryContext` (account/profile/environment/datasource/`Logger`). It also creates `TextStorage` at `~/.tealium/{account}.{profile}.data`, and a `CollectDispatcher` unless one was injected via `setCollectDispatcher()`.
2. **`DataManager`** builds the payload layers:
   - persistent data, via `PersistentUdo` → `TextStorage` (a JSON file, cached in memory, with legacy percent-encoded files still readable). `addPersistentData` / `deletePersistentData` write it back.
   - volatile data, regenerated on every call: timestamp, random, session_id.
   - universal keys.

   Key names live in `DataManager.Key` and event types in `DataManager.EventType`.
3. `Tealium` merges those layers with the event keys and the caller's `Udo`, then calls **`CollectDispatcher.dispatch`**. That method POSTs JSON over a blocking `HttpURLConnection` on the caller's thread; there are no executors. A dispatch succeeds only with HTTP 200 and no `x-error` response header. Failures are wrapped in `CollectDispatchException`. The result reaches the caller through `DispatchCallback.dispatchComplete(success, info, error)`.

`Udo` wraps a `Map<String, Object>` whose values are `String` or `List<String>`. Overloads that take a raw `Map` are deprecated in favor of `Udo`.

## Tests

The tests live in `src/test/java/com/tealium/`, all in one flat directory. `TestUtils` provides in-memory `TextStorage`/`PersistentUdo` fakes and sample maps; `TestLibraryContext` builds contexts. Mockito is used in `PersistentUdoTests`; it's also wired as a Java agent (`mockitoAgent` configuration + `-javaagent` on `tasks.test`) to avoid JDK 21+'s "dynamically loaded agent" warning.
