package avn.spectrahud.client.config;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.gui.HudEditOverlayController;
import avn.spectrahud.client.gui.HudEditScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SpectraHudConfigScreen extends Screen {
	private static final int[] SUSPECT_PRESETS = {0xFFFF4B43, 0xFFFF7A2F, 0xFFFF335F};

	private final Screen parent;
	private final ConfigManager manager;
	private final SpectraHudConfig config;

	public SpectraHudConfigScreen(Screen parent) {
		super(Text.literal("Настройки NVP-assist"));
		this.parent = parent;
		this.manager = SpectraHudClient.getConfigManager();
		this.config = manager.get();
	}

	@Override
	protected void init() {
		int width = Math.min(380, this.width - 40);
		int x = this.width / 2 - width / 2;
		int y = 34;
		int row = 24;

		addToggle(x, y, width, "HUD", () -> config.hudEnabled, value -> config.hudEnabled = value);
		y += row;

		addDrawableChild(ButtonWidget.builder(modeText(), button -> {
			config.hudMode = config.hudMode.next();
			button.setMessage(modeText());
			manager.save();
		}).dimensions(x, y, width, 20).build());

		y += row;
		addToggle(x, y, width, "Подсветка подозреваемого", () -> config.suspectOutline, value -> config.suspectOutline = value);

		y += row;
		addToggle(x, y, width, "Луч взгляда", () -> config.showViewRay, value -> config.showViewRay = value);

		y += row;
		addHologramButton(x, y, width);

		y += row;
		addToggle(x, y, width, "Полоса задержки", () -> config.showHitDelayStrip, value -> config.showHitDelayStrip = value);

		y += row;
		addToggle(x, y, width, "ReplayMod-клипы", () -> config.replayClips, value -> config.replayClips = value);

		y += row;
		addToggle(x, y, width, "Авто-TPO к подозреваемому", () -> config.autoTeleportToSuspect, value -> config.autoTeleportToSuspect = value);

		y += row;
		addColorButton(x, y, width, "Цвет подозреваемого", SUSPECT_PRESETS, () -> config.suspectColor, color -> config.suspectColor = color);

		y += row;
		addDrawableChild(new OpacitySlider(x, y, width, 20, config));

		y += row;
		addDrawableChild(ButtonWidget.builder(Text.literal("Сброс позиции HUD"), button -> {
			config.resetHudPosition();
			manager.save();
		}).dimensions(x, y, width, 20).build());

		y += row;
		addDrawableChild(ButtonWidget.builder(Text.literal("Сброс масштаба HUD"), button -> {
			config.resetHudScale();
			manager.save();
		}).dimensions(x, y, width, 20).build());

		y += row;
		addDrawableChild(ButtonWidget.builder(Text.literal("Редактировать HUD"), button -> {
			HudEditOverlayController.setActive(false);
			manager.save();
			if (client != null) {
				client.setScreen(new HudEditScreen());
			}
		}).dimensions(x, y, width, 20).build());

		y += row;
		addDrawableChild(ButtonWidget.builder(Text.literal("Бинды NVP-assist"), button -> {
			manager.save();
			if (client != null) {
				client.setScreen(new SpectraHudKeyBindScreen(this));
			}
		}).dimensions(x, y, width, 20).build());

		y += row + 8;
		addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close()).dimensions(x, y, width, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		context.drawCenteredTextWithShadow(textRenderer, title, this.width / 2, 15, 0xFFE8EEEE);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		manager.save();
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public void removed() {
		manager.save();
	}

	private void addToggle(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
		addDrawableChild(ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), button -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(toggleText(label, getter.getAsBoolean()));
			manager.save();
		}).dimensions(x, y, width, 20).build());
	}

	private void addHologramButton(int x, int y, int width) {
		addDrawableChild(ButtonWidget.builder(hologramText(), button -> {
			config.hideAnticheatHologram = !config.hideAnticheatHologram;
			button.setMessage(hologramText());
			manager.save();
		}).dimensions(x, y, width, 20).build());
	}

	private void addColorButton(int x, int y, int width, String label, int[] presets, IntSupplier getter, IntConsumer setter) {
		addDrawableChild(ButtonWidget.builder(colorText(label, getter.getAsInt()), button -> {
			int current = getter.getAsInt();
			int next = presets[0];
			for (int index = 0; index < presets.length; index++) {
				if (presets[index] == current) {
					next = presets[(index + 1) % presets.length];
					break;
				}
			}
			setter.accept(next);
			button.setMessage(colorText(label, getter.getAsInt()));
			manager.save();
		}).dimensions(x, y, width, 20).build());
	}

	private Text modeText() {
		return Text.literal("Режим HUD: " + config.hudMode.title());
	}

	private Text hologramText() {
		return Text.literal("Голограмма античита: " + (config.hideAnticheatHologram ? "скрывать" : "показывать"));
	}

	private static Text toggleText(String label, boolean enabled) {
		return Text.literal(label + ": " + (enabled ? "вкл" : "выкл"));
	}

	private static Text colorText(String label, int color) {
		return Text.literal(label + ": " + toHex(color));
	}

	private static String toHex(int argb) {
		return String.format("#%06X", argb & 0xFFFFFF);
	}

	@FunctionalInterface
	private interface IntSupplier {
		int getAsInt();
	}

	@FunctionalInterface
	private interface IntConsumer {
		void accept(int value);
	}

	private class OpacitySlider extends SliderWidget {
		OpacitySlider(int x, int y, int width, int height, SpectraHudConfig config) {
			super(x, y, width, height, Text.empty(), (config.hudOpacity - 0.25D) / 0.75D);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal("Прозрачность HUD: " + Math.round(SpectraHudConfigScreen.this.config.hudOpacity * 100.0D) + "%"));
		}

		@Override
		protected void applyValue() {
			SpectraHudConfigScreen.this.config.hudOpacity = 0.25D + value * 0.75D;
			manager.save();
		}
	}
}
