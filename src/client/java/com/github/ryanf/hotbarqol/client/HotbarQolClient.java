package com.github.ryanf.hotbarqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HotbarQolClient implements ClientModInitializer {
	public static final String MOD_ID = "hotbar_qol";

	private HotbarLayoutStore store;
	private HotbarLayoutApplier applier;
	private KeyMapping applyKey;

	@Override
	public void onInitializeClient() {
		store = new HotbarLayoutStore();
		applier = new HotbarLayoutApplier(store);

		applyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hotbar_qol.apply",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "hotbar_qol"))
		));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("hotbarqol")
						.then(ClientCommands.literal("save").executes(context -> {
							try {
								store.saveCurrentLayout(Minecraft.getInstance());
								context.getSource().sendFeedback(Component.translatable("hotbar_qol.command.save.success"));
								return 1;
							} catch (HotbarLayoutException exception) {
								context.getSource().sendFeedback(Component.translatable("hotbar_qol.command.save.error", exception.getMessage()));
								return 0;
							}
						}))
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (applyKey.consumeClick()) {
				applier.start(client);
			}

			applier.tick(client);
		});
	}
}
