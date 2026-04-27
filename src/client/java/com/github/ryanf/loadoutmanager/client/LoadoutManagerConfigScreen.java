package com.github.ryanf.loadoutmanager.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class LoadoutManagerConfigScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int LABEL_WIDTH = 170;
	private static final int ROW_WIDTH = LABEL_WIDTH + BUTTON_WIDTH + 8;

	private final Screen parent;
	private Button keyButton;
	private boolean capturingKey;

	public LoadoutManagerConfigScreen(Screen parent) {
		super(Component.translatable("loadout_manager.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = (width - ROW_WIDTH) / 2;
		int rowY = height / 2 - 20;
		int footerY = height / 2 + 30;

		keyButton = addRenderableWidget(Button.builder(keyButtonMessage(), button -> {
					capturingKey = true;
					updateKeyButton();
				})
				.bounds(left + LABEL_WIDTH + 8, rowY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());

		addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> {
					applyKey(LoadoutManagerClient.applyKey().getDefaultKey());
					capturingKey = false;
					updateKeyButton();
				})
				.bounds(width / 2 - 154, footerY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(width / 2 + 4, footerY, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
		super.extractRenderState(graphics, mouseX, mouseY, tickProgress);

		int left = (width - ROW_WIDTH) / 2;
		int rowY = height / 2 - 20;
		graphics.centeredText(font, title, width / 2, 20, 0xFFFFFF);
		graphics.text(font, Component.translatable("key.loadout_manager.apply"), left, rowY + 6, 0xFFFFFF);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (capturingKey) {
			if (event.key() != InputConstants.KEY_ESCAPE) {
				applyKey(InputConstants.getKey(event));
			}

			capturingKey = false;
			updateKeyButton();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (capturingKey && (keyButton == null || !keyButton.isMouseOver(event.x(), event.y()))) {
			applyKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			capturingKey = false;
			updateKeyButton();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private void applyKey(InputConstants.Key key) {
		LoadoutManagerClient.applyKey().setKey(key);
		KeyMapping.resetMapping();
		Minecraft.getInstance().options.save();
	}

	private Component keyButtonMessage() {
		if (capturingKey) {
			return Component.translatable("loadout_manager.settings.apply_key.capturing");
		}

		return LoadoutManagerClient.applyKey().getTranslatedKeyMessage();
	}

	private void updateKeyButton() {
		if (keyButton != null) {
			keyButton.setMessage(keyButtonMessage());
		}
	}
}
