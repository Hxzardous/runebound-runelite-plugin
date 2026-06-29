# Security Policy

## Supported Scope

This repository contains only the RuneBound RuneLite plugin.

## Reporting Security Issues

For a public GitHub repository, report vulnerabilities through GitHub Security Advisories when available. Do not file public issues for suspected credential exposure or exploitable network behavior.

## Data Handling

The plugin can send only the selected public RuneScape display name and normal HTTPS request metadata when a user explicitly requests a RuneBound summary lookup.

The plugin must not collect passwords, Jagex account data, private chat, inventory, bank contents, location, combat state, gameplay actions, credentials, keys, or tokens.

## Network Safety

Network summary lookups are disabled by default. When enabled by the user, the plugin sends explicit HTTPS GET requests only to the RuneBound summary endpoint and applies a local per-username cooldown.

`Open Profile` opens a user-initiated rune-bound.net browser URL only. The plugin does not call a RuneBound update endpoint.
