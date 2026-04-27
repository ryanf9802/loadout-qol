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
		JsonArray hotbar = new JsonArray();
		for (int slot = 0; slot < LoadoutLayout.HOTBAR_SIZE; slot++) {
			hotbar.add(JsonNull.INSTANCE);
		}
		legacy.addProperty("version", 1);
		legacy.add("hotbar", hotbar);
		legacy.add("offhand", JsonNull.INSTANCE);

		JsonObject migrated = LoadoutLayoutStore.migrateLegacyJson(legacy);

		assertEquals(2, migrated.get("version").getAsInt());
		assertEquals(LoadoutLayout.HOTBAR_SIZE, migrated.getAsJsonArray("hotbar").size());
		assertTrue(migrated.get("offhand").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("head").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("chest").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("legs").isJsonNull());
		assertTrue(migrated.getAsJsonObject("armor").get("feet").isJsonNull());
	}
}
