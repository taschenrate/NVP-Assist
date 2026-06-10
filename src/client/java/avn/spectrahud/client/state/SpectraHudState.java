package avn.spectrahud.client.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.Optional;
import java.util.UUID;

public class SpectraHudState {
	private String activeSuspectName = "";
	private UUID activeSuspectUuid;
	private UUID currentTargetUuid;
	private long sessionStartedMs;
	private boolean seenSuspectOnce;
	private int missingTicks;

	public boolean setSuspect(MinecraftClient client, String name) {
		String normalized = name == null ? "" : name.trim();
		if (normalized.isEmpty()) {
			return false;
		}

		boolean changed = !normalized.equalsIgnoreCase(activeSuspectName);
		activeSuspectName = normalized;
		activeSuspectUuid = null;
		currentTargetUuid = null;
		sessionStartedMs = System.currentTimeMillis();
		seenSuspectOnce = false;
		missingTicks = 0;
		resolveSuspect(client);
		return changed;
	}

	public void resolveSuspect(MinecraftClient client) {
		if (client == null || client.world == null || activeSuspectName.isEmpty()) {
			return;
		}

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player.getGameProfile().getName().equalsIgnoreCase(activeSuspectName)) {
				activeSuspectUuid = player.getUuid();
				activeSuspectName = player.getGameProfile().getName();
				seenSuspectOnce = true;
				missingTicks = 0;
				return;
			}
		}

		if (seenSuspectOnce) {
			missingTicks++;
		}
	}

	public void clearRuntime() {
		activeSuspectUuid = null;
		currentTargetUuid = null;
	}

	public void clearSuspect() {
		activeSuspectName = "";
		activeSuspectUuid = null;
		currentTargetUuid = null;
		sessionStartedMs = 0L;
		seenSuspectOnce = false;
		missingTicks = 0;
	}

	public boolean shouldClearMissing(int ticks) {
		return hasSuspect() && seenSuspectOnce && missingTicks >= ticks;
	}

	public boolean hasSuspect() {
		return !activeSuspectName.isEmpty();
	}

	public String getActiveSuspectName() {
		return activeSuspectName;
	}

	public long getSessionStartedMs() {
		return sessionStartedMs;
	}

	public Optional<AbstractClientPlayerEntity> getSuspect(MinecraftClient client) {
		if (client == null || client.world == null || !hasSuspect()) {
			return Optional.empty();
		}

		if (activeSuspectUuid != null) {
			for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
				if (player.getUuid().equals(activeSuspectUuid)) {
					seenSuspectOnce = true;
					missingTicks = 0;
					return Optional.of(player);
				}
			}
		}

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player.getGameProfile().getName().equalsIgnoreCase(activeSuspectName)) {
				activeSuspectUuid = player.getUuid();
				seenSuspectOnce = true;
				missingTicks = 0;
				return Optional.of(player);
			}
		}

		return Optional.empty();
	}

	public void setCurrentTarget(AbstractClientPlayerEntity target) {
		currentTargetUuid = target == null ? null : target.getUuid();
	}

	public Optional<AbstractClientPlayerEntity> getCurrentTarget(MinecraftClient client) {
		if (client == null || client.world == null || currentTargetUuid == null) {
			return Optional.empty();
		}

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player.getUuid().equals(currentTargetUuid)) {
				return Optional.of(player);
			}
		}

		return Optional.empty();
	}
}
