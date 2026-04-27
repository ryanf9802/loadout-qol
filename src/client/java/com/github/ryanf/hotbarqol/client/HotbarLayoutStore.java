package com.github.ryanf.hotbarqol.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HotbarLayoutStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int DATA_VERSION = 1;

	private final Path path;

	public HotbarLayoutStore() {
		this(FabricLoader.getInstance().getConfigDir().resolve("hotbar_qol.json"));
	}

	HotbarLayoutStore(Path path) {
		this.path = path;
	}

	public boolean exists() {
		return Files.exists(path);
	}

	public void saveCurrentLayout(Minecraft client) throws HotbarLayoutException {
		if (client.player == null || client.level == null) {
			throw new HotbarLayoutException("join a world first");
		}

		RegistryOps<JsonElement> ops = registryOps(client.level.registryAccess());
		Inventory inventory = client.player.getInventory();
		JsonObject root = new JsonObject();
		JsonArray hotbar = new JsonArray(HotbarLayout.HOTBAR_SIZE);

		root.addProperty("version", DATA_VERSION);
		for (int slot = 0; slot < HotbarLayout.HOTBAR_SIZE; slot++) {
			hotbar.add(encodeStack(inventory.getItem(slot), ops));
		}
		root.add("hotbar", hotbar);
		root.add("offhand", encodeStack(client.player.getOffhandItem(), ops));

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException exception) {
			throw new HotbarLayoutException(exception.getMessage(), exception);
		}
	}

	public HotbarLayout load(Minecraft client) throws HotbarLayoutException {
		if (client.level == null) {
			throw new HotbarLayoutException("join a world first");
		}
		if (!exists()) {
			throw new HotbarLayoutException("no saved layout");
		}

		JsonObject root;
		try (Reader reader = Files.newBufferedReader(path)) {
			root = GSON.fromJson(reader, JsonObject.class);
		} catch (IOException | JsonParseException exception) {
			throw new HotbarLayoutException(exception.getMessage(), exception);
		}

		if (root == null || root.get("version") == null || root.get("version").getAsInt() != DATA_VERSION) {
			throw new HotbarLayoutException("unsupported or missing layout version");
		}

		JsonArray savedHotbar = root.getAsJsonArray("hotbar");
		if (savedHotbar == null || savedHotbar.size() != HotbarLayout.HOTBAR_SIZE) {
			throw new HotbarLayoutException("layout must contain nine hotbar slots");
		}

		RegistryOps<JsonElement> ops = registryOps(client.level.registryAccess());
		ItemStack[] hotbar = new ItemStack[HotbarLayout.HOTBAR_SIZE];
		for (int slot = 0; slot < HotbarLayout.HOTBAR_SIZE; slot++) {
			hotbar[slot] = decodeStack(savedHotbar.get(slot), ops);
		}

		return new HotbarLayout(hotbar, decodeStack(root.get("offhand"), ops));
	}

	private static JsonElement encodeStack(ItemStack stack, RegistryOps<JsonElement> ops) throws HotbarLayoutException {
		if (stack.isEmpty()) {
			return JsonNull.INSTANCE;
		}

		ItemStack savedStack = stack.copy();
		savedStack.setCount(1);
		return ItemStack.CODEC.encodeStart(ops, savedStack)
				.result()
				.orElseThrow(() -> new HotbarLayoutException("failed to encode item stack"));
	}

	private static ItemStack decodeStack(JsonElement element, RegistryOps<JsonElement> ops) throws HotbarLayoutException {
		if (element == null || element.isJsonNull()) {
			return ItemStack.EMPTY;
		}

		return ItemStack.CODEC.parse(ops, element)
				.result()
				.orElseThrow(() -> new HotbarLayoutException("failed to decode item stack"));
	}

	private static RegistryOps<JsonElement> registryOps(RegistryAccess registryAccess) {
		return registryAccess.createSerializationContext(JsonOps.INSTANCE);
	}
}
