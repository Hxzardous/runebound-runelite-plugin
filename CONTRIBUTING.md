# Contributing

RuneBound RuneLite plugin changes must stay informational, reviewable, and safe for RuneLite Plugin Hub consideration.

## Safety Rules

- Do not automate gameplay.
- Do not click, move, fight, skill, select menu entries, modify packets, or interact with game input.
- Do not collect passwords, Jagex account data, private chat, inventory, bank, location, combat state, or gameplay actions.
- Do not add hidden polling, game-tick network refreshes, or repeated background requests.
- Do not add secrets, API keys, tokens, service accounts, or private credentials.
- Keep summary lookups explicit, opt-in, read-only, and routed only through RuneBound's summary endpoint.
- Do not call profile update endpoints from the plugin.

## Development Checks

Run before opening a pull request:

```bash
./gradlew clean
./gradlew test
./gradlew check
```

Manually verify RuneLite startup with:

```bash
./gradlew run
```

Only manually interact with the RuneLite/game UI. Do not automate RuneScape input for testing.
