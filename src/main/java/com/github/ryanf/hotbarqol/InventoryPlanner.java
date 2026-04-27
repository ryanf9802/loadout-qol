package com.github.ryanf.hotbarqol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InventoryPlanner<T> {
	public static final int FIRST_MAIN_MENU_SLOT = 9;
	public static final int LAST_MAIN_MENU_SLOT = 35;
	public static final int FIRST_HOTBAR_MENU_SLOT = 36;
	public static final int OFFHAND_MENU_SLOT = 45;

	private final StackRules<T> stackRules;

	public InventoryPlanner(StackRules<T> stackRules) {
		this.stackRules = stackRules;
	}

	public Result plan(List<Slot<T>> inventory, List<Target<T>> targets) {
		List<MutableSlot<T>> states = new ArrayList<>();
		for (Slot<T> slot : inventory) {
			states.add(new MutableSlot<>(slot.menuSlot(), slot.stack()));
		}

		Set<Integer> lockedTargets = new HashSet<>();
		List<Move> moves = new ArrayList<>();
		int skippedSlots = 0;

		for (Target<T> target : targets) {
			MutableSlot<T> targetState = findState(states, target.menuSlot());
			if (targetState == null) {
				skippedSlots++;
				continue;
			}

			if (stackRules.isEmpty(target.desired())) {
				if (!stackRules.isEmpty(targetState.stack) && !moveToEmptySlot(states, lockedTargets, targetState, moves)) {
					skippedSlots++;
				}
				lockedTargets.add(target.menuSlot());
				continue;
			}

			if (stackRules.matches(targetState.stack, target.desired())) {
				lockedTargets.add(target.menuSlot());
				continue;
			}

			MutableSlot<T> source = findMatchingSource(states, lockedTargets, target.menuSlot(), target.desired());
			if (source == null) {
				if (!stackRules.isEmpty(targetState.stack) && !moveToEmptySlot(states, lockedTargets, targetState, moves)) {
					skippedSlots++;
				}
				lockedTargets.add(target.menuSlot());
				continue;
			}

			swap(source, targetState, moves);
			lockedTargets.add(target.menuSlot());
		}

		return new Result(List.copyOf(moves), skippedSlots);
	}

	private boolean moveToEmptySlot(List<MutableSlot<T>> states, Set<Integer> lockedTargets, MutableSlot<T> source, List<Move> moves) {
		MutableSlot<T> empty = firstEmptyMainSlot(states, lockedTargets, source.menuSlot);
		if (empty == null) {
			return false;
		}
		swap(source, empty, moves);
		return true;
	}

	private void swap(MutableSlot<T> source, MutableSlot<T> target, List<Move> moves) {
		if (source.menuSlot == target.menuSlot) {
			return;
		}

		moves.add(new Move(source.menuSlot, target.menuSlot));
		T sourceStack = source.stack;
		source.stack = target.stack;
		target.stack = sourceStack;
	}

	private MutableSlot<T> findMatchingSource(List<MutableSlot<T>> states, Set<Integer> lockedTargets, int targetMenuSlot, T desired) {
		for (MutableSlot<T> state : states) {
			if (state.menuSlot == targetMenuSlot || lockedTargets.contains(state.menuSlot)) {
				continue;
			}
			if (stackRules.matches(state.stack, desired)) {
				return state;
			}
		}
		return null;
	}

	private MutableSlot<T> firstEmptyMainSlot(List<MutableSlot<T>> states, Set<Integer> lockedTargets, int sourceMenuSlot) {
		for (MutableSlot<T> state : states) {
			if (state.menuSlot == sourceMenuSlot || lockedTargets.contains(state.menuSlot)) {
				continue;
			}
			if (state.menuSlot >= FIRST_MAIN_MENU_SLOT && state.menuSlot <= LAST_MAIN_MENU_SLOT && stackRules.isEmpty(state.stack)) {
				return state;
			}
		}
		return null;
	}

	private static <T> MutableSlot<T> findState(List<MutableSlot<T>> states, int menuSlot) {
		for (MutableSlot<T> state : states) {
			if (state.menuSlot == menuSlot) {
				return state;
			}
		}
		return null;
	}

	public interface StackRules<T> {
		boolean isEmpty(T stack);

		boolean matches(T actual, T desired);
	}

	public record Slot<T>(int menuSlot, T stack) {
	}

	public record Target<T>(int menuSlot, T desired) {
	}

	public record Move(int sourceMenuSlot, int targetMenuSlot) {
	}

	public record Result(List<Move> moves, int skippedSlots) {
	}

	private static final class MutableSlot<T> {
		private final int menuSlot;
		private T stack;

		private MutableSlot(int menuSlot, T stack) {
			this.menuSlot = menuSlot;
			this.stack = stack;
		}
	}
}
