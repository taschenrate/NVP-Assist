package avn.spectrahud.client.render;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.analysis.AimAnalyzer;
import avn.spectrahud.client.analysis.CheatAssessment;
import avn.spectrahud.client.analysis.CombatAnalyzer;
import avn.spectrahud.client.config.HudMode;
import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.gui.HudEditOverlayController;
import avn.spectrahud.client.gui.HudEditScreen;
import avn.spectrahud.client.hologram.HologramData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class HudRenderer {
	private static final int PANEL_RGB = 0x040A0C;
	private static final int BORDER = 0x553EFFF0;
	private static final int TEXT = 0xFFE8EEEE;
	private static final int MUTED = 0xFFA9B2B5;
	private static final int PURPLE = 0xFFB05CFF;
	private static final int CYAN = 0xFF28FFF0;
	private static final int YELLOW = 0xFFFFC24A;
	private static final int GREEN = 0xFF47FF6A;
	private static final int RED = 0xFFFF4B43;

	public void render(DrawContext context, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof HudEditScreen) {
			return;
		}

		renderHud(context, false);
	}

	public void renderHud(DrawContext context, boolean editMode) {
		MinecraftClient client = MinecraftClient.getInstance();
		SpectraHudConfig config = SpectraHudClient.config();
		boolean editing = editMode || HudEditOverlayController.isActive() || client.currentScreen instanceof HudEditScreen;
		if ((!config.hudEnabled || !SpectraHudClient.getState().hasSuspect()) && !editing) {
			return;
		}

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		HudBounds bounds = getBounds(screenWidth, screenHeight);
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(bounds.x(), bounds.y(), 0.0D);
		matrices.scale((float) config.hudScale, (float) config.hudScale, 1.0F);

		int width = getLogicalWidth();
		int height = getLogicalHeight();
		drawPanel(context, 0, 0, width, height, alpha(config.hudOpacity));
		drawContent(context, client.textRenderer, config, width);

		if (editing) {
			drawEditFrame(context, width, height);
		}

		matrices.pop();
	}

	public HudBounds getBounds(int screenWidth, int screenHeight) {
		SpectraHudConfig config = SpectraHudClient.config();
		int logicalWidth = getLogicalWidth();
		int logicalHeight = getLogicalHeight();
		int width = (int) Math.round(logicalWidth * config.hudScale);
		int height = (int) Math.round(logicalHeight * config.hudScale);
		int x = config.hudX < 0.0D ? screenWidth - width - 16 : (int) Math.round(config.hudX);
		int y = (int) Math.round(config.hudY);
		x = Math.max(0, Math.min(screenWidth - width, x));
		y = Math.max(0, Math.min(screenHeight - height, y));
		return new HudBounds(x, y, width, height);
	}

	public int getLogicalWidth() {
		return 190;
	}

	public int getLogicalHeight() {
		SpectraHudConfig config = SpectraHudClient.config();
		int base = config.hudMode == HudMode.COMPACT ? 170 : 236;
		return config.showHitDelayStrip ? base + 42 : base;
	}

	private void drawContent(DrawContext context, TextRenderer textRenderer, SpectraHudConfig config, int width) {
		CombatAnalyzer combat = SpectraHudClient.getCombatAnalyzer();
		AimAnalyzer aim = SpectraHudClient.getAimAnalyzer();
		CheatAssessment cheat = SpectraHudClient.getCheatClassifier().classify(combat, aim, config, SpectraHudClient.getTargetTracker().getTargetSwitches());
		HologramData hologram = SpectraHudClient.getHologramReader().getData();
		String suspect = SpectraHudClient.getState().hasSuspect() ? SpectraHudClient.getState().getActiveSuspectName() : "нет слежки";
		double combatRisk = combat.getRisk(config, SpectraHudClient.getTargetTracker().getTargetSwitches());
		double aimRisk = aim.getRisk();
		double combinedRisk = Math.min(1.0D, combatRisk + aimRisk * 0.35D);
		double acRisk = hologram.riskValue();

		int y = 9;
		drawLabel(context, textRenderer, "ПОДОЗРЕВАЕМЫЙ", 10, y);
		y += 10;
		drawText(context, textRenderer, suspect, 10, y, config.suspectColor);
		y += 14;

		drawLabel(context, textRenderer, "AC / РИСК", 10, y);
		y += 10;
		String riskText = formatAc(hologram) + " / " + formatRisk(combinedRisk);
		drawText(context, textRenderer, riskText, 10, y, TEXT);
		drawRiskBar(context, 10, y + 10, width - 20, 4, Math.max(acRisk, combinedRisk));
		y += 19;

		if (config.hudMode == HudMode.NORMAL) {
			y = drawRow(context, textRenderer, y, "Последний AVG:", formatAc(hologram), TEXT, width);
		}

		drawLabel(context, textRenderer, "ВОЗМОЖНЫЙ ЧИТ", 10, y);
		y += 10;
		drawText(context, textRenderer, cheat.label(), 10, y, cheatLabelColor(cheat));
		String confidence = formatPercent(cheat.confidence());
		int confidenceX = width - 10 - textRenderer.getWidth(confidence);
		context.drawText(textRenderer, confidence, confidenceX, y, MUTED, false);
		y += 14;

		drawSeparator(context, y, width);
		y += 5;

		y = drawRow(context, textRenderer, y, "Чистые попадания:", Integer.toString(combat.getCleanHits()), GREEN, width);
		y = drawRow(context, textRenderer, y, "Грязные попадания:", Integer.toString(combat.getDirtyHits()), RED, width);

		if (config.hudMode == HudMode.NORMAL) {
			y = drawRow(context, textRenderer, y, "Последняя дистанция:", formatDistance(combat.getLastReach()), reachColor(combat.getLastReach(), config), width);
		}

		y = drawRow(context, textRenderer, y, "Макс. дистанция:", formatDistance(combat.getMaxReach()), reachColor(combat.getMaxReach(), config), width);
		y = drawRow(context, textRenderer, y, "Средняя дистанция:", formatDistance(combat.getAverageReach()), TEXT, width);
		y = drawRow(context, textRenderer, y, "Средняя задержка:", formatDelay(combat.getAverageDelay()), YELLOW, width);
		y = drawRow(context, textRenderer, y, "Рывки головы:", Integer.toString(aim.getSharpJerks()), aim.getSharpJerks() > 0 ? YELLOW : MUTED, width);

		if (config.hudMode == HudMode.NORMAL) {
			y = drawRow(context, textRenderer, y, "Разброс задержки:", formatDelay(combat.getDelaySpread()), MUTED, width);
			y = drawRow(context, textRenderer, y, "Развороты:", Integer.toString(aim.getBigTurns()), aim.getBigTurns() > 0 ? YELLOW : MUTED, width);
			y = drawRow(context, textRenderer, y, "Макс. поворот:", formatDegrees(aim.getMaxTurn()), aim.getMaxTurn() >= 80.0D ? RED : MUTED, width);
		}

		y = drawRow(context, textRenderer, y, "Согласованность:", combat.getConsistency(config) + "%", CYAN, width);

		if (config.showHitDelayStrip) {
			drawDelayStrip(context, textRenderer, y + 4, width, combat.getHitDelays(), combat.getAverageDelay(), combat.getConsistency(config), config.hudMode == HudMode.NORMAL);
		}
	}

	private void drawDelayStrip(DrawContext context, TextRenderer textRenderer, int y, int width, Deque<Long> delays, long average, int consistency, boolean normalMode) {
		drawSeparator(context, y, width);
		y += 6;
		drawLabel(context, textRenderer, "ЗАДЕРЖКА (мс)", 10, y);
		y += 10;

		List<Long> values = new ArrayList<>(delays);
		if (values.isEmpty()) {
			drawText(context, textRenderer, "нет данных", 10, y + 5, MUTED);
			context.fill(10, y + 16, width - 14, y + 17, 0x553EFFF0);
			return;
		}

		if (normalMode) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < values.size(); i++) {
				if (i > 0) {
					builder.append(" | ");
				}
				builder.append(values.get(i));
			}
			drawText(context, textRenderer, builder.toString(), 10, y, MUTED);
			y += 10;
		}

		int lineX = 12;
		int lineY = y + 15;
		int lineWidth = width - 34;
		context.fill(lineX, lineY, lineX + lineWidth, lineY + 1, 0x775C6DFF);
		context.fill(lineX + lineWidth, lineY - 2, lineX + lineWidth + 4, lineY + 3, CYAN);

		for (int i = 0; i < values.size(); i++) {
			int x = values.size() == 1 ? lineX + lineWidth / 2 : lineX + (int) Math.round(i * (lineWidth / (double) (values.size() - 1)));
			int color = delayColor(values.get(i), average, consistency, i);
			context.fill(x - 2, lineY - 2, x + 3, lineY + 3, color);
			if (!normalMode && (i % 2 == 0 || i == values.size() - 1)) {
				String label = Long.toString(values.get(i));
				int labelX = clampInt(x - textRenderer.getWidth(label) / 2, 10, width - 10 - textRenderer.getWidth(label));
				context.drawText(textRenderer, label, labelX, y, color, false);
			}
		}
	}

	private int drawRow(DrawContext context, TextRenderer textRenderer, int y, String label, String value, int valueColor, int width) {
		drawText(context, textRenderer, label, 10, y, MUTED);
		context.drawText(textRenderer, value, width - 10 - textRenderer.getWidth(value), y, valueColor, false);
		return y + 10;
	}

	private void drawPanel(DrawContext context, int x, int y, int width, int height, int alpha) {
		int panel = (alpha << 24) | PANEL_RGB;
		context.fill(x + 4, y, x + width - 4, y + height, panel);
		context.fill(x, y + 4, x + width, y + height - 4, panel);
		context.fill(x + 2, y + 2, x + width - 2, y + height - 2, panel);
		context.fill(x + 4, y, x + width - 4, y + 1, BORDER);
		context.fill(x + 4, y + height - 1, x + width - 4, y + height, BORDER);
		context.fill(x, y + 4, x + 1, y + height - 4, BORDER);
		context.fill(x + width - 1, y + 4, x + width, y + height - 4, BORDER);
	}

	private void drawEditFrame(DrawContext context, int width, int height) {
		context.fill(width - 15, height - 3, width - 3, height - 2, CYAN);
		context.fill(width - 7, height - 15, width - 6, height - 3, CYAN);
		context.fill(width - 18, height - 18, width - 4, height - 4, 0x3328FFF0);
		context.fill(width - 12, height - 12, width - 4, height - 4, 0x6628FFF0);
		context.fill(width - 10, height - 10, width - 4, height - 4, 0xAA28FFF0);
	}

	private void drawRiskBar(DrawContext context, int x, int y, int width, int height, double risk) {
		context.fill(x, y, x + width, y + height, 0x33000000);
		int fill = (int) Math.round(width * Math.max(0.0D, Math.min(1.0D, risk)));
		int color = risk >= 0.70D ? RED : risk >= 0.40D ? YELLOW : CYAN;
		context.fill(x, y, x + fill, y + height, color);
	}

	private void drawSeparator(DrawContext context, int y, int width) {
		context.fill(10, y, width - 10, y + 1, 0x22FFFFFF);
	}

	private void drawLabel(DrawContext context, TextRenderer textRenderer, String text, int x, int y) {
		drawText(context, textRenderer, text, x, y, MUTED);
	}

	private void drawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
		context.drawText(textRenderer, text, x, y, color, false);
	}

	private int delayColor(long delay, long average, int consistency, int index) {
		if (average > 0L) {
			long diff = Math.abs(delay - average);
			if (consistency > 90 && diff <= 5L) {
				return RED;
			}
			if (consistency > 75 && diff <= 12L) {
				return YELLOW;
			}
		}

		return index % 2 == 0 ? PURPLE : CYAN;
	}

	private int reachColor(double reach, SpectraHudConfig config) {
		if (reach <= 0.0D) {
			return MUTED;
		}
		if (reach <= config.reachGreenMax) {
			return GREEN;
		}
		if (reach <= config.reachYellowMax) {
			return YELLOW;
		}
		return RED;
	}

	private static int alpha(double opacity) {
		return (int) Math.round(Math.max(0.0D, Math.min(1.0D, opacity)) * 255.0D);
	}

	private static String formatRisk(double value) {
		return String.format(Locale.ROOT, "%.2f", Math.max(0.0D, Math.min(1.0D, value)));
	}

	private static String formatAc(HologramData hologram) {
		if (!hologram.isPresent()) {
			return "AVG --";
		}

		if (hologram.percent() >= 0) {
			return String.format(Locale.ROOT, "AVG %.2f (%d%%)", hologram.average(), hologram.percent());
		}

		return String.format(Locale.ROOT, "AVG %.2f", hologram.average());
	}

	private static String formatDistance(double value) {
		return value <= 0.0D ? "0.00" : String.format(Locale.ROOT, "%.2f", value);
	}

	private static String formatDelay(long value) {
		return value <= 0L ? "0 мс" : value + " мс";
	}

	private static String formatDegrees(double value) {
		return value <= 0.0D ? "0°" : Math.round(value) + "°";
	}

	private static String formatPercent(double value) {
		return Math.round(Math.max(0.0D, Math.min(1.0D, value)) * 100.0D) + "%";
	}

	private static int cheatLabelColor(CheatAssessment cheat) {
		String label = cheat.label();
		if (label.contains("KillAura")) {
			return RED;
		}
		if (label.contains("Reach")) {
			return PURPLE;
		}
		if (label.contains("Trigger")) {
			return CYAN;
		}
		if (label.contains("Aim")) {
			return YELLOW;
		}
		return PURPLE;
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
