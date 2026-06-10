package avn.spectrahud.client.config;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.input.SpectraHudKeyBindings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpectraHudKeyBindScreen extends Screen {
	private final Screen parent;
	private final Map<KeyBinding, ButtonWidget> keyButtons = new LinkedHashMap<>();
	private KeyBinding selectedBinding;
	private String selectedLabel = "";

	public SpectraHudKeyBindScreen(Screen parent) {
		super(Text.literal("Бинды NVP-assist"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		keyButtons.clear();
		int buttonWidth = Math.min(380, this.width - 40);
		int x = this.width / 2 - buttonWidth / 2;
		int y = 38;
		int row = 24;

		SpectraHudKeyBindings bindings = SpectraHudClient.getKeyBindings();
		addKeyButton(x, y, buttonWidth, "Редактировать HUD", bindings.getEditHud());
		y += row;
		addKeyButton(x, y, buttonWidth, "Включить/выключить HUD", bindings.getToggleHud());
		y += row;
		addKeyButton(x, y, buttonWidth, "ПКМ по подозреваемому", bindings.getOpenSuspectMenu());
		y += row;
		addKeyButton(x, y, buttonWidth, "Сохранить клип ReplayMod", bindings.getReplayClip());
		y += row + 8;

		addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить бинды NVP-assist"), button -> {
			resetBinding(bindings.getEditHud());
			resetBinding(bindings.getToggleHud());
			resetBinding(bindings.getOpenSuspectMenu());
			resetBinding(bindings.getReplayClip());
			saveKeyBindings();
			refreshKeyButtons();
		}).dimensions(x, y, buttonWidth, 20).build());
		y += row;

		addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close()).dimensions(x, y, buttonWidth, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		context.drawCenteredTextWithShadow(textRenderer, title, this.width / 2, 15, 0xFFE8EEEE);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (selectedBinding != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
				setBinding(selectedBinding, InputUtil.UNKNOWN_KEY);
			} else {
				setBinding(selectedBinding, InputUtil.fromKeyCode(keyCode, scanCode));
			}
			selectedBinding = null;
			selectedLabel = "";
			refreshKeyButtons();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (selectedBinding != null) {
			setBinding(selectedBinding, InputUtil.Type.MOUSE.createFromCode(button));
			selectedBinding = null;
			selectedLabel = "";
			refreshKeyButtons();
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	private void addKeyButton(int x, int y, int width, String label, KeyBinding binding) {
		if (binding == null) {
			return;
		}

		ButtonWidget button = ButtonWidget.builder(keyText(label, binding), pressed -> {
			selectedBinding = binding;
			selectedLabel = label;
			pressed.setMessage(Text.literal(label + ": нажмите клавишу"));
		}).dimensions(x, y, width, 20).build();
		keyButtons.put(binding, button);
		addDrawableChild(button);
	}

	private void setBinding(KeyBinding binding, InputUtil.Key key) {
		if (client == null || binding == null) {
			return;
		}

		client.options.setKeyCode(binding, key);
		saveKeyBindings();
	}

	private void resetBinding(KeyBinding binding) {
		if (client != null && binding != null) {
			client.options.setKeyCode(binding, binding.getDefaultKey());
		}
	}

	private void saveKeyBindings() {
		if (client != null) {
			KeyBinding.updateKeysByCode();
			client.options.write();
		}
	}

	private void refreshKeyButtons() {
		keyButtons.forEach((binding, button) -> {
			String label = button == null || binding != selectedBinding ? labelFor(binding) : selectedLabel;
			button.setMessage(keyText(label, binding));
		});
	}

	private String labelFor(KeyBinding binding) {
		SpectraHudKeyBindings bindings = SpectraHudClient.getKeyBindings();
		if (binding == bindings.getEditHud()) {
			return "Редактировать HUD";
		}
		if (binding == bindings.getToggleHud()) {
			return "Включить/выключить HUD";
		}
		if (binding == bindings.getOpenSuspectMenu()) {
			return "ПКМ по подозреваемому";
		}
		if (binding == bindings.getReplayClip()) {
			return "Сохранить клип ReplayMod";
		}
		return binding.getTranslationKey();
	}

	private Text keyText(String label, KeyBinding binding) {
		return Text.literal(label + ": ").append(binding.getBoundKeyLocalizedText());
	}
}
