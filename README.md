# RuneBound RuneLite Plugin

RuneBound is an Old School RuneScape profile and progression companion. This repository contains the standalone RuneBound RuneLite plugin.

The plugin is display-only. It shows a RuneBound side panel, opens RuneBound pages in the browser, and can request a cached public profile summary from rune-bound.net.

## Features

- Detect the logged-in player's public RuneScape display name through RuneLite.
- Let the user manually look up a public RuneScape display name.
- Display cached RuneBound profile summary fields when they are available.
- Open RuneBound player pages in the browser.
- Respect a 15 minute local cache TTL and a 30 minute same-player lookup cooldown.

## Privacy

Network summary lookups are disabled by default. If enabled, the plugin may send the selected public RuneScape display name and normal HTTPS request metadata to RuneBound when the user clicks `Refresh`.

RuneLite may show a third-party network warning when summary lookups are enabled. That warning is expected for Plugin Hub plugins that can contact a non-RuneLite service.

The summary endpoint is:

```text
GET https://rune-bound.net/api/runelite/v1/players/{username}/summary
```

The plugin does not:

- Automate gameplay.
- Click, move, fight, skill, select menu entries, modify packets, or interact with game input.
- Collect passwords or Jagex account data.
- Collect private chat.
- Collect inventory, bank, location, combat state, world, or gameplay actions.
- Send hidden telemetry.
- Trigger profile updates directly.
- Write profile data from the client.
- Store credentials, API keys, tokens, or service accounts.

## Usage

1. Open the RuneBound side panel in RuneLite.
2. Optionally enable `Enable RuneBound summary lookups` in plugin config.
3. Use the current detected player or enter a public username manually.
4. Click `Refresh` to request a cached summary.
5. Use `Open Profile` to view the player on RuneBound.

Direct profile updates are not included in V1. Use the RuneBound profile page in your browser to begin tracking or refresh a profile.

Automatic logout-triggered updates are not included in V1. They are a possible V2 feature only if they can remain opt-in, rate-limited, and Plugin Hub-safe.

If no cached summary exists, the panel shows:

```text
No cached summary yet. Open Profile on RuneBound to begin tracking.
```

## Development

Requirements:

- Java 11.
- The Gradle wrapper included in this repository.
- RuneLite development-client setup.

Build:

```bash
./gradlew build
```

Test:

```bash
./gradlew test
```

Run the development client:

```bash
./gradlew run
```

If using a Jagex Account, follow RuneLite's official development-client instructions for Jagex Accounts.

## Plugin Hub Notes

- The plugin targets Java 11.
- The root `icon.png` is 48x48 px.
- `runelite-plugin.properties` identifies the plugin class and metadata.
- Third-party network lookups are opt-in and use RuneLite's warning text.
- Browser actions open only rune-bound.net URLs.

## License

BSD-2-Clause. See [LICENSE](LICENSE).
