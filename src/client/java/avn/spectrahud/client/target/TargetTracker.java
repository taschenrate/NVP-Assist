package avn.spectrahud.client.target;

import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.state.SpectraHudState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.UUID;

public class TargetTracker {
	private UUID lookTargetUuid;
	private UUID currentTargetUuid;
	private long lastTargetChangeMs;
	private int targetSwitches;

	public void tick(MinecraftClient client, SpectraHudState state, SpectraHudConfig config) {
		Optional<AbstractClientPlayerEntity> suspect = state.getSuspect(client);
		if (suspect.isEmpty()) {
			lookTargetUuid = null;
			currentTargetUuid = null;
			return;
		}

		Optional<AbstractClientPlayerEntity> lookTarget = findLookTarget(client, suspect.get(), config.rayLength);
		UUID nextLookTarget = lookTarget.map(AbstractClientPlayerEntity::getUuid).orElse(null);

		if (nextLookTarget != null && currentTargetUuid != null && !nextLookTarget.equals(currentTargetUuid) && countNearbyPlayers(client, suspect.get()) >= 3) {
			long now = System.currentTimeMillis();
			if (now - lastTargetChangeMs < 1500L) {
				targetSwitches++;
			}
			lastTargetChangeMs = now;
		}

		lookTargetUuid = nextLookTarget;
		if (currentTargetUuid == null) {
			currentTargetUuid = lookTargetUuid;
		}

		state.setCurrentTarget(getTarget(client, state).orElse(null));
	}

	public Optional<AbstractClientPlayerEntity> getTarget(MinecraftClient client, SpectraHudState state) {
		Optional<AbstractClientPlayerEntity> current = findByUuid(client, currentTargetUuid);
		if (current.isPresent()) {
			return current;
		}

		return findByUuid(client, lookTargetUuid);
	}

	public Optional<AbstractClientPlayerEntity> getLookTarget(MinecraftClient client) {
		return findByUuid(client, lookTargetUuid);
	}

	public void markSuccessfulHit(AbstractClientPlayerEntity target) {
		if (target == null) {
			return;
		}

		UUID next = target.getUuid();
		if (currentTargetUuid != null && !currentTargetUuid.equals(next)) {
			long now = System.currentTimeMillis();
			if (now - lastTargetChangeMs < 1500L) {
				targetSwitches++;
			}
			lastTargetChangeMs = now;
		}

		currentTargetUuid = next;
	}

	public boolean isLookingAt(AbstractClientPlayerEntity suspect, AbstractClientPlayerEntity target, double length) {
		if (suspect == null || target == null) {
			return false;
		}

		Vec3d start = suspect.getEyePos();
		Vec3d end = start.add(suspect.getRotationVec(1.0F).normalize().multiply(length));
		Box targetBox = target.getBoundingBox().expand(0.30D);
		return targetBox.raycast(start, end).isPresent();
	}

	public int getTargetSwitches() {
		return targetSwitches;
	}

	public void reset() {
		lookTargetUuid = null;
		currentTargetUuid = null;
		lastTargetChangeMs = 0L;
		targetSwitches = 0;
	}

	private Optional<AbstractClientPlayerEntity> findLookTarget(MinecraftClient client, AbstractClientPlayerEntity suspect, double length) {
		if (client.world == null) {
			return Optional.empty();
		}

		Vec3d start = suspect.getEyePos();
		Vec3d end = start.add(suspect.getRotationVec(1.0F).normalize().multiply(length));
		AbstractClientPlayerEntity best = null;
		double bestDistance = Double.MAX_VALUE;

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == suspect || player == client.player || player.isSpectator()) {
				continue;
			}

			Optional<Vec3d> hit = player.getBoundingBox().expand(0.30D).raycast(start, end);
			if (hit.isPresent()) {
				double distance = start.squaredDistanceTo(hit.get());
				if (distance < bestDistance) {
					bestDistance = distance;
					best = player;
				}
			}
		}

		return Optional.ofNullable(best);
	}

	private Optional<AbstractClientPlayerEntity> findByUuid(MinecraftClient client, UUID uuid) {
		if (client.world == null || uuid == null) {
			return Optional.empty();
		}

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player.getUuid().equals(uuid)) {
				return Optional.of(player);
			}
		}

		return Optional.empty();
	}

	private int countNearbyPlayers(MinecraftClient client, AbstractClientPlayerEntity suspect) {
		if (client.world == null) {
			return 0;
		}

		int count = 0;
		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player != suspect && player != client.player && player.squaredDistanceTo(suspect) <= 64.0D) {
				count++;
			}
		}
		return count;
	}
}
