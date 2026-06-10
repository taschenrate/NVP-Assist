package avn.spectrahud.client.analysis;

import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.state.SpectraHudState;
import avn.spectrahud.client.target.TargetTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

public class AimAnalyzer {
	private boolean initialized;
	private float lastYaw;
	private float lastPitch;
	private double lastTurn;
	private int samples;
	private int sharpJerks;
	private int bigTurns;
	private double maxTurn;
	private double totalTurn;

	public void tick(MinecraftClient client, SpectraHudState state, TargetTracker targetTracker, SpectraHudConfig config) {
		Optional<AbstractClientPlayerEntity> suspect = state.getSuspect(client);
		if (suspect.isEmpty()) {
			initialized = false;
			return;
		}

		AbstractClientPlayerEntity player = suspect.get();
		float yaw = player.getYaw();
		float pitch = player.getPitch();

		if (!initialized) {
			initialized = true;
			lastYaw = yaw;
			lastPitch = pitch;
			lastTurn = 0.0D;
			return;
		}

		double yawDelta = Math.abs(MathHelper.wrapDegrees(yaw - lastYaw));
		double pitchDelta = Math.abs(pitch - lastPitch);
		double turn = Math.hypot(yawDelta, pitchDelta);
		double acceleration = Math.abs(turn - lastTurn);
		boolean hasNearbyTarget = targetTracker.getTarget(client, state)
				.map(target -> target.squaredDistanceTo(player) <= 49.0D)
				.orElse(false);

		if (turn >= 70.0D || yawDelta >= 65.0D || pitchDelta >= 38.0D) {
			bigTurns++;
		}

		if (hasNearbyTarget && turn >= 24.0D && acceleration >= 18.0D) {
			sharpJerks++;
		}

		maxTurn = Math.max(maxTurn, turn);
		totalTurn += turn;
		samples++;
		lastTurn = turn;
		lastYaw = yaw;
		lastPitch = pitch;
	}

	public void reset() {
		initialized = false;
		lastYaw = 0.0F;
		lastPitch = 0.0F;
		lastTurn = 0.0D;
		samples = 0;
		sharpJerks = 0;
		bigTurns = 0;
		maxTurn = 0.0D;
		totalTurn = 0.0D;
	}

	public int getSharpJerks() {
		return sharpJerks;
	}

	public int getBigTurns() {
		return bigTurns;
	}

	public double getMaxTurn() {
		return maxTurn;
	}

	public double getAverageTurn() {
		return samples == 0 ? 0.0D : totalTurn / samples;
	}

	public double getRisk() {
		double jerkScore = Math.min(0.36D, sharpJerks * 0.06D);
		double turnScore = Math.min(0.26D, bigTurns * 0.045D);
		double maxScore = maxTurn >= 120.0D ? 0.18D : maxTurn >= 80.0D ? 0.10D : 0.0D;
		return Math.min(1.0D, jerkScore + turnScore + maxScore);
	}
}
