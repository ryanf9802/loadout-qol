package com.github.ryanf.hotbarqol.client;

import com.github.ryanf.hotbarqol.InventoryPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class HotbarLayoutApplier {
	private final HotbarLayoutStore store;
	private final Queue<ClickStep> pendingClicks = new ArrayDeque<>();
	private boolean applying;
	private int skippedSlots;

	public HotbarLayoutApplier(HotbarLayoutStore store) {
		this.store = store;
	}

	public void start(Minecraft client) {
		if (applying) {
			message(client, "hotbar_qol.apply.busy");
			return;
		}
		if (client.player == null || client.gameMode == null) {
			message(client, "hotbar_qol.apply.no_player");
			return;
		}
		if (!store.exists()) {
			message(client, "hotbar_qol.apply.no_layout");
			return;
		}
		if (client.screen != null && !(client.screen instanceof InventoryScreen)) {
			message(client, "hotbar_qol.apply.screen");
			return;
		}
		if (!client.player.containerMenu.getCarried().isEmpty()) {
			message(client, "hotbar_qol.apply.carried");
			return;
		}

		try {
			HotbarLayout layout = store.load(client);
			plan(client.player, layout);
			if (pendingClicks.isEmpty()) {
				message(client, "hotbar_qol.apply.success");
				return;
			}
			applying = true;
		} catch (HotbarLayoutException exception) {
			message(client, "hotbar_qol.apply.invalid_layout", exception.getMessage());
		}
	}

	public void tick(Minecraft client) {
		if (!applying) {
			return;
		}
		if (client.player == null || client.gameMode == null) {
			pendingClicks.clear();
			applying = false;
			return;
		}
		if (client.screen != null && !(client.screen instanceof InventoryScreen)) {
			pendingClicks.clear();
			applying = false;
			message(client, "hotbar_qol.apply.screen");
			return;
		}

		AbstractContainerMenu menu = client.player.containerMenu;
		for (int clickCount = 0; clickCount < 3 && !pendingClicks.isEmpty(); clickCount++) {
			ClickStep click = pendingClicks.poll();
			client.gameMode.handleContainerInput(menu.containerId, click.menuSlot(), 0, ContainerInput.PICKUP, client.player);
		}

		if (!client.player.containerMenu.getCarried().isEmpty()) {
			pendingClicks.clear();
			applying = false;
			message(client, "hotbar_qol.apply.carried");
			return;
		}

		if (pendingClicks.isEmpty()) {
			applying = false;
			if (skippedSlots == 0) {
				message(client, "hotbar_qol.apply.success");
			} else {
				message(client, "hotbar_qol.apply.partial", skippedSlots);
			}
		}
	}

	private void plan(LocalPlayer player, HotbarLayout layout) {
		pendingClicks.clear();
		List<InventoryPlanner.Target<ItemStack>> targets = new ArrayList<>(HotbarLayout.HOTBAR_SIZE + 1);
		for (int hotbarSlot = 0; hotbarSlot < HotbarLayout.HOTBAR_SIZE; hotbarSlot++) {
			targets.add(new InventoryPlanner.Target<>(InventoryPlanner.FIRST_HOTBAR_MENU_SLOT + hotbarSlot, layout.hotbar()[hotbarSlot]));
		}
		targets.add(new InventoryPlanner.Target<>(InventoryPlanner.OFFHAND_MENU_SLOT, layout.offhand()));

		InventoryPlanner<ItemStack> planner = new InventoryPlanner<>(new InventoryPlanner.StackRules<>() {
			@Override
			public boolean isEmpty(ItemStack stack) {
				return stack.isEmpty();
			}

			@Override
			public boolean matches(ItemStack actual, ItemStack desired) {
				return sameSavedStack(actual, desired);
			}
		});

		InventoryPlanner.Result result = planner.plan(snapshot(player), targets);
		skippedSlots = result.skippedSlots();

		for (InventoryPlanner.Move move : result.moves()) {
			pendingClicks.add(new ClickStep(move.sourceMenuSlot()));
			pendingClicks.add(new ClickStep(move.targetMenuSlot()));
			pendingClicks.add(new ClickStep(move.sourceMenuSlot()));
		}
	}

	private static List<InventoryPlanner.Slot<ItemStack>> snapshot(LocalPlayer player) {
		List<InventoryPlanner.Slot<ItemStack>> states = new ArrayList<>();
		AbstractContainerMenu menu = player.containerMenu;
		for (int slot = InventoryPlanner.FIRST_MAIN_MENU_SLOT; slot <= InventoryPlanner.OFFHAND_MENU_SLOT; slot++) {
			if (slot >= 0 && slot < menu.slots.size()) {
				states.add(new InventoryPlanner.Slot<>(slot, menu.slots.get(slot).getItem().copy()));
			}
		}
		return states;
	}

	private static boolean sameSavedStack(ItemStack actual, ItemStack desired) {
		return !actual.isEmpty() && ItemStack.isSameItemSameComponents(actual, desired);
	}

	private static void message(Minecraft client, String translationKey, Object... args) {
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.translatable(translationKey, args));
		}
	}

	private record ClickStep(int menuSlot) {
	}
}
