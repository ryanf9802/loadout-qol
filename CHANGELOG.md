# Changelog

## Unreleased

- Saves and applies backpack slot order in addition to hotbar, armor, and offhand.
- Ignores durability and repair cost when matching saved items during apply.
- Ignores shulker, bundle, container loot, and contained entity data when matching saved items while keeping item color/type exact.

## 0.2.0

- Renamed the mod to Loadout Manager.
- Saves and applies armor slots in addition to hotbar and offhand.
- Suppresses successful apply notifications from the hotkey.
- Adds migration from the old `hotbar_qol.json` config file.

## 0.1.0

- Initial Fabric client mod for Minecraft 26.1.1.
- Adds `/hotbarqol save` for saving one hotbar plus offhand layout.
- Adds a configurable apply keybinding, defaulting to `H`.
- Applies layouts through normal inventory moves with exact item/component matching.
