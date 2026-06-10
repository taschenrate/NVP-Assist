package avn.spectrahud.client.gui;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.render.HudBounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {
	private boolean dragging;
	private boolean resizing;
	private double dragOffsetX;
	private double dragOffsetY;

	public HudEditScreen() {
		super(Text.literal("Редактирование HUD"));
	}

	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> changeScale(-0.10D)).dimensions(this.width / 2 - 214, this.height - 28, 28, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> changeScale(0.10D)).dimensions(this.width / 2 - 182, this.height - 28, 28, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Сброс позиции"), button -> {
			SpectraHudClient.config().resetHudPosition();
			SpectraHudClient.getConfigManager().save();
		}).dimensions(this.width / 2 - 154, this.height - 28, 100, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Сброс масштаба"), button -> {
			SpectraHudClient.config().resetHudScale();
			SpectraHudClient.getConfigManager().save();
		}).dimensions(this.width / 2 - 50, this.height - 28, 100, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close()).dimensions(this.width / 2 + 54, this.height - 28, 100, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		SpectraHudClient.getHudRenderer().renderHud(context, true);
		context.drawCenteredTextWithShadow(textRenderer, title, this.width / 2, 10, 0xFFE8EEEE);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			HudBounds bounds = currentBounds();
			if (bounds.containsResizeHandle(mouseX, mouseY)) {
				resizing = true;
				return true;
			}
			if (bounds.contains(mouseX, mouseY)) {
				dragging = true;
				dragOffsetX = mouseX - bounds.x();
				dragOffsetY = mouseY - bounds.y();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (button == 0 && dragging) {
			SpectraHudConfig config = SpectraHudClient.config();
			HudBounds bounds = currentBounds();
			double maxX = Math.max(0.0D, this.width - bounds.width());
			double maxY = Math.max(0.0D, this.height - bounds.height());
			config.hudX = clamp(mouseX - dragOffsetX, 0.0D, maxX);
			config.hudY = clamp(mouseY - dragOffsetY, 0.0D, maxY);
			return true;
		}

		if (button == 0 && resizing) {
			SpectraHudConfig config = SpectraHudClient.config();
			HudBounds bounds = currentBounds();
			double logicalWidth = SpectraHudClient.getHudRenderer().getLogicalWidth();
			double logicalHeight = SpectraHudClient.getHudRenderer().getLogicalHeight();
			double scaleX = (mouseX - bounds.x()) / logicalWidth;
			double scaleY = (mouseY - bounds.y()) / logicalHeight;
			config.hudScale = clamp(Math.min(scaleX, scaleY), 0.65D, 2.0D);
			keepHudOnScreen();
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && (dragging || resizing)) {
			dragging = false;
			resizing = false;
			SpectraHudClient.getConfigManager().save();
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		HudBounds bounds = currentBounds();
		if (bounds.contains(mouseX, mouseY)) {
			SpectraHudConfig config = SpectraHudClient.config();
			double oldScale = config.hudScale;
			double localX = (mouseX - bounds.x()) / oldScale;
			double localY = (mouseY - bounds.y()) / oldScale;
			config.hudScale = clamp(config.hudScale + amount * 0.06D, 0.65D, 2.0D);
			config.hudX = clamp(mouseX - localX * config.hudScale, 0.0D, Math.max(0.0D, this.width - SpectraHudClient.getHudRenderer().getLogicalWidth() * config.hudScale));
			config.hudY = clamp(mouseY - localY * config.hudScale, 0.0D, Math.max(0.0D, this.height - SpectraHudClient.getHudRenderer().getLogicalHeight() * config.hudScale));
			SpectraHudClient.getConfigManager().save();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public void close() {
		SpectraHudClient.getConfigManager().save();
		if (client != null) {
			client.setScreen(null);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private HudBounds currentBounds() {
		return SpectraHudClient.getHudRenderer().getBounds(this.width, this.height);
	}

	private void changeScale(double delta) {
		SpectraHudConfig config = SpectraHudClient.config();
		config.hudScale = clamp(config.hudScale + delta, 0.65D, 2.0D);
		keepHudOnScreen();
		SpectraHudClient.getConfigManager().save();
	}

	private void keepHudOnScreen() {
		SpectraHudConfig config = SpectraHudClient.config();
		HudBounds bounds = currentBounds();
		config.hudX = clamp(bounds.x(), 0.0D, Math.max(0.0D, this.width - bounds.width()));
		config.hudY = clamp(bounds.y(), 0.0D, Math.max(0.0D, this.height - bounds.height()));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
