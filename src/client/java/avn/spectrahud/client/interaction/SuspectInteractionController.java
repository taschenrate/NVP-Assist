package avn.spectrahud.client.interaction;

import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.state.SpectraHudState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Optional;

public class SuspectInteractionController {
	private static final double AUTO_TELEPORT_DISTANCE_SQUARED = 50.0D * 50.0D;
	private static final double DIRECT_INTERACTION_DISTANCE_SQUARED = 6.0D * 6.0D;
	private static final int TELEPORT_COOLDOWN_TICKS = 140;
	private static final int PENDING_INTERACTION_TICKS = 120;

	private int teleportCooldownTicks;
	private int pendingInteractionTicks;
	private String pendingInteractionNick = "";

	public void tick(MinecraftClient client, SpectraHudState state, SpectraHudConfig config) {
		if (teleportCooldownTicks > 0) {
			teleportCooldownTicks--;
		}

		tickPendingInteraction(client, state);

		if (!config.autoTeleportToSuspect || !state.hasSuspect()) {
			return;
		}

		Optional<AbstractClientPlayerEntity> suspect = state.getSuspect(client);
		if (suspect.isEmpty() || isFarFromClient(client, suspect.get(), AUTO_TELEPORT_DISTANCE_SQUARED)) {
			sendTeleport(client, state.getActiveSuspectName(), false);
		}
	}

	public void openSuspectMenu(MinecraftClient client, SpectraHudState state) {
		if (client == null || client.player == null) {
			return;
		}

		if (!state.hasSuspect()) {
			sendStatus(client, "[НВП] Нет активного подозреваемого");
			return;
		}

		Optional<AbstractClientPlayerEntity> suspect = state.getSuspect(client);
		if (suspect.isPresent() && !isFarFromClient(client, suspect.get(), DIRECT_INTERACTION_DISTANCE_SQUARED)) {
			rightClick(client, suspect.get());
			return;
		}

		pendingInteractionNick = state.getActiveSuspectName();
		pendingInteractionTicks = PENDING_INTERACTION_TICKS;
		sendTeleport(client, pendingInteractionNick, true);
		sendStatus(client, "[НВП] TPO к " + pendingInteractionNick + ", затем ПКМ");
	}

	private void tickPendingInteraction(MinecraftClient client, SpectraHudState state) {
		if (pendingInteractionTicks <= 0) {
			pendingInteractionNick = "";
			return;
		}

		pendingInteractionTicks--;
		if (!state.hasSuspect() || !state.getActiveSuspectName().equalsIgnoreCase(pendingInteractionNick)) {
			pendingInteractionTicks = 0;
			pendingInteractionNick = "";
			return;
		}

		Optional<AbstractClientPlayerEntity> suspect = state.getSuspect(client);
		if (suspect.isPresent() && !isFarFromClient(client, suspect.get(), DIRECT_INTERACTION_DISTANCE_SQUARED)) {
			rightClick(client, suspect.get());
			pendingInteractionTicks = 0;
			pendingInteractionNick = "";
		}
	}

	private boolean sendTeleport(MinecraftClient client, String nick, boolean force) {
		if (client == null || client.player == null || client.getNetworkHandler() == null || nick == null || nick.isBlank()) {
			return false;
		}

		if (!force && teleportCooldownTicks > 0) {
			return false;
		}

		client.getNetworkHandler().sendChatCommand("tpo " + nick.trim());
		teleportCooldownTicks = TELEPORT_COOLDOWN_TICKS;
		return true;
	}

	private void rightClick(MinecraftClient client, AbstractClientPlayerEntity suspect) {
		if (client.player == null || client.interactionManager == null || suspect == null) {
			return;
		}

		client.interactionManager.interactEntity(client.player, suspect, Hand.MAIN_HAND);
		client.player.swingHand(Hand.MAIN_HAND);
		sendStatus(client, "[НВП] ПКМ по " + suspect.getGameProfile().getName());
	}

	private boolean isFarFromClient(MinecraftClient client, AbstractClientPlayerEntity suspect, double distanceSquared) {
		return client.player == null || suspect == null || client.player.squaredDistanceTo(suspect) > distanceSquared;
	}

	private void sendStatus(MinecraftClient client, String message) {
		if (client.player != null) {
			client.player.sendMessage(Text.literal(message), true);
		}
	}
}
