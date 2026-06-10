package avn.spectrahud.client.gui;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.render.HudBounds;

public final class HudEditOverlayController {
	private static boolean active;
	private static boolean dragging;
	private static boolean resizing;
	private static double dragOffsetX;
	private static double dragOffsetY;

	private HudEditOverlayController() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void setActive(boolean value) {
		active = value;
		if (!active) {
			dragging = false;
			resizing = false;
		}
	}

	public static void toggle() {
		setActive(!active);
	}

	public static boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight) {
		if (!active || button != 0) {
			return false;
		}

		HudBounds bounds = bounds(screenWidth, screenHeight);
		if (bounds.containsResizeHandle(mouseX, mouseY)) {
			dragging = false;
			resizing = true;
			return true;
		}

		if (bounds.contains(mouseX, mouseY)) {
			dragging = true;
			resizing = false;
			dragOffsetX = mouseX - bounds.x();
			dragOffsetY = mouseY - bounds.y();
			return true;
		}

		return false;
	}

	public static boolean mouseDragged(double mouseX, double mouseY, int button, int screenWidth, int screenHeight) {
		if (!active || button != 0) {
			return false;
		}

		SpectraHudConfig config = SpectraHudClient.config();
		if (dragging) {
			HudBounds bounds = bounds(screenWidth, screenHeight);
			double maxX = Math.max(0.0D, screenWidth - bounds.width());
			double maxY = Math.max(0.0D, screenHeight - bounds.height());
			config.hudX = clamp(mouseX - dragOffsetX, 0.0D, maxX);
			config.hudY = clamp(mouseY - dragOffsetY, 0.0D, maxY);
			return true;
		}

		if (resizing) {
			HudBounds bounds = bounds(screenWidth, screenHeight);
			double logicalWidth = SpectraHudClient.getHudRenderer().getLogicalWidth();
			double logicalHeight = SpectraHudClient.getHudRenderer().getLogicalHeight();
			double scaleX = (mouseX - bounds.x()) / logicalWidth;
			double scaleY = (mouseY - bounds.y()) / logicalHeight;
			config.hudScale = clamp(Math.min(scaleX, scaleY), 0.65D, 2.0D);
			keepHudOnScreen(screenWidth, screenHeight);
			return true;
		}

		return false;
	}

	public static boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (!active || button != 0) {
			return false;
		}

		if (dragging || resizing) {
			dragging = false;
			resizing = false;
			SpectraHudClient.getConfigManager().save();
			return true;
		}

		return false;
	}

	public static boolean mouseScrolled(double mouseX, double mouseY, double amount, int screenWidth, int screenHeight) {
		if (!active) {
			return false;
		}

		HudBounds bounds = bounds(screenWidth, screenHeight);
		if (!bounds.contains(mouseX, mouseY)) {
			return false;
		}

		SpectraHudConfig config = SpectraHudClient.config();
		double oldScale = config.hudScale;
		double localX = (mouseX - bounds.x()) / oldScale;
		double localY = (mouseY - bounds.y()) / oldScale;
		config.hudScale = clamp(config.hudScale + amount * 0.06D, 0.65D, 2.0D);
		config.hudX = clamp(mouseX - localX * config.hudScale, 0.0D, Math.max(0.0D, screenWidth - SpectraHudClient.getHudRenderer().getLogicalWidth() * config.hudScale));
		config.hudY = clamp(mouseY - localY * config.hudScale, 0.0D, Math.max(0.0D, screenHeight - SpectraHudClient.getHudRenderer().getLogicalHeight() * config.hudScale));
		SpectraHudClient.getConfigManager().save();
		return true;
	}

	private static HudBounds bounds(int screenWidth, int screenHeight) {
		return SpectraHudClient.getHudRenderer().getBounds(screenWidth, screenHeight);
	}

	private static void keepHudOnScreen(int screenWidth, int screenHeight) {
		SpectraHudConfig config = SpectraHudClient.config();
		HudBounds bounds = bounds(screenWidth, screenHeight);
		config.hudX = clamp(bounds.x(), 0.0D, Math.max(0.0D, screenWidth - bounds.width()));
		config.hudY = clamp(bounds.y(), 0.0D, Math.max(0.0D, screenHeight - bounds.height()));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
