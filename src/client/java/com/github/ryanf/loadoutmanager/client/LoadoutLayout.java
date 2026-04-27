package com.github.ryanf.loadoutmanager.client;

import net.minecraft.world.item.ItemStack;

public record LoadoutLayout(ItemStack[] hotbar, ItemStack[] backpack, ItemStack offhand, Armor armor) {
	public static final int HOTBAR_SIZE = 9;
	public static final int BACKPACK_SIZE = 27;

	public LoadoutLayout {
		if (hotbar.length != HOTBAR_SIZE) {
			throw new IllegalArgumentException("Expected " + HOTBAR_SIZE + " hotbar slots, got " + hotbar.length);
		}
		if (backpack.length != BACKPACK_SIZE) {
			throw new IllegalArgumentException("Expected " + BACKPACK_SIZE + " backpack slots, got " + backpack.length);
		}
	}

	public record Armor(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
		public static Armor empty() {
			return new Armor(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
		}
	}
}
