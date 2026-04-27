package com.github.ryanf.loadoutmanager;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InventoryPlannerTest {
	private final InventoryPlanner<String> planner = new InventoryPlanner<>(new InventoryPlanner.StackRules<>() {
		@Override
		public boolean isEmpty(String stack) {
			return stack == null;
		}

		@Override
		public boolean matches(String actual, String desired) {
			return desired != null && desired.equals(actual);
		}
	});

	@Test
	void exactIdentityDoesNotMatchDifferentComponents() {
		TestInventory inventory = inventory(
				slot(9, "pickaxe:plain"),
				slot(36, "pickaxe:enchanted"),
				slot(37, null)
		);

		apply(inventory, target(36, "pickaxe:plain"));

		assertEquals("pickaxe:plain", inventory.stackAt(36));
		assertEquals("pickaxe:enchanted", inventory.stackAt(9));
	}

	@Test
	void missingDesiredItemClearsTargetWhenMainSpaceExists() {
		TestInventory inventory = inventory(slot(9, null), slot(36, "dirt"));

		InventoryPlanner.Result result = apply(inventory, target(36, "diamond_sword"));

		assertEquals(null, inventory.stackAt(36));
		assertEquals("dirt", inventory.stackAt(9));
		assertEquals(0, result.skippedSlots());
	}

	@Test
	void emptySavedSlotClearsTargetWhenMainSpaceExists() {
		TestInventory inventory = inventory(slot(9, null), slot(36, "torch"));

		apply(inventory, target(36, null));

		assertEquals(null, inventory.stackAt(36));
		assertEquals("torch", inventory.stackAt(9));
	}

	@Test
	void fullInventoryLeavesTargetInPlaceAndSkips() {
		TestInventory inventory = inventory(slot(9, "stone"), slot(36, "torch"));

		InventoryPlanner.Result result = apply(inventory, target(36, null));

		assertEquals("torch", inventory.stackAt(36));
		assertEquals(1, result.skippedSlots());
	}

	@Test
	void offhandCanBeTargeted() {
		TestInventory inventory = inventory(slot(9, "shield"), slot(45, null));

		apply(inventory, target(45, "shield"));

		assertEquals("shield", inventory.stackAt(45));
		assertEquals(null, inventory.stackAt(9));
	}

	@Test
	void armorCanBeTargeted() {
		TestInventory inventory = inventory(slot(9, "helmet"), slot(5, null));

		apply(inventory, target(5, "helmet"));

		assertEquals("helmet", inventory.stackAt(5));
		assertEquals(null, inventory.stackAt(9));
	}

	@Test
	void armorCanBeUsedAsSource() {
		TestInventory inventory = inventory(slot(5, "helmet"), slot(9, null), slot(36, null));

		apply(inventory, target(36, "helmet"));

		assertEquals(null, inventory.stackAt(5));
		assertEquals("helmet", inventory.stackAt(36));
	}

	@Test
	void emptySavedArmorSlotClearsArmorWhenMainSpaceExists() {
		TestInventory inventory = inventory(slot(5, "helmet"), slot(9, null));

		apply(inventory, target(5, null));

		assertEquals(null, inventory.stackAt(5));
		assertEquals("helmet", inventory.stackAt(9));
	}

	@Test
	void lockedCorrectSlotIsNotUsedForDuplicateLaterTarget() {
		TestInventory inventory = inventory(slot(9, null), slot(36, "torch"), slot(37, null));

		InventoryPlanner.Result result = apply(inventory, target(36, "torch"), target(37, "torch"));

		assertEquals("torch", inventory.stackAt(36));
		assertEquals(null, inventory.stackAt(37));
		assertEquals(0, result.skippedSlots());
	}

	@Test
	void backpackCanBeTargeted() {
		TestInventory inventory = inventory(slot(9, "cobblestone"), slot(10, "apple"));

		apply(inventory, target(9, "apple"));

		assertEquals("apple", inventory.stackAt(9));
		assertEquals("cobblestone", inventory.stackAt(10));
	}

	@Test
	void unmentionedBackpackSlotIsLeftFlexible() {
		TestInventory inventory = inventory(slot(9, "cobblestone"), slot(36, "sword"));

		apply(inventory, target(36, "sword"));

		assertEquals("cobblestone", inventory.stackAt(9));
		assertEquals("sword", inventory.stackAt(36));
	}

	@SafeVarargs
	private final InventoryPlanner.Result apply(TestInventory inventory, InventoryPlanner.Target<String>... targets) {
		InventoryPlanner.Result result = planner.plan(inventory.toSlots(), List.of(targets));
		for (InventoryPlanner.Move move : result.moves()) {
			inventory.swap(move.sourceMenuSlot(), move.targetMenuSlot());
		}
		return result;
	}

	private static TestInventory inventory(TestSlot... slots) {
		return new TestInventory(new ArrayList<>(List.of(slots)));
	}

	private static InventoryPlanner.Target<String> target(int menuSlot, String stack) {
		return new InventoryPlanner.Target<>(menuSlot, stack);
	}

	private static TestSlot slot(int menuSlot, String stack) {
		return new TestSlot(menuSlot, stack);
	}

	private record TestSlot(int menuSlot, String stack) {
	}

	private static final class TestInventory {
		private final List<MutableSlot> slots;

		private TestInventory(List<TestSlot> slots) {
			this.slots = new ArrayList<>();
			for (TestSlot slot : slots) {
				this.slots.add(new MutableSlot(slot.menuSlot(), slot.stack()));
			}
		}

		private List<InventoryPlanner.Slot<String>> toSlots() {
			List<InventoryPlanner.Slot<String>> result = new ArrayList<>();
			for (MutableSlot slot : slots) {
				result.add(new InventoryPlanner.Slot<>(slot.menuSlot, slot.stack));
			}
			return result;
		}

		private String stackAt(int menuSlot) {
			return slot(menuSlot).stack;
		}

		private void swap(int firstMenuSlot, int secondMenuSlot) {
			MutableSlot first = slot(firstMenuSlot);
			MutableSlot second = slot(secondMenuSlot);
			String stack = first.stack;
			first.stack = second.stack;
			second.stack = stack;
		}

		private MutableSlot slot(int menuSlot) {
			return slots.stream().filter(slot -> slot.menuSlot == menuSlot).findFirst().orElseThrow();
		}
	}

	private static final class MutableSlot {
		private final int menuSlot;
		private String stack;

		private MutableSlot(int menuSlot, String stack) {
			this.menuSlot = menuSlot;
			this.stack = stack;
		}
	}
}
