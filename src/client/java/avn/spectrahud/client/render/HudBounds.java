package avn.spectrahud.client.render;

public record HudBounds(int x, int y, int width, int height) {
	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	public boolean containsResizeHandle(double mouseX, double mouseY) {
		return mouseX >= x + width - 36 && mouseX <= x + width && mouseY >= y + height - 36 && mouseY <= y + height;
	}
}
