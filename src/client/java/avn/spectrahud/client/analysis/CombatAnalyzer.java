package avn.spectrahud.client.analysis;

import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.state.SpectraHudState;
import avn.spectrahud.client.target.TargetTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CombatAnalyzer {
	private static final long MIN_HIT_DELAY_MS = 120L;
	private static final long MAX_CHAIN_HIT_DELAY_MS = 1800L;

	private final Map<UUID, Integer> previousHurtTimes = new HashMap<>();
	private final Deque<Long> hitDelays = new ArrayDeque<>();

	private int cleanHits;
	private int dirtyHits;
	private double lastReach;
	private double maxReach;
	private double totalReach;
	private long lastSuccessfulHitMs;
	private long lastSwingMs;
	private long lastDirtySwingMs;
	private boolean wasSwinging;

	public void tick(MinecraftClient client, SpectraHudState state, TargetTracker targetTracker, SpectraHudConfig config) {
		Optional<AbstractClientPlayerEntity> suspectOptional = state.getSuspect(client);
		if (client.world == null || suspectOptional.isEmpty()) {
			previousHurtTimes.clear();
			wasSwinging = false;
			return;
		}

		AbstractClientPlayerEntity suspect = suspectOptional.get();
		boolean swinging = suspect.getHandSwingProgress(1.0F) > 0.02F;
		boolean swingStarted = swinging && !wasSwinging;
		wasSwinging = swinging;

		if (swingStarted) {
			lastSwingMs = System.currentTimeMillis();
			registerDirtyAttemptIfNeeded(client, suspect, targetTracker, config);
		}

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == suspect || player == client.player || player.isSpectator()) {
				continue;
			}

			int previous = previousHurtTimes.getOrDefault(player.getUuid(), 0);
			int current = player.hurtTime;
			previousHurtTimes.put(player.getUuid(), current);

			if (current > 0 && previous <= 0 && isLikelySuspectHit(suspect, player, targetTracker, config)) {
				registerSuccessfulHit(state, targetTracker, suspect, player);
			}
		}
	}

	public void reset() {
		previousHurtTimes.clear();
		hitDelays.clear();
		cleanHits = 0;
		dirtyHits = 0;
		lastReach = 0.0D;
		maxReach = 0.0D;
		totalReach = 0.0D;
		lastSuccessfulHitMs = 0L;
		lastSwingMs = 0L;
		lastDirtySwingMs = 0L;
		wasSwinging = false;
	}

	public int getCleanHits() {
		return cleanHits;
	}

	public int getDirtyHits() {
		return dirtyHits;
	}

	public double getLastReach() {
		return lastReach;
	}

	public double getMaxReach() {
		return maxReach;
	}

	public double getAverageReach() {
		return cleanHits == 0 ? 0.0D : totalReach / cleanHits;
	}

	public Deque<Long> getHitDelays() {
		return new ArrayDeque<>(hitDelays);
	}

	public long getAverageDelay() {
		if (hitDelays.isEmpty()) {
			return 0L;
		}

		long sum = 0L;
		for (long delay : hitDelays) {
			sum += delay;
		}
		return Math.round(sum / (double) hitDelays.size());
	}

	public long getDelaySpread() {
		if (hitDelays.size() < 2) {
			return 0L;
		}

		double average = getAverageDelay();
		double deviation = 0.0D;
		for (long delay : hitDelays) {
			deviation += Math.abs(delay - average);
		}
		return Math.round(deviation / hitDelays.size());
	}

	public int getConsistency(SpectraHudConfig config) {
		if (hitDelays.size() < 3 || getAverageDelay() <= 0L) {
			return 0;
		}

		double average = getAverageDelay();
		double deviation = getDelaySpread();
		double penalty = (deviation / average) * 100.0D * config.consistencySensitivity;
		return (int) Math.round(100.0D - clamp(penalty, 0.0D, 100.0D));
	}

	public double getRisk(SpectraHudConfig config, int targetSwitches) {
		double reachScore;
		if (maxReach <= config.reachGreenMax) {
			reachScore = 0.0D;
		} else if (maxReach <= config.reachYellowMax) {
			reachScore = 0.18D + (maxReach - config.reachGreenMax) / (config.reachYellowMax - config.reachGreenMax) * 0.22D;
		} else {
			reachScore = 0.48D + Math.min(0.22D, (maxReach - config.reachYellowMax) * 0.35D);
		}

		int consistency = getConsistency(config);
		double triggerScore = consistency > 90 ? 0.32D : consistency > 75 ? 0.18D : 0.0D;
		double dirtyScore = Math.min(0.16D, dirtyHits * 0.025D);
		double switchScore = Math.min(0.12D, targetSwitches * 0.04D);
		return clamp(reachScore + triggerScore + dirtyScore + switchScore, 0.0D, 1.0D);
	}

	private void registerSuccessfulHit(SpectraHudState state, TargetTracker targetTracker, AbstractClientPlayerEntity suspect, AbstractClientPlayerEntity target) {
		long now = System.currentTimeMillis();
		if (lastSuccessfulHitMs > 0L) {
			long delay = now - lastSuccessfulHitMs;
			if (delay >= MIN_HIT_DELAY_MS && delay <= MAX_CHAIN_HIT_DELAY_MS) {
				hitDelays.addLast(delay);
				while (hitDelays.size() > 7) {
					hitDelays.removeFirst();
				}
			}
		}

		lastSuccessfulHitMs = now;
		lastReach = suspect.distanceTo(target);
		maxReach = Math.max(maxReach, lastReach);
		totalReach += lastReach;
		cleanHits++;
		targetTracker.markSuccessfulHit(target);
		state.setCurrentTarget(target);
	}

	private boolean isLikelySuspectHit(AbstractClientPlayerEntity suspect, AbstractClientPlayerEntity target, TargetTracker targetTracker, SpectraHudConfig config) {
		long now = System.currentTimeMillis();
		double distance = suspect.distanceTo(target);
		if (distance > config.rayLength + 1.0D) {
			return false;
		}

		boolean recentSwing = now - lastSwingMs <= 650L;
		boolean lookingAtTarget = targetTracker.isLookingAt(suspect, target, config.rayLength + 0.75D);
		return recentSwing && (lookingAtTarget || distance <= config.rayLength);
	}

	private void registerDirtyAttemptIfNeeded(MinecraftClient client, AbstractClientPlayerEntity suspect, TargetTracker targetTracker, SpectraHudConfig config) {
		Optional<AbstractClientPlayerEntity> lookTarget = targetTracker.getLookTarget(client);
		if (lookTarget.isEmpty()) {
			return;
		}

		double distance = suspect.distanceTo(lookTarget.get());
		long now = System.currentTimeMillis();
		if (distance > config.reachYellowMax && now - lastDirtySwingMs > 300L) {
			dirtyHits++;
			lastDirtySwingMs = now;
		}
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
