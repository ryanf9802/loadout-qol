package com.github.ryanf.loadoutmanager.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class LoadoutManagerClient implements ClientModInitializer {
	public static final String MOD_ID = "loadout_manager";

	private LoadoutLayoutStore store;
	private LoadoutLayoutApplier applier;
	private KeyMapping applyKey;

	@Override
	public void onInitializeClient() {
		store = new LoadoutLayoutStore();
		applier = new LoadoutLayoutApplier(store);

		applyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.loadout_manager.apply",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "loadout_manager"))
		));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("loadoutmanager")
					.then(ClientCommands.literal("save").executes(context -> saveLayout(context.getSource()))));
			dispatcher.register(ClientCommands.literal("hotbarqol")
					.then(ClientCommands.literal("save").executes(context -> saveLayout(context.getSource()))));
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (applyKey.consumeClick()) {
				applier.start(client);
			}

			applier.tick(client);
		});
	}

	private int saveLayout(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
		try {
			store.saveCurrentLayout(Minecraft.getInstance());
			source.sendFeedback(Component.translatable("loadout_manager.command.save.success"));
			return 1;
		} catch (LoadoutLayoutException exception) {
			source.sendFeedback(Component.translatable("loadout_manager.command.save.error", exception.getMessage()));
			return 0;
		}
	}
}
