package com.github.ryanf.loadoutmanager.client;

import com.github.ryanf.loadoutmanager.InventoryPlanner;
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

public final class LoadoutLayoutApplier {
	private final LoadoutLayoutStore store;
	private final Queue<ClickStep> pendingClicks = new ArrayDeque<>();
	private boolean applying;

	public LoadoutLayoutApplier(LoadoutLayoutStore store) {
		this.store = store;
	}

	public void start(Minecraft client) {
		if (applying) {
			message(client, "loadout_manager.apply.busy");
			return;
		}
		if (client.player == null || client.gameMode == null) {
			message(client, "loadout_manager.apply.no_player");
			return;
		}
		if (!store.exists()) {
			message(client, "loadout_manager.apply.no_layout");
			return;
		}
		if (client.screen != null && !(client.screen instanceof InventoryScreen)) {
			message(client, "loadout_manager.apply.screen");
			return;
		}
		if (!client.player.containerMenu.getCarried().isEmpty()) {
			message(client, "loadout_manager.apply.carried");
			return;
		}

		try {
			LoadoutLayout layout = store.load(client);
			plan(client.player, layout);
			applying = !pendingClicks.isEmpty();
		} catch (LoadoutLayoutException exception) {
			message(client, "loadout_manager.apply.invalid_layout", exception.getMessage());
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
			message(client, "loadout_manager.apply.screen");
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
			message(client, "loadout_manager.apply.carried");
			return;
		}

		if (pendingClicks.isEmpty()) {
			applying = false;
		}
	}

	private void plan(LocalPlayer player, LoadoutLayout layout) {
		pendingClicks.clear();
		List<InventoryPlanner.Target<ItemStack>> targets = new ArrayList<>(LoadoutLayout.HOTBAR_SIZE + 5);
		for (int hotbarSlot = 0; hotbarSlot < LoadoutLayout.HOTBAR_SIZE; hotbarSlot++) {
			targets.add(new InventoryPlanner.Target<>(InventoryPlanner.FIRST_HOTBAR_MENU_SLOT + hotbarSlot, layout.hotbar()[hotbarSlot]));
		}
		targets.add(new InventoryPlanner.Target<>(InventoryPlanner.ARMOR_HEAD_MENU_SLOT, layout.armor().head()));
		targets.add(new InventoryPlanner.Target<>(InventoryPlanner.ARMOR_CHEST_MENU_SLOT, layout.armor().chest()));
		targets.add(new InventoryPlanner.Target<>(InventoryPlanner.ARMOR_LEGS_MENU_SLOT, layout.armor().legs()));
		targets.add(new InventoryPlanner.Target<>(InventoryPlanner.ARMOR_FEET_MENU_SLOT, layout.armor().feet()));
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
		for (InventoryPlanner.Move move : result.moves()) {
			pendingClicks.add(new ClickStep(move.sourceMenuSlot()));
			pendingClicks.add(new ClickStep(move.targetMenuSlot()));
			pendingClicks.add(new ClickStep(move.sourceMenuSlot()));
		}
	}

	private static List<InventoryPlanner.Slot<ItemStack>> snapshot(LocalPlayer player) {
		List<InventoryPlanner.Slot<ItemStack>> states = new ArrayList<>();
		AbstractContainerMenu menu = player.containerMenu;
		addSnapshotSlot(states, menu, InventoryPlanner.ARMOR_HEAD_MENU_SLOT);
		addSnapshotSlot(states, menu, InventoryPlanner.ARMOR_CHEST_MENU_SLOT);
		addSnapshotSlot(states, menu, InventoryPlanner.ARMOR_LEGS_MENU_SLOT);
		addSnapshotSlot(states, menu, InventoryPlanner.ARMOR_FEET_MENU_SLOT);
		for (int slot = InventoryPlanner.FIRST_MAIN_MENU_SLOT; slot <= InventoryPlanner.OFFHAND_MENU_SLOT; slot++) {
			addSnapshotSlot(states, menu, slot);
		}
		return states;
	}

	private static void addSnapshotSlot(List<InventoryPlanner.Slot<ItemStack>> states, AbstractContainerMenu menu, int menuSlot) {
		if (menuSlot >= 0 && menuSlot < menu.slots.size()) {
			states.add(new InventoryPlanner.Slot<>(menuSlot, menu.slots.get(menuSlot).getItem().copy()));
		}
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
