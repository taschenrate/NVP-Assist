package avn.spectrahud.client.analysis;

import avn.spectrahud.client.config.SpectraHudConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CheatClassifier {
	private static final double MIN_SIGNAL = 0.22D;

	public CheatAssessment classify(CombatAnalyzer combat, AimAnalyzer aim, SpectraHudConfig config, int targetSwitches) {
		double reachScore = scoreReach(combat, config);
		double triggerScore = scoreTrigger(combat, config);
		double aimScore = scoreAim(aim);
		double switchScore = scoreSwitch(targetSwitches);

		List<Signal> signals = new ArrayList<>(4);
		signals.add(new Signal("Reach", reachScore));
		signals.add(new Signal("Trigger", triggerScore));
		signals.add(new Signal("Aim", aimScore));
		signals.add(new Signal("KillAura / TargetSwitch", switchScore));
		signals.sort(Comparator.comparingDouble(Signal::score).reversed());

		Signal top = signals.get(0);
		Signal second = signals.get(1);
		if (top.score < MIN_SIGNAL) {
			return new CheatAssessment("Неясно", 0.0D, reachScore, triggerScore, aimScore, switchScore);
		}

		String label = top.label;
		double confidence = top.score;
		if (shouldPair(top, second)) {
			label = top.label + " / " + second.label;
			confidence = clamp(top.score * 0.65D + second.score * 0.35D, 0.0D, 1.0D);
		}

		return new CheatAssessment(label, confidence, reachScore, triggerScore, aimScore, switchScore);
	}

	private boolean shouldPair(Signal top, Signal second) {
		if (second.score < 0.40D) {
			return false;
		}

		double gap = top.score - second.score;
		return gap <= 0.15D || (top.score < 0.55D && second.score >= 0.45D);
	}

	private double scoreReach(CombatAnalyzer combat, SpectraHudConfig config) {
		double maxReach = combat.getMaxReach();
		double averageReach = combat.getAverageReach();
		int dirtyHits = combat.getDirtyHits();

		if (maxReach <= 0.0D && averageReach <= 0.0D && dirtyHits == 0) {
			return 0.0D;
		}

		double peak = clamp((maxReach - config.reachGreenMax) / Math.max(0.01D, config.reachYellowMax - config.reachGreenMax), 0.0D, 1.0D);
		double average = clamp((averageReach - 3.00D) / 0.45D, 0.0D, 1.0D);
		double dirty = clamp(dirtyHits / 6.0D, 0.0D, 1.0D);
		return clamp(peak * 0.55D + average * 0.25D + dirty * 0.20D, 0.0D, 1.0D);
	}

	private double scoreTrigger(CombatAnalyzer combat, SpectraHudConfig config) {
		int consistency = combat.getConsistency(config);
		long averageDelay = combat.getAverageDelay();
		long delaySpread = combat.getDelaySpread();
		int samples = combat.getHitDelays().size();

		if (samples < 3 || averageDelay <= 0L) {
			return 0.0D;
		}

		double consistencyScore = clamp((consistency - 68.0D) / 32.0D, 0.0D, 1.0D);
		double spreadRatio = delaySpread / (double) Math.max(averageDelay, 1L);
		double spreadScore = clamp(1.0D - spreadRatio * 3.0D, 0.0D, 1.0D);
		double cadenceScore = clamp(1.0D - Math.abs(averageDelay - 500.0D) / 700.0D, 0.0D, 1.0D);
		return clamp(consistencyScore * 0.55D + spreadScore * 0.30D + cadenceScore * 0.15D, 0.0D, 1.0D);
	}

	private double scoreAim(AimAnalyzer aim) {
		int jerks = aim.getSharpJerks();
		int turns = aim.getBigTurns();
		double maxTurn = aim.getMaxTurn();
		double avgTurn = aim.getAverageTurn();

		if (jerks == 0 && turns == 0 && maxTurn <= 0.0D) {
			return 0.0D;
		}

		double jerkScore = clamp(jerks / 6.0D, 0.0D, 1.0D);
		double turnScore = clamp(turns / 4.0D, 0.0D, 1.0D);
		double maxTurnScore = clamp((maxTurn - 35.0D) / 85.0D, 0.0D, 1.0D);
		double avgTurnScore = clamp((avgTurn - 18.0D) / 40.0D, 0.0D, 1.0D);
		return clamp(jerkScore * 0.35D + turnScore * 0.30D + maxTurnScore * 0.20D + avgTurnScore * 0.15D, 0.0D, 1.0D);
	}

	private double scoreSwitch(int targetSwitches) {
		return clamp(targetSwitches / 4.0D, 0.0D, 1.0D);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private record Signal(String label, double score) {
	}
}
