# Hotbar QoL

Client-side Fabric mod for Minecraft 26.1.1 that saves one hotbar/offhand layout and reapplies it with a keybind.

## Usage

- Arrange the nine hotbar slots and offhand exactly how you want them.
- Run `/hotbarqol save`.
- Press the `H` keybind, configurable under the Hotbar QoL controls category, to apply the saved layout.

The apply action moves whole stacks only, matches item type plus components, and ignores stack counts. Missing saved items leave their target slot empty when inventory space allows it.

## Build

This project targets Java 25, Minecraft `26.1.1`, Fabric Loader `0.19.2`, and Fabric API `0.145.4+26.1.1`.

```sh
gradle build
```
