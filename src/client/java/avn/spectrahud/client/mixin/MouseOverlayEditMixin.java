package avn.spectrahud.client.mixin;

import avn.spectrahud.client.gui.HudEditOverlayController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseOverlayEditMixin {
	@Shadow
	private double x;

	@Shadow
	private double y;

	@Inject(method = "onMouseButton", at = @At("HEAD"))
	private void spectrahud$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!(client.currentScreen instanceof ChatScreen) || !HudEditOverlayController.isActive()) {
			return;
		}

		double mouseX = scaledX(client, x);
		double mouseY = scaledY(client, y);
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		if (action == GLFW.GLFW_PRESS) {
			HudEditOverlayController.mouseClicked(mouseX, mouseY, button, screenWidth, screenHeight);
		} else if (action == GLFW.GLFW_RELEASE) {
			HudEditOverlayController.mouseReleased(mouseX, mouseY, button);
		}
	}

	@Inject(method = "onCursorPos", at = @At("HEAD"))
	private void spectrahud$onCursorPos(long window, double x, double y, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!(client.currentScreen instanceof ChatScreen) || !HudEditOverlayController.isActive()) {
			return;
		}

		double mouseX = scaledX(client, x);
		double mouseY = scaledY(client, y);
		HudEditOverlayController.mouseDragged(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
	}

	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void spectrahud$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!(client.currentScreen instanceof ChatScreen) || !HudEditOverlayController.isActive()) {
			return;
		}

		double mouseX = scaledX(client, x);
		double mouseY = scaledY(client, y);
		if (HudEditOverlayController.mouseScrolled(mouseX, mouseY, vertical, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())) {
			ci.cancel();
		}
	}

	private static double scaledX(MinecraftClient client, double rawX) {
		Window window = client.getWindow();
		return rawX * window.getScaledWidth() / window.getWidth();
	}

	private static double scaledY(MinecraftClient client, double rawY) {
		Window window = client.getWindow();
		return rawY * window.getScaledHeight() / window.getHeight();
	}
}
