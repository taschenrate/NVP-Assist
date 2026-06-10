package avn.spectrahud.client.input;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.gui.HudEditOverlayController;
import avn.spectrahud.client.gui.HudEditScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SpectraHudKeyBindings {
	private KeyBinding editHud;
	private KeyBinding toggleHud;
	private KeyBinding replayClip;
	private KeyBinding openSuspectMenu;

	public void register() {
		editHud = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.spectrahud.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "category.spectrahud"));
		toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.spectrahud.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.spectrahud"));
		replayClip = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.spectrahud.replay_clip", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.spectrahud"));
		openSuspectMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.spectrahud.open_suspect_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.spectrahud"));
	}

	public void tick(MinecraftClient client) {
		while (editHud.wasPressed()) {
			if (client.currentScreen instanceof ChatScreen) {
				HudEditOverlayController.toggle();
				if (client.player != null) {
					client.player.sendMessage(Text.literal("[НВП] Режим редактирования HUD: " + (HudEditOverlayController.isActive() ? "вкл" : "выкл")), true);
				}
			} else {
				HudEditOverlayController.setActive(false);
				client.setScreen(new HudEditScreen());
			}
		}

		while (toggleHud.wasPressed()) {
			SpectraHudClient.config().hudEnabled = !SpectraHudClient.config().hudEnabled;
			SpectraHudClient.getConfigManager().save();
			if (client.player != null) {
				client.player.sendMessage(Text.literal("[НВП] HUD: " + (SpectraHudClient.config().hudEnabled ? "вкл" : "выкл")), true);
			}
		}

		while (replayClip.wasPressed()) {
			SpectraHudClient.getReplayIntegration().saveManualClip(client);
		}

		while (openSuspectMenu.wasPressed()) {
			SpectraHudClient.getSuspectInteraction().openSuspectMenu(client, SpectraHudClient.getState());
		}
	}
}
