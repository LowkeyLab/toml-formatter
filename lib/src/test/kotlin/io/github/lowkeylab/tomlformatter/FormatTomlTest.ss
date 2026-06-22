╔═ snapshots formatted TOML fixture basic-spacing ═╗
title = "TOML Formatter"

[owner]
name = "Lowkey Lab"
enabled = true

╔═ snapshots formatted TOML fixture comments-inline ═╗
# top-level comment
[database]
ports = [8000, 8001, 8002]
connection = { host = "localhost", ssl = true }
# trailing section comment

╔═ snapshots formatted TOML fixture nested-arrays ═╗
[package]
name = "formatter"
keywords = ["toml", "wasm", "kotlin"]

[[package.targets]]
name = "jvm"
enabled = true

[[package.targets]]
name = "wasm"
enabled = true

╔═ [end of file] ═╗
