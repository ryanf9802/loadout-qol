package com.github.ryanf.loadoutmanager.client;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
	void sameSavedStackIgnoresEnchantments() {
		ItemStack actual = stack(Items.DIAMOND_SWORD);
		ItemStack desired = stack(Items.DIAMOND_SWORD);
		actual.set(DataComponents.ENCHANTMENTS, enchantments(1));
		desired.set(DataComponents.ENCHANTMENTS, enchantments(2));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
	}

	@Test
	void sameSavedStackIgnoresStoredEnchantments() {
		ItemStack actual = stack(Items.ENCHANTED_BOOK);
		ItemStack desired = stack(Items.ENCHANTED_BOOK);
		actual.set(DataComponents.STORED_ENCHANTMENTS, enchantments(1));
		desired.set(DataComponents.STORED_ENCHANTMENTS, enchantments(2));

		assertTrue(LoadoutLayoutApplier.sameSavedStack(actual, desired));
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
}
