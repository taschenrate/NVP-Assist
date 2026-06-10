package avn.spectrahud.client.interaction;

import avn.spectrahud.client.SpectraHudClient;
import avn.spectrahud.client.config.SpectraHudConfig;
import avn.spectrahud.client.state.SpectraHudState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class SuspectInteractionController {
	private static final double AUTO_TELEPORT_DISTANCE_SQUARED = 50.0D * 50.0D;
	private static final double DIRECT_INTERACTION_DISTANCE_SQUARED = 12.0D * 12.0D;
	private static final int TELEPORT_COOLDOWN_TICKS = 60;
	private static final int PENDING_INTERACTION_TICKS = 260;

	private int teleportCooldownTicks;
	private int pendingInteractionTicks;
	private String pendingInteractionNick = "";

	public void tick(MinecraftClient client, SpectraHudState state, SpectraHudConfig config) {
		if (!SpectraHudClient.isSpectatorMode(client)) {
			reset();
			return;
		}

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
		if (!SpectraHudClient.isSpectatorMode(client)) {
			sendStatus(client, "[НВП] Доступно только в spectator");
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
			return;
		}

		sendTeleport(client, pendingInteractionNick, false);
	}

	private boolean sendTeleport(MinecraftClient client, String nick, boolean force) {
		if (!SpectraHudClient.isSpectatorMode(client) || client.getNetworkHandler() == null || nick == null || nick.isBlank()) {
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
		if (!SpectraHudClient.isSpectatorMode(client) || client.interactionManager == null || suspect == null) {
			return;
		}

		Vec3d hitPos = hitboxCenter(suspect);
		lookAt(client, hitPos);
		client.interactionManager.interactEntityAtLocation(client.player, suspect, new EntityHitResult(suspect, hitPos), Hand.MAIN_HAND);
		client.interactionManager.interactEntity(client.player, suspect, Hand.MAIN_HAND);
		client.player.swingHand(Hand.MAIN_HAND);
		sendStatus(client, "[НВП] ПКМ по " + suspect.getGameProfile().getName());
	}

	private Vec3d hitboxCenter(AbstractClientPlayerEntity suspect) {
		Box box = suspect.getBoundingBox();
		return new Vec3d((box.minX + box.maxX) * 0.5D, (box.minY + box.maxY) * 0.5D, (box.minZ + box.maxZ) * 0.5D);
	}

	private void lookAt(MinecraftClient client, Vec3d target) {
		if (client.player == null || client.getNetworkHandler() == null) {
			return;
		}

		Vec3d eye = client.player.getEyePos();
		Vec3d delta = target.subtract(eye);
		double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		if (horizontal < 1.0E-6D && Math.abs(delta.y) < 1.0E-6D) {
			return;
		}

		float yaw = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D));
		float pitch = MathHelper.clamp((float) (-Math.toDegrees(Math.atan2(delta.y, horizontal))), -90.0F, 90.0F);
		client.player.setYaw(yaw);
		client.player.setPitch(pitch);
		client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, client.player.isOnGround()));
	}

	private boolean isFarFromClient(MinecraftClient client, AbstractClientPlayerEntity suspect, double distanceSquared) {
		return client.player == null || suspect == null || client.player.squaredDistanceTo(suspect) > distanceSquared;
	}

	private void sendStatus(MinecraftClient client, String message) {
		if (client.player != null) {
			client.player.sendMessage(Text.literal(message), true);
		}
	}

	public void reset() {
		teleportCooldownTicks = 0;
		pendingInteractionTicks = 0;
		pendingInteractionNick = "";
	}

	public boolean hasPendingInteraction() {
		return pendingInteractionTicks > 0;
	}
}
