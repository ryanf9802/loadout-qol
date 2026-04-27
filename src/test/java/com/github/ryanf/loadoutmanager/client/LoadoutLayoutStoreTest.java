package com.github.ryanf.loadoutmanager.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LoadoutLayoutStoreTest {
	@Test
	void migratesLegacyHotbarLayoutWithEmptyArmor() throws LoadoutLayoutException {
		JsonObject legacy = new JsonObject();
		legacy.addProperty("version", 1);
		legacy.add("hotbar", emptyHotbar());
		legacy.add("offhand", JsonNull.INSTANCE);

		JsonObject migrated = LoadoutLayoutStore.migrateLegacyJson(legacy);

		assertEquals(3, migrated.get("version").getAsInt());
		assertEquals(LoadoutLayout.HOTBAR_SIZE, migrated.getAsJsonArray("hotbar").size());
		assertEquals(LoadoutLayout.BACKPACK_SIZE, migrated.getAsJsonArray("backpack").size());
		assertTrue(migrated.get("offhand").isJsonNull());
		for (int slot = 0; slot < LoadoutLayout.BACKPACK_SIZE; slot++) {
			assertTrue(migrated.getAsJsonArray("backpack").get(slot).isJsonNull());
		}
		assertTrue(migrated.getAsJsonObject("armor").get("head").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("chest").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("legs").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("feet").isJsonNull());
	}

	@Test
	void migratesArmorLayoutWithEmptyBackpack() throws LoadoutLayoutException {
		JsonObject armor = new JsonObject();
		JsonObject helmet = new JsonObject();
		helmet.addProperty("id", "minecraft:diamond_helmet");
		armor.add("head", helmet);
		armor.add("chest", JsonNull.INSTANCE);
		armor.add("legs", JsonNull.INSTANCE);
		armor.add("feet", JsonNull.INSTANCE);

		JsonObject current = new JsonObject();
		current.addProperty("version", 2);
		current.add("hotbar", emptyHotbar());
		current.add("offhand", JsonNull.INSTANCE);
		current.add("armor", armor);

		JsonObject migrated = LoadoutLayoutStore.migrateLegacyJson(current);

		assertEquals(3, migrated.get("version").getAsInt());
		assertEquals(LoadoutLayout.BACKPACK_SIZE, migrated.getAsJsonArray("backpack").size());
		assertEquals(helmet, migrated.getAsJsonObject("armor").getAsJsonObject("head"));
		for (int slot = 0; slot < LoadoutLayout.BACKPACK_SIZE; slot++) {
			assertTrue(migrated.getAsJsonArray("backpack").get(slot).isJsonNull());
		}
	}

	private static JsonArray emptyHotbar() {
		JsonArray hotbar = new JsonArray();
		for (int slot = 0; slot < LoadoutLayout.HOTBAR_SIZE; slot++) {
			hotbar.add(JsonNull.INSTANCE);
		}
		return hotbar;
	}
}
