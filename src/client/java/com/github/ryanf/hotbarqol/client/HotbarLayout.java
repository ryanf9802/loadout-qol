package com.github.ryanf.hotbarqol.client;

import net.minecraft.world.item.ItemStack;

public record HotbarLayout(ItemStack[] hotbar, ItemStack offhand) {
	public static final int HOTBAR_SIZE = 9;

	public HotbarLayout {
		if (hotbar.length != HOTBAR_SIZE) {
			throw new IllegalArgumentException("Expected " + HOTBAR_SIZE + " hotbar slots, got " + hotbar.length);
		}
	}
}
