# Loadout Manager

Client-side Fabric mod for Minecraft 26.1.1 that saves one hotbar, backpack, armor, and offhand loadout and reapplies it with a keybind.

## Usage

- Arrange the nine hotbar slots, backpack slots, armor slots, and offhand exactly how you want them.
- Run `/loadoutmanager save`.
- Press the `H` keybind, configurable under the Loadout Manager controls category or through Mod Menu if installed, to apply the saved loadout.

The apply action moves whole stacks only, matches item type plus components, and ignores stack counts. Missing saved hotbar, armor, and offhand items leave their target slot empty when inventory space allows it. Empty saved backpack slots stay flexible so other inventory items can fill the remaining spots.

## Build

This project targets Java 25, Minecraft `26.1.1`, Fabric Loader `0.19.2`, and Fabric API `0.145.4+26.1.1`.

```sh
./gradlew build
```
