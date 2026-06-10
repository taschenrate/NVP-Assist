package avn.spectrahud.client.config;

public class SpectraHudConfig {
	public boolean hudEnabled = true;
	public HudMode hudMode = HudMode.COMPACT;
	public double hudX = -1.0D;
	public double hudY = 16.0D;
	public double hudScale = 1.0D;
	public double hudOpacity = 0.80D;
	public boolean showHitDelayStrip = true;
	public int hitDelaySamples = 7;

	public boolean suspectOutline = true;
	public boolean targetOutline = false;
	public boolean throughWalls = false;
	public boolean showViewRay = true;
	public boolean hideAnticheatHologram = true;
	public boolean replayClips = false;
	public boolean autoTeleportToSuspect = false;

	public int suspectColor = 0xFFFF4B43;
	public int targetColor = 0xFF28FFF0;
	public int rayColor = 0xFFFF4B43;
	public int rayHitColor = 0xFF39FF6A;
	public int rayMissColor = 0xFFFF3E3E;

	public double reachGreenMax = 3.10D;
	public double reachYellowMax = 3.39D;
	public double rayLength = 4.50D;
	public double viewRayLength = 8.00D;
	public double consistencySensitivity = 3.25D;

	public void clamp() {
		if (hudMode == null) {
			hudMode = HudMode.COMPACT;
		}

		hudScale = clamp(hudScale, 0.65D, 2.00D);
		hudOpacity = clamp(hudOpacity, 0.25D, 1.00D);
		hitDelaySamples = 7;
		targetOutline = false;
		rayLength = clamp(rayLength, 2.0D, 8.0D);
		viewRayLength = clamp(viewRayLength <= 0.0D ? 8.0D : viewRayLength, 4.0D, 16.0D);
		consistencySensitivity = clamp(consistencySensitivity, 0.75D, 8.0D);
		if (rayHitColor == 0) {
			rayHitColor = 0xFF39FF6A;
		}
		if (rayMissColor == 0) {
			rayMissColor = 0xFFFF3E3E;
		}
	}

	public void resetHudPosition() {
		hudX = -1.0D;
		hudY = 16.0D;
	}

	public void resetHudScale() {
		hudScale = 1.0D;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
