package com.github.ryanf.loadoutmanager.client;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LoadoutLayoutApplierTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void sameSavedStackIgnoresDamage() {
		ItemStack actual = stack(Items.DIAMOND_SWORD);
		ItemStack desired = stack(Items.DIAMOND_SWORD);
		actual.set(DataComponents.DAMAGE, 12);
		desired.set(DataComponents.DAMAGE, 37);

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackRejectsDifferentEnchantments() {
		ItemStack actual = stack(Items.DIAMOND_SWORD);
		ItemStack desired = stack(Items.DIAMOND_SWORD);
		actual.set(DataComponents.ENCHANTMENTS, enchantments(1));
		desired.set(DataComponents.ENCHANTMENTS, enchantments(2));

		assertFalse(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackRejectsDifferentStoredEnchantments() {
		ItemStack actual = stack(Items.ENCHANTED_BOOK);
		ItemStack desired = stack(Items.ENCHANTED_BOOK);
		actual.set(DataComponents.STORED_ENCHANTMENTS, enchantments(1));
		desired.set(DataComponents.STORED_ENCHANTMENTS, enchantments(2));

		assertFalse(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresRepairCost() {
		ItemStack actual = stack(Items.DIAMOND_SWORD);
		ItemStack desired = stack(Items.DIAMOND_SWORD);
		actual.set(DataComponents.REPAIR_COST, 1);
		desired.set(DataComponents.REPAIR_COST, 7);

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresShulkerContents() {
		ItemStack actual = stack(Items.BLUE_SHULKER_BOX);
		ItemStack desired = stack(Items.BLUE_SHULKER_BOX);
		actual.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack(Items.DIAMOND))));
		desired.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack(Items.EMERALD))));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackRejectsDifferentShulkerColor() {
		ItemStack actual = stack(Items.BLUE_SHULKER_BOX);
		ItemStack desired = stack(Items.RED_SHULKER_BOX);
		actual.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack(Items.DIAMOND))));
		desired.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack(Items.DIAMOND))));

		assertFalse(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresBundleContents() {
		ItemStack actual = stack(Items.WHITE_BUNDLE);
		ItemStack desired = stack(Items.WHITE_BUNDLE);
		actual.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(stack(Items.DIAMOND)))));
		desired.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(stack(Items.EMERALD)))));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresContainerLoot() {
		ItemStack actual = stack(Items.CHEST);
		ItemStack desired = stack(Items.CHEST);
		actual.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootTable("alpha"), 1));
		desired.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootTable("beta"), 2));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresBees() {
		ItemStack actual = stack(Items.BEE_NEST);
		ItemStack desired = stack(Items.BEE_NEST);
		actual.set(DataComponents.BEES, Bees.EMPTY);

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresBucketEntityData() {
		ItemStack actual = stack(Items.AXOLOTL_BUCKET);
		ItemStack desired = stack(Items.AXOLOTL_BUCKET);
		actual.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(tag("variant", "blue")));
		desired.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(tag("variant", "gold")));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresEntityData() {
		ItemStack actual = stack(Items.PIG_SPAWN_EGG);
		ItemStack desired = stack(Items.PIG_SPAWN_EGG);
		actual.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityType.PIG, tag("name", "alpha")));
		desired.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityType.COW, tag("name", "beta")));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresBlockEntityData() {
		ItemStack actual = stack(Items.CHEST);
		ItemStack desired = stack(Items.CHEST);
		actual.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.CHEST, tag("lock", "alpha")));
		desired.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.SHULKER_BOX, tag("lock", "beta")));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackRejectsDifferentCustomName() {
		ItemStack actual = stack(Items.DIAMOND_SWORD);
		ItemStack desired = stack(Items.DIAMOND_SWORD);
		actual.set(DataComponents.CUSTOM_NAME, Component.literal("alpha"));
		desired.set(DataComponents.CUSTOM_NAME, Component.literal("beta"));

		assertFalse(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackRejectsDifferentItemType() {
		assertFalse(LoadoutLayoutApplier.sameSavedStack(
				stack(Items.DIAMOND_SWORD),
				stack(Items.STONE_SWORD)
		));
	}

	@Test
	void sameSavedStackRejectsEmptyActualStack() {
		assertFalse(LoadoutLayoutApplier.sameSavedStack(ItemStack.EMPTY, stack(Items.DIAMOND_SWORD)));
	}

	private static ItemEnchantments enchantments(int level) {
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(testEnchantment(), level);
		return enchantments.toImmutable();
	}

	private static Holder<Enchantment> testEnchantment() {
		return Holder.direct(new Enchantment(
				Component.literal("test"),
				Enchantment.definition(
						HolderSet.direct(holder(Items.DIAMOND_SWORD)),
						1,
						1,
						Enchantment.constantCost(1),
						Enchantment.constantCost(1),
						1,
						EquipmentSlotGroup.MAINHAND
				),
				HolderSet.empty(),
				DataComponentMap.EMPTY
		));
	}

	private static ItemStack stack(Item item) {
		return new ItemStack(holder(item), 1);
	}

	private static Holder.Reference<Item> holder(Item item) {
		Holder.Reference<Item> holder = item.builtInRegistryHolder();
		holder.bindComponents(DataComponentMap.EMPTY);
		return holder;
	}

	private static CompoundTag tag(String key, String value) {
		CompoundTag tag = new CompoundTag();
		tag.putString(key, value);
		return tag;
	}

	private static ResourceKey<LootTable> lootTable(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("loadout_manager_test", path));
	}
}
