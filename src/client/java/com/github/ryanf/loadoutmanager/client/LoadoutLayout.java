package com.github.ryanf.loadoutmanager.client;

import net.minecraft.world.item.ItemStack;

public record LoadoutLayout(ItemStack[] hotbar, ItemStack offhand, Armor armor) {
	public static final int HOTBAR_SIZE = 9;

	public LoadoutLayout {
		if (hotbar.length != HOTBAR_SIZE) {
			throw new IllegalArgumentException("Expected " + HOTBAR_SIZE + " hotbar slots, got " + hotbar.length);
		}
	}

	public record Armor(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
		public static Armor empty() {
			return new Armor(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
		}
	}
}
