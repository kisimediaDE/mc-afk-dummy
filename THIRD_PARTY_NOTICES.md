# Third-party notices

## AFKDummy / FakePlayerFarm

Derived from AFKDummy 1.0.3 by subham1920 / FakePlayerFarm and contributors,
declared MIT by its upstream README and Modrinth listing.
See [NOTICE](NOTICE), [LICENSE](LICENSE) and [provenance](docs/UPSTREAM.md).

## Gson 2.13.1 — bundled

Gson is developed by Google and contributors, licensed under Apache 2.0.
The full license is included in [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)
and in the JAR under `META-INF/licenses/Apache-2.0.txt`.

- Source: https://github.com/google/gson/tree/gson-parent-2.13.1
- License: https://github.com/google/gson/blob/gson-parent-2.13.1/LICENSE
- Shadow minimization omits unused classes; Gson source is not modified.

## Build and runtime dependencies

Paper/Minecraft are supplied by the server and are not bundled. JUnit and
Mockito are used for tests only. The Gradle wrapper in the source package
is distributed under Apache 2.0; its existing source headers are retained.
See https://github.com/gradle/gradle/blob/v9.6.1/LICENSE.

bStats is not bundled or initialized in the Community release. Skin requests
may still contact Mojang; this is separate from usage analytics.
