package com.github.ryanf.loadoutmanager.client;

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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LoadoutLayoutStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int DATA_VERSION = 3;
	private static final int ARMOR_DATA_VERSION = 2;
	private static final int LEGACY_DATA_VERSION = 1;

	private final Path path;
	private final Path legacyPath;

	public LoadoutLayoutStore() {
		this(
				FabricLoader.getInstance().getConfigDir().resolve("loadout_manager.json"),
				FabricLoader.getInstance().getConfigDir().resolve("hotbar_qol.json")
		);
	}

	LoadoutLayoutStore(Path path, Path legacyPath) {
		this.path = path;
		this.legacyPath = legacyPath;
	}

	public boolean exists() {
		return Files.exists(path) || Files.exists(legacyPath);
	}

	public void saveCurrentLayout(Minecraft client) throws LoadoutLayoutException {
		if (client.player == null || client.level == null) {
			throw new LoadoutLayoutException("join a world first");
		}

		RegistryOps<JsonElement> ops = registryOps(client.level.registryAccess());
		Inventory inventory = client.player.getInventory();
		JsonObject root = new JsonObject();
		JsonArray hotbar = new JsonArray(LoadoutLayout.HOTBAR_SIZE);
		JsonArray backpack = new JsonArray(LoadoutLayout.BACKPACK_SIZE);

		root.addProperty("version", DATA_VERSION);
		for (int slot = 0; slot < LoadoutLayout.HOTBAR_SIZE; slot++) {
			hotbar.add(encodeStack(inventory.getItem(slot), ops));
		}
		for (int slot = 0; slot < LoadoutLayout.BACKPACK_SIZE; slot++) {
			backpack.add(encodeStack(inventory.getItem(LoadoutLayout.HOTBAR_SIZE + slot), ops));
		}
		root.add("hotbar", hotbar);
		root.add("backpack", backpack);
		root.add("offhand", encodeStack(client.player.getOffhandItem(), ops));
		root.add("armor", encodeArmor(new LoadoutLayout.Armor(
				client.player.getItemBySlot(EquipmentSlot.HEAD),
				client.player.getItemBySlot(EquipmentSlot.CHEST),
				client.player.getItemBySlot(EquipmentSlot.LEGS),
				client.player.getItemBySlot(EquipmentSlot.FEET)
		), ops));

		write(root);
	}

	public LoadoutLayout load(Minecraft client) throws LoadoutLayoutException {
		if (client.level == null) {
			throw new LoadoutLayoutException("join a world first");
		}
		if (!exists()) {
			throw new LoadoutLayoutException("no saved layout");
		}

		JsonObject root = readRoot(Files.exists(path) ? path : legacyPath);
		RegistryOps<JsonElement> ops = registryOps(client.level.registryAccess());
		LoadoutLayout layout = decodeLayout(root, ops);

		if (!Files.exists(path) && Files.exists(legacyPath)) {
			write(encodeLayout(layout, ops));
		}

		return layout;
	}

	static JsonObject migrateLegacyJson(JsonObject root) throws LoadoutLayoutException {
		if (root == null || root.get("version") == null) {
			throw new LoadoutLayoutException("unsupported or missing layout version");
		}

		int version = root.get("version").getAsInt();
		if (version != LEGACY_DATA_VERSION && version != ARMOR_DATA_VERSION) {
			throw new LoadoutLayoutException("unsupported or missing layout version");
		}

		JsonArray savedHotbar = root.getAsJsonArray("hotbar");
		if (savedHotbar == null || savedHotbar.size() != LoadoutLayout.HOTBAR_SIZE) {
			throw new LoadoutLayoutException("layout must contain nine hotbar slots");
		}

		JsonObject migrated = new JsonObject();
		migrated.addProperty("version", DATA_VERSION);
		migrated.add("hotbar", savedHotbar.deepCopy());
		migrated.add("backpack", emptyBackpackJson());
		migrated.add("offhand", root.has("offhand") ? root.get("offhand").deepCopy() : JsonNull.INSTANCE);
		migrated.add("armor", version == ARMOR_DATA_VERSION && root.has("armor") ? root.get("armor").deepCopy() : emptyArmorJson());
		return migrated;
	}

	private LoadoutLayout decodeLayout(JsonObject root, RegistryOps<JsonElement> ops) throws LoadoutLayoutException {
		JsonObject currentRoot = root;
		if (currentRoot != null && currentRoot.get("version") != null && currentRoot.get("version").getAsInt() != DATA_VERSION) {
			currentRoot = migrateLegacyJson(currentRoot);
		}
		if (currentRoot == null || currentRoot.get("version") == null || currentRoot.get("version").getAsInt() != DATA_VERSION) {
			throw new LoadoutLayoutException("unsupported or missing layout version");
		}

		JsonArray savedHotbar = currentRoot.getAsJsonArray("hotbar");
		if (savedHotbar == null || savedHotbar.size() != LoadoutLayout.HOTBAR_SIZE) {
			throw new LoadoutLayoutException("layout must contain nine hotbar slots");
		}
		JsonArray savedBackpack = currentRoot.getAsJsonArray("backpack");
		if (savedBackpack == null || savedBackpack.size() != LoadoutLayout.BACKPACK_SIZE) {
			throw new LoadoutLayoutException("layout must contain 27 backpack slots");
		}

		ItemStack[] hotbar = new ItemStack[LoadoutLayout.HOTBAR_SIZE];
		for (int slot = 0; slot < LoadoutLayout.HOTBAR_SIZE; slot++) {
			hotbar[slot] = decodeStack(savedHotbar.get(slot), ops);
		}
		ItemStack[] backpack = new ItemStack[LoadoutLayout.BACKPACK_SIZE];
		for (int slot = 0; slot < LoadoutLayout.BACKPACK_SIZE; slot++) {
			backpack[slot] = decodeStack(savedBackpack.get(slot), ops);
		}

		return new LoadoutLayout(
				hotbar,
				backpack,
				decodeStack(currentRoot.get("offhand"), ops),
				decodeArmor(currentRoot.getAsJsonObject("armor"), ops)
		);
	}

	private static JsonObject encodeLayout(LoadoutLayout layout, RegistryOps<JsonElement> ops) throws LoadoutLayoutException {
		JsonObject root = new JsonObject();
		JsonArray hotbar = new JsonArray(LoadoutLayout.HOTBAR_SIZE);
		JsonArray backpack = new JsonArray(LoadoutLayout.BACKPACK_SIZE);

		root.addProperty("version", DATA_VERSION);
		for (int slot = 0; slot < LoadoutLayout.HOTBAR_SIZE; slot++) {
			hotbar.add(encodeStack(layout.hotbar()[slot], ops));
		}
		for (int slot = 0; slot < LoadoutLayout.BACKPACK_SIZE; slot++) {
			backpack.add(encodeStack(layout.backpack()[slot], ops));
		}
		root.add("hotbar", hotbar);
		root.add("backpack", backpack);
		root.add("offhand", encodeStack(layout.offhand(), ops));
		root.add("armor", encodeArmor(layout.armor(), ops));
		return root;
	}

	private static JsonObject encodeArmor(LoadoutLayout.Armor armor, RegistryOps<JsonElement> ops) throws LoadoutLayoutException {
		JsonObject json = new JsonObject();
		json.add("head", encodeStack(armor.head(), ops));
		json.add("chest", encodeStack(armor.chest(), ops));
		json.add("legs", encodeStack(armor.legs(), ops));
		json.add("feet", encodeStack(armor.feet(), ops));
		return json;
	}

	private static LoadoutLayout.Armor decodeArmor(JsonObject json, RegistryOps<JsonElement> ops) throws LoadoutLayoutException {
		if (json == null) {
			return LoadoutLayout.Armor.empty();
		}
		return new LoadoutLayout.Armor(
				decodeStack(json.get("head"), ops),
				decodeStack(json.get("chest"), ops),
				decodeStack(json.get("legs"), ops),
				decodeStack(json.get("feet"), ops)
		);
	}

	private static JsonObject emptyArmorJson() {
		JsonObject json = new JsonObject();
		json.add("head", JsonNull.INSTANCE);
		json.add("chest", JsonNull.INSTANCE);
		json.add("legs", JsonNull.INSTANCE);
		json.add("feet", JsonNull.INSTANCE);
		return json;
	}

	private static JsonArray emptyBackpackJson() {
		JsonArray json = new JsonArray(LoadoutLayout.BACKPACK_SIZE);
		for (int slot = 0; slot < LoadoutLayout.BACKPACK_SIZE; slot++) {
			json.add(JsonNull.INSTANCE);
		}
		return json;
	}

	private JsonObject readRoot(Path source) throws LoadoutLayoutException {
		try (Reader reader = Files.newBufferedReader(source)) {
			return GSON.fromJson(reader, JsonObject.class);
		} catch (IOException | JsonParseException exception) {
			throw new LoadoutLayoutException(exception.getMessage(), exception);
		}
	}

	private void write(JsonObject root) throws LoadoutLayoutException {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException exception) {
			throw new LoadoutLayoutException(exception.getMessage(), exception);
		}
	}

	private static JsonElement encodeStack(ItemStack stack, RegistryOps<JsonElement> ops) throws LoadoutLayoutException {
		if (stack.isEmpty()) {
			return JsonNull.INSTANCE;
		}

		ItemStack savedStack = stack.copy();
		savedStack.setCount(1);
		return ItemStack.CODEC.encodeStart(ops, savedStack)
				.result()
				.orElseThrow(() -> new LoadoutLayoutException("failed to encode item stack"));
	}

	private static ItemStack decodeStack(JsonElement element, RegistryOps<JsonElement> ops) throws LoadoutLayoutException {
		if (element == null || element.isJsonNull()) {
			return ItemStack.EMPTY;
		}

		return ItemStack.CODEC.parse(ops, element)
				.result()
				.orElseThrow(() -> new LoadoutLayoutException("failed to decode item stack"));
	}

	private static RegistryOps<JsonElement> registryOps(RegistryAccess registryAccess) {
		return registryAccess.createSerializationContext(JsonOps.INSTANCE);
	}
}
